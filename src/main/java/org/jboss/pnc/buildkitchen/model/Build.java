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
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotNull;
import org.jboss.pnc.api.constants.BuildGenerator;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
public class Build extends PanacheEntity {

    @NotNull
    @ManyToOne
    public BuildRecipe recipe;

    @NotNull
    @Enumerated(EnumType.STRING)
    public BuildGenerator generator;

    @NotNull
    public String buildId;

    @NotNull
    @ManyToMany
    public Set<Artifact> builtArtifacts = new HashSet<>();

    public String versionGenerated;

    @NotNull
    public Instant buildTime;
    public long buildDuration;

    public long memory;

    public String image;

    @NotNull
    @ElementCollection
    public Set<String> buildTools = new HashSet<>();

}
