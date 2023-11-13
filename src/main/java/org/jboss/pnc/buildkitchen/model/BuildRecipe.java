/**
 * JBoss, Home of Professional Open Source.
 * Copyright 2021 Red Hat, Inc., and individual contributors
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

    @NotNull
    @ManyToOne
    public ScmInfo scmInfo;

    @NotNull
    @ManyToMany
    public Set<BuildTool> buildTools = new HashSet<>();

    @NotNull
    @Column(columnDefinition = "TEXT")
    public String buildScript;

    public long memoryRequired;

    @NotNull
    @OneToMany(mappedBy = "recipe")
    public Set<Build> builds = new HashSet<>();

    @NotNull
    @OneToMany
    public Set<ShadedArtifact> shadedDependencies = new HashSet<>();

    @Column(columnDefinition = "TEXT")
    public String discrepancyWithUpstream;


    public static List<BuildRecipe> findByScmInfo(ScmInfo scmInfo){
        return find("""
                FROM BuildRecipe r
                WHERE r.scmInfo = :scmInfo
                """,
                Parameters.with("scmInfo", scmInfo)).list();
    }

    public static List<BuildRecipe> findByScmInfo(String url, String revision){
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
                """,
                Parameters.with("nurl", normalizedUrl).and("revision", revision)).list();
    }
}
