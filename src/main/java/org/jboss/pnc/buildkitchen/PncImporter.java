/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2023-2023 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.pnc.buildkitchen;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.commonjava.atlas.maven.ident.ref.SimpleProjectVersionRef;
import org.commonjava.atlas.npm.ident.ref.NpmPackageRef;
import org.jboss.pnc.api.constants.BuildConfigurationParameterKeys;
import org.jboss.pnc.api.constants.BuildGenerator;
import org.jboss.pnc.buildkitchen.api.PurlSha;
import org.jboss.pnc.buildkitchen.model.BuildRecipe;
import org.jboss.pnc.buildkitchen.model.BuildTool;
import org.jboss.pnc.buildkitchen.model.ScmInfo;
import org.jboss.pnc.client.BuildClient;
import org.jboss.pnc.client.BuildConfigurationClient;
import org.jboss.pnc.client.RemoteResourceException;
import org.jboss.pnc.dto.Artifact;
import org.jboss.pnc.dto.Build;
import org.jboss.pnc.dto.BuildConfigurationRevision;
import org.jboss.pnc.dto.BuildConfigurationRevisionRef;
import org.jboss.pnc.dto.Environment;
import org.jboss.pnc.enums.BuildStatus;
import org.jboss.pnc.enums.BuildType;
import org.jboss.pnc.restclient.util.ArtifactUtil;

import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
@ApplicationScoped
public class PncImporter {

    private static final String DEFAULT_MEMORY = "2";
    @Inject
    BuildClient buildClient;

    @Inject
    BuildConfigurationClient buildConfigClient;

    @Transactional
    public BuildRecipe importBuild(String buildId) {
        try {
            log.info("Importing PNC build {}", buildId);
            Build pncBuild = getSuccessfulBuild(buildId);
            Collection<Artifact> builtArtifacts = buildClient.getBuiltArtifacts(buildId).getAll();
            BuildType buildType = pncBuild.getBuildConfigRevision().getBuildType();

            ScmInfo scmInfo = getScmInfo(
                    pncBuild.getScmUrl(),
                    pncBuild.getScmRevision(),
                    pncBuild.getScmRepository().getExternalUrl(),
                    pncBuild.getScmBuildConfigRevision(),
                    pncBuild.getBuildConfigRevision().getScmRevision());

            Set<BuildTool> buildTools = getBuildTools(pncBuild.getEnvironment().getAttributes());
            long memoryRequired = getMemoryRequired(pncBuild.getBuildConfigRevision());

            BuildRecipe buildRecipe = getRecipe(scmInfo);
            buildRecipe.buildScript = pncBuild.getBuildConfigRevision().getBuildScript();
            buildRecipe.buildTools = buildTools;
            buildRecipe.memoryRequired = memoryRequired;
            buildRecipe.persist();

            org.jboss.pnc.buildkitchen.model.Build build = new org.jboss.pnc.buildkitchen.model.Build();
            build.recipe = buildRecipe;
            build.generator = BuildGenerator.PNC;
            build.buildId = buildId;
            build.buildDuration = Duration.between(pncBuild.getStartTime(), pncBuild.getEndTime()).getSeconds();
            build.buildTime = pncBuild.getEndTime();
            build.memory = memoryRequired;
            build.image = getImage(pncBuild.getEnvironment());
            build.builtArtifacts = new HashSet<>(persistArtifacts(builtArtifacts).values());
            build.versionGenerated = findGeneratedVersion(builtArtifacts, buildType).orElse(null);
            build.persist();

            buildRecipe.builds.add(build);

            log.debug("PNC build {} imported as build {} with recipe {}", buildId, build.buildId, buildRecipe.id);
            return buildRecipe;
        } catch (RemoteResourceException ex) {
            throw new RuntimeException("Failed to read build information from PNC.", ex);
        }
    }

