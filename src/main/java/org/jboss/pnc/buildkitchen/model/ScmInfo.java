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
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;

import java.util.Optional;

@Entity
@Table(
        uniqueConstraints = @UniqueConstraint(
                columnNames = { "buildScmUrl", "buildCommitId", "originScmUrl", "originCommitId" }))
public class ScmInfo extends PanacheEntity {

    /**
     * Normalized URL of sources that were actually used for running the build.
     * <p>
     * E.g. internal URL in PNC.
     */
    @NotNull
    public String buildScmUrl;

    /**
     * Revision that was actually used for running the build. It is and URL after applying any changes.
     * <p>
     * E.g. internal revision with alignment done in PNC.
     */
    @NotNull
    public String buildCommitId;

    /**
     * Normalized URL of the original source code.
     * <p>
     * E.g. midstream or upstream URL in PNC.
     */
    @NotNull
    public String originScmUrl;

    /**
     * Revision that was used as input for the system.
     * <p>
     * E.g. midstream or upstream revision before applying alignment in PNC.
     */
    @NotNull
    public String originCommitId;

    /**
     * Tag or branch name that the sources were taken from. This may be the input from user, that gets resolved to
     * {@link ScmInfo#originCommitId}.
     */
    public String originRevision;

    public ScmInfo() {
    }

    public ScmInfo(
            String buildScmUrl,
            String buildCommitId,
            String originScmUrl,
            String originCommitId,
            String originRevision) {
        this.buildScmUrl = buildScmUrl;
        this.buildCommitId = buildCommitId;
        this.originScmUrl = originScmUrl;
        this.originCommitId = originCommitId;
        this.originRevision = originRevision;
    }

    public void setBuildScmUrl(String buildScmUrl) {
        this.buildScmUrl = normalizeUrl(buildScmUrl);
    }

    public void setOriginScmUrl(String originScmUrl) {
        this.originScmUrl = normalizeUrl(originScmUrl);
    }

    public static String normalizeUrl(String url) {
        return url; // TODO: normalizeURL
    }

    public static ScmInfo getOrCreate(
            String buildScmUrl,
            String buildCommitId,
            String originScmUrl,
            String originCommitId,
            String originRevision) {
        String normalizedBuildScmUrl = ScmInfo.normalizeUrl(buildScmUrl);
        String normalizedOriginScmUrl = ScmInfo.normalizeUrl(originScmUrl);
        Optional<ScmInfo> entity = find(
                """
                        FROM ScmInfo scm
                        WHERE
                                scm.buildScmUrl = :buildScmUrl
                            AND scm.buildCommitId = :buildCommitId
                            AND scm.originScmUrl = :originScmUrl
                            AND scm.originCommitId = :originCommitId
                        """,
                Parameters.with("buildScmUrl", normalizedBuildScmUrl)
                        .and("buildCommitId", buildCommitId)
                        .and("originScmUrl", normalizedOriginScmUrl)
                        .and("originCommitId", originCommitId))
                .singleResultOptional();
        return entity
                .orElseGet(() -> createNew(buildScmUrl, buildCommitId, originScmUrl, originCommitId, originRevision));
    }

    public static ScmInfo createNew(
            String buildScmUrl,
            String buildCommitId,
            String originScmUrl,
            String originCommitId,
            String originRevision) {
        ScmInfo scmInfo = new ScmInfo(buildScmUrl, buildCommitId, originScmUrl, originCommitId, originRevision);
        persist(scmInfo);
        return scmInfo;
    }

}
