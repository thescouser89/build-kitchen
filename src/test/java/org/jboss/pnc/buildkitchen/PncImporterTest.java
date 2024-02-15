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

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.jboss.pnc.api.constants.BuildGenerator;
import org.jboss.pnc.buildkitchen.mapper.BuildRecipeMapper;
import org.jboss.pnc.buildkitchen.model.Build;
import org.jboss.pnc.buildkitchen.model.BuildRecipe;
import org.jboss.pnc.buildkitchen.model.BuildTool;
import org.jboss.pnc.buildkitchen.model.ScmInfo;
import org.jboss.pnc.client.RemoteResourceException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
class PncImporterTest {

    @Inject
    PncImporter pncImporter;

    @Inject
    BuildRecipeMapper mapper;

    @Test
    void importBuild() throws RemoteResourceException {
        BuildRecipe buildRecipe = pncImporter.importBuild("A6IR2VCQCDYAA");

        assertNotNull(buildRecipe);
        assertEquals("mvn clean deploy", buildRecipe.buildScript);
        assertEquals(3_373_159_940L, buildRecipe.memoryRequired);
        ScmInfo scmInfo = buildRecipe.scmInfo;
        assertNotNull(scmInfo);
        assertEquals("abcdef0123456789abcdef0123456789abcdef01", scmInfo.buildCommitId);
        assertEquals("123456789abc123456789abc123465789abc1234", scmInfo.originCommitId);
        assertEquals("4.0.4-RI", scmInfo.originRevision);
        assertEquals("https://example.com/gerrit/eclipse-ee4j/jaxb-ri.git", scmInfo.buildScmUrl);
        assertEquals("https://github.com/eclipse-ee4j/jaxb-ri.git", scmInfo.originScmUrl);
        Set<BuildTool> buildTools = buildRecipe.buildTools;
        assertNotNull(buildTools);
        assertEquals(3, buildTools.size());
        Set<Build> builds = buildRecipe.builds;
        assertNotNull(builds);
        assertEquals(1, builds.size());
        Build build = builds.iterator().next();
        assertEquals(3_373_159_940L, build.memory);
        assertEquals(Instant.parse("2024-02-13T14:29:32.388Z"), build.buildTime);
        assertEquals(214, build.buildDuration);
        assertEquals(BuildGenerator.PNC, build.generator);
        assertEquals("example.com/namespace/builder:1.0.0", build.image);
        assertEquals("4.0.4.temporary-redhat-00001", build.versionGenerated);

        System.out.println(mapper.toResource(buildRecipe));
    }
}