    private Optional<String> findGeneratedVersion(Collection<Artifact> builtArtifacts, BuildType buildType) {
        try {
            Set<String> versions = switch (buildType) {
                case SBT, GRADLE,
                        MVN ->
                    builtArtifacts.stream()
                            .map(ArtifactUtil::parseMavenCoordinates)
                            .map(SimpleProjectVersionRef::getVersionString)
                            .collect(Collectors.toSet());
                case NPM -> builtArtifacts.stream()
                        .map(ArtifactUtil::parseNPMCoordinates)
                        .map(NpmPackageRef::getVersionString)
                        .collect(Collectors.toSet());
            };
            if (versions.size() == 1) {
                return Optional.of(versions.iterator().next());
            }
        } catch (RuntimeException ex) {
            log.warn("Caught exception when parsing the generated version.", ex);
        }
        return Optional.empty();
    }

    private static String getImage(Environment environment) {
        String repoURL = environment.getSystemImageRepositoryUrl();
        if (!repoURL.endsWith("/")) {
            repoURL += "/";
        }
        return repoURL + environment.getSystemImageId();
    }

    private Build getSuccessfulBuild(String buildId) throws RemoteResourceException {
        Build pncBuild = buildClient.getSpecific(buildId);

        if (pncBuild == null) {
            throw new IllegalArgumentException("Build " + buildId + " not found.");
        }
        if (pncBuild.getStatus() != BuildStatus.SUCCESS) {
            throw new IllegalArgumentException(
                    "Build " + buildId + " does not have status SUCCESS (has " + pncBuild.getStatus() + ")");
        }
        return pncBuild;
    }

    public static ScmInfo getScmInfo(
            @NotNull String buildUrl,
            @NotNull String buildCommitId,
            String originUrl,
            String originCommitId,
            @NotNull String revision) {
        return ScmInfo.getOrCreate(buildUrl, buildCommitId, originUrl, originCommitId, revision);
    }

    private static BuildRecipe getRecipe(ScmInfo scmInfo) {
        List<BuildRecipe> byScmInfo = BuildRecipe.findByScmInfo(scmInfo);
        BuildRecipe buildRecipe;
        if (byScmInfo.size() == 1) {
            buildRecipe = byScmInfo.get(0);
        } else { // Zero or multiple and can't decide between them => create new
            buildRecipe = new BuildRecipe();
            buildRecipe.scmInfo = scmInfo;
        }
        return buildRecipe;
    }

    private Set<BuildTool> getBuildTools(Map<String, String> attributes) {
        // TODO filter bad keys out, or just set of good keys in
        return attributes.entrySet()
                .stream()
                .map(e -> BuildTool.getOrCreate(e.getKey(), e.getValue()))
                .collect(Collectors.toSet());
    }

    private long getMemoryRequired(BuildConfigurationRevisionRef buildConfigRevision) throws RemoteResourceException {
        BuildConfigurationRevision revision = buildConfigClient
                .getRevision(buildConfigRevision.getId(), buildConfigRevision.getRev());
        String memory = revision.getParameters()
                .getOrDefault(BuildConfigurationParameterKeys.BUILDER_POD_MEMORY.name(), DEFAULT_MEMORY);
        return (long) (Double.parseDouble(memory) * 1024 * 1024 * 1024);
    }

    private Map<PurlSha, org.jboss.pnc.buildkitchen.model.Artifact> persistArtifacts(
            Collection<Artifact> builtArtifacts) {
        Set<PurlSha> purls = StreamSupport.stream(builtArtifacts.spliterator(), false)
                .map(a -> new PurlSha(a.getPurl(), a.getSha256()))
                .collect(Collectors.toSet());

        HashMap<PurlSha, org.jboss.pnc.buildkitchen.model.Artifact> artifacts = new HashMap<>();
        artifacts.putAll(org.jboss.pnc.buildkitchen.model.Artifact.findByPurls(purls));
        purls.removeAll(artifacts.keySet());
        for (PurlSha purlSha : purls) {
            org.jboss.pnc.buildkitchen.model.Artifact artifact = new org.jboss.pnc.buildkitchen.model.Artifact(purlSha);
            artifact.persist();
            artifacts.put(purlSha, artifact);
        }
        return artifacts;
    }

}
