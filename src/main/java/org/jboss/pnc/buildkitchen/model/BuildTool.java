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
package org.jboss.pnc.buildkitchen.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import io.quarkus.panache.common.Parameters;
import jakarta.persistence.Entity;
import jakarta.validation.constraints.NotNull;

import java.util.Optional;

@Entity
public class BuildTool extends PanacheEntity {

    /**
     * Name of the tool.
     * <p>
     * E.g. JDK, gradle, maven, SBT, ant
     */
    @NotNull
    public String identifier;

    /**
     * Major version of the tool.
     */
    @NotNull
    public String version;

    public static BuildTool getOrCreate(String identifier, String version) {
        Optional<BuildTool> entity = find("""
                FROM BuildTool bt
                WHERE
                        bt.identifier = :identifier
                    AND bt.version = :version
                """, Parameters.with("identifier", identifier).and("version", version)).singleResultOptional();
        return entity.orElseGet(() -> createNew(identifier, version));
    }

    public static BuildTool createNew(String identifier, String version) {
        BuildTool buildTool = new BuildTool();
        buildTool.identifier = identifier;
        buildTool.version = version;
        persist(buildTool);
        return buildTool;
    }
}
