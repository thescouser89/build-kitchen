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
package org.jboss.pnc.buildkitchen.api;

import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;
import java.util.Set;

@Value
@Builder
@Jacksonized
public class PNCBuild {

    @NotNull
    @ManyToOne
    public ScmInfoDTO scmInfo;

    @NotNull
    @ManyToMany
    public Set<BuildToolDTO> buildTools;

    @NotNull
    public String buildId;
    @NotNull
    public String buildScript;

    @NotNull
    @Valid
    public Set<ArtifactDTO> builtArtifacts;

    public String versionGenerated;

    @NotNull
    public Instant buildTime;

    public long buildDuration;

    public long memory;

    public String image;

}
