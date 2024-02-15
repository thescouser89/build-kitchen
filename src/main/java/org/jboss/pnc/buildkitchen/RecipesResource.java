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
import org.jboss.pnc.api.constants.BuildGenerator;
import org.jboss.pnc.buildkitchen.api.ArtifactDTO;
import org.jboss.pnc.buildkitchen.api.BuildRecipeDTO;
import org.jboss.pnc.buildkitchen.api.BuildToolDTO;
import org.jboss.pnc.buildkitchen.api.PNCBuild;
import org.jboss.pnc.buildkitchen.api.PurlSha;
import org.jboss.pnc.buildkitchen.api.Recipes;
import org.jboss.pnc.buildkitchen.api.ScmInfoDTO;
import org.jboss.pnc.buildkitchen.mapper.BuildRecipeMapper;
import org.jboss.pnc.buildkitchen.model.Artifact;
import org.jboss.pnc.buildkitchen.model.Build;
import org.jboss.pnc.buildkitchen.model.BuildRecipe;
import org.jboss.pnc.buildkitchen.model.BuildTool;
import org.jboss.pnc.buildkitchen.model.ScmInfo;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
@Transactional
public class RecipesResource implements Recipes {
    @Inject
    BuildRecipeMapper mapper;

    @Inject
    PncImporter pncImporter;

    @Override
    public BuildRecipeDTO getSpecific(long id) {
        return mapper.toResource(BuildRecipe.findById(id));
    }

    @Override
    public BuildRecipeDTO createRecipe(BuildRecipeDTO recipe) {
        ScmInfo scmInfo = getScmInfo(recipe.getScmInfo());
        Set<BuildTool> buildTools = getBuildTools(recipe.getBuildTools());
        BuildRecipe entity = mapper.toModel(recipe);
        entity.scmInfo = scmInfo;
        entity.buildTools = buildTools;
        entity.persist();
        return mapper.toResource(entity);
    }

    @Override
    public BuildRecipeDTO submitPNCBuild(PNCBuild build) {
        ScmInfo scmInfo = getScmInfo(build.getScmInfo());

        List<BuildRecipe> byScmInfo = BuildRecipe.findByScmInfo(scmInfo);
        BuildRecipe buildRecipe;
        if (byScmInfo.size() == 1) {
            buildRecipe = byScmInfo.get(0);
        } else { // Zero or multiple and can't decide between them => create new
            buildRecipe = new BuildRecipe();
            buildRecipe.scmInfo = scmInfo;
        }
        Set<BuildTool> buildTools = getBuildTools(build.getBuildTools());
        buildRecipe.buildScript = build.buildScript;
        buildRecipe.buildTools = buildTools;
        buildRecipe.memoryRequired = build.memory;
        buildRecipe.persist();

        Build buildEntity = new Build();
        buildEntity.recipe = buildRecipe;
        buildEntity.generator = BuildGenerator.PNC;
        buildEntity.buildId = build.getBuildId();
        buildEntity.buildDuration = build.getBuildDuration();
        buildEntity.buildTime = build.getBuildTime();
        buildEntity.memory = build.getMemory();
        buildEntity.image = build.getImage();
        buildEntity.builtArtifacts = new HashSet<>(persistArtifacts(build.builtArtifacts).values());
        buildEntity.persist();

        buildRecipe.builds.add(buildEntity);

        return mapper.toResource(buildRecipe);
    }

    @Override
    public BuildRecipeDTO submitPNCBuild(String buildId) {
        return mapper.toResource(pncImporter.importBuild(buildId));
    }

    private static ScmInfo getScmInfo(ScmInfoDTO scmInfoDto) {
        return ScmInfo.getOrCreate(
                scmInfoDto.getBuildScmUrl(),
                scmInfoDto.getBuildCommitId(),
                scmInfoDto.getOriginScmUrl(),
                scmInfoDto.getOriginCommitId(),
                scmInfoDto.getOriginRevision());
    }

    public static Set<BuildTool> getBuildTools(Set<BuildToolDTO> recipe) {
        return recipe.stream().map(bt -> BuildTool.getOrCreate(bt.identifier, bt.version)).collect(Collectors.toSet());
    }

    private Map<PurlSha, Artifact> persistArtifacts(Set<ArtifactDTO> artifactDTOS) {
        HashSet<PurlSha> purls = artifactDTOS.stream()
                .map(ArtifactDTO::getPurlSha)
                .collect(Collectors.toCollection(HashSet::new));
        HashMap<PurlSha, Artifact> artifacts = new HashMap<>();
        artifacts.putAll(Artifact.findByPurls(purls));
        purls.removeAll(artifacts.keySet());
        for (PurlSha purlSha : purls) {
            Artifact artifact = new Artifact(purlSha);
            artifact.persist();
            artifacts.put(purlSha, artifact);
        }
        return artifacts;
    }
}
