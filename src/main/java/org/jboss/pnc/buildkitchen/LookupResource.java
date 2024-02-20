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
package org.jboss.pnc.buildkitchen;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.pnc.buildkitchen.api.BuildRecipeDTO;
import org.jboss.pnc.buildkitchen.api.Lookup;
import org.jboss.pnc.buildkitchen.mapper.BuildRecipeMapper;
import org.jboss.pnc.buildkitchen.model.BuildRecipe;

import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class LookupResource implements Lookup {
    @Inject
    BuildRecipeMapper buildRecipeMapper;

    @Override
    public List<BuildRecipeDTO> lookupByScmRevision(String url, String revision) {
        return BuildRecipe.findByScmInfo(url, revision)
                .stream()
                .map(buildRecipeMapper::toResource)
                .collect(Collectors.toList());
    }

    @Override
    public List<BuildRecipeDTO> lookupByPurl(String purl) {
        return BuildRecipe.findByPurl(purl)
                .stream()
                .map(buildRecipeMapper::toResource)
                .collect(Collectors.toList());
    }
}
