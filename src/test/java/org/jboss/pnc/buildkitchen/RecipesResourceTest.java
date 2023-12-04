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

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import org.jboss.pnc.buildkitchen.api.ArtifactDTO;
import org.jboss.pnc.buildkitchen.api.BuildRecipeDTO;
import org.jboss.pnc.buildkitchen.api.BuildToolDTO;
import org.jboss.pnc.buildkitchen.api.PNCBuild;
import org.jboss.pnc.buildkitchen.api.Recipes;
import org.jboss.pnc.buildkitchen.api.ScmInfoDTO;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
@TestHTTPEndpoint(Recipes.class)
public class RecipesResourceTest {

    @Test
    public void testGetSpecific() {
        BuildRecipeDTO recipe = given().when()
                .get("/100")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .as(BuildRecipeDTO.class);

        assertEquals(recipe.getId(), 100);
        assertEquals(recipe.getBuildScript(), "mvn clean deploy");
    }

    @Test
    public void testCreateRecipe() {
        BuildRecipeDTO requestObject = BuildRecipeDTO.builder()
                .scmInfo(
                        ScmInfoDTO.builder()
                                .buildScmUrl("https://internal.example.com/project-ncl/build-kitchen.git")
                                .originCommitId("fe305f0df59edaf7b40620ec7290749dcccfa228")
                                .originScmUrl("https://github.com/project-ncl/build-kitchen.git")
                                .originCommitId("fe305f0df59edaf7b40620ec7290749dcccfa228")
                                .originRevision("main")
                                .build())
                .buildScript("mvn clean deploy -DskipTests")
                .memoryRequired(2 * 1024 * 1024 * 1023)
                .buildTools(Set.of(BuildToolDTO.builder().identifier("JAVA").version("11").build()))
                .build();

        BuildRecipeDTO recipe = given().contentType("application/json")
                .body(requestObject)
                .when()
                .post()
                .then()
                .statusCode(200)
                .extract()
                .body()
                .as(BuildRecipeDTO.class);

        assertNotNull(recipe.getId());
        assertNotNull(recipe.getScmInfo().getId());
        assertEquals(recipe.getBuildScript(), requestObject.getBuildScript());
    }

    @Test
    public void testSubmitPNCBuild() {
        PNCBuild requestObject = PNCBuild.builder()
                .buildId("AAA8JS7F82HAA")
                .buildDuration(2 * 3600 + 13 * 60 + 5)
                .buildScript("mvn clean deploy -DskipTests")
                .buildTime(Instant.now())
                .scmInfo(
                        ScmInfoDTO.builder()
                                .buildScmUrl("https://internal.example.com/project-ncl/build-kitchen.git")
                                .originCommitId("fe305f0df59edaf7b40620ec7290749dcccfa228")
                                .originScmUrl("https://github.com/project-ncl/build-kitchen.git")
                                .originCommitId("fe305f0df59edaf7b40620ec7290749dcccfa228")
                                .originRevision("main")
                                .build())
                .buildTools(Set.of(BuildToolDTO.builder().identifier("JAVA").version("11").build()))
                .image("pnc-image-java11")
                .memory(2 * 1024 * 1024 * 1023)
                .builtArtifacts(
                        Set.of(
                                new ArtifactDTO(
                                        "pkg:maven/junit/junit@1.2.3.redhat-00001",
                                        "123456789012345678901234567890ab"),
                                new ArtifactDTO(
                                        "pkg:maven/junit/junit-runner@1.2.3.redhat-00001",
                                        "123456789012345678901234567890cd")))
                .versionGenerated("1.2.3.redhat-00001")
                .build();

        BuildRecipeDTO recipe = given().contentType("application/json")
                .body(requestObject)
                .when()
                .post("/pnc-build")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .as(BuildRecipeDTO.class);

        assertNotNull(recipe.getId());
        assertNotNull(recipe.getScmInfo().getId());
        assertEquals(recipe.getBuildScript(), requestObject.getBuildScript());
    }

}