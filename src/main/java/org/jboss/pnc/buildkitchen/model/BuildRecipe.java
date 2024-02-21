/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2023-2024 Red Hat, Inc., and individual contributors
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
package org.jboss.pnc.buildkitchen.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import io.quarkus.panache.common.Parameters;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
public class BuildRecipe extends PanacheEntity {

    /**
     * Source Code Management coordinates used in this build recipe.
     */
    @NotNull
    @ManyToOne
    public ScmInfo scmInfo;

    /**
     * Collections of build tools used for running this recipe.
     */
    @NotNull
    @ManyToMany
    public Set<BuildTool> buildTools = new HashSet<>();

    /**
     * Script that is used for execution of the build recipe.
     */
    @NotNull
    @Column(columnDefinition = "TEXT")
    public String buildScript;

    /**
     * How much memory is needed for executing the build recipe in bytes.
     */
    public long memoryRequired;

    /**
     * Builds that were produced by (a version of) this recipe.
     */
    @NotNull
    @OneToMany(mappedBy = "recipe")
    public Set<Build> builds = new HashSet<>();

    /**
     * List of artifacts that were shaded into the output.
     * <p>
     * JBS specific.
     */
    @NotNull
    @OneToMany
    public Set<ShadedArtifact> shadedDependencies = new HashSet<>();

    /**
     * Difference of the resulting artifacts with upstram. Diff-like format.
     * <p>
     * JBS specific.
     */
    @Column(columnDefinition = "TEXT")
    public String discrepancyWithUpstream;

    public static List<BuildRecipe> findByScmInfo(ScmInfo scmInfo) {
        return find("""
                FROM BuildRecipe r
                WHERE r.scmInfo = :scmInfo
                """, Parameters.with("scmInfo", scmInfo)).list();
    }

    public static List<BuildRecipe> findByScmInfo(String url, String revision) {
        String normalizedUrl = ScmInfo.normalizeUrl(url);
        return find("""
                FROM BuildRecipe r
                WHERE (
                       r.scmInfo.buildScmUrl = :nurl
                    OR r.scmInfo.originScmUrl = :nurl
                  ) AND (
                        r.scmInfo.buildCommitId = :revision
                     OR r.scmInfo.originCommitId = :revision
                     OR r.scmInfo.originRevision = :revision
                  )
                """, Parameters.with("nurl", normalizedUrl).and("revision", revision)).list();
    }

    public static List<BuildRecipe> findByPurl(String purl) {
        return find("""
                FROM BuildRecipe r
                JOIN r.builds b
                JOIN b.builtArtifacts a
                WHERE a.purl = :purl
                """, Parameters.with("purl", purl)).list();
    }

    public static List<BuildRecipe> findByScmUrlAndVersion(String url, String version) {
        String normalizedUrl = ScmInfo.normalizeUrl(url);
        return find("""
                FROM BuildRecipe r
                JOIN r.builds b
                WHERE (
                       r.scmInfo.buildScmUrl = :nurl
                    OR r.scmInfo.originScmUrl = :nurl
                  ) AND b.versionGenerated = :version
                """, Parameters.with("nurl", normalizedUrl).and("version", version)).list();
    }
}
