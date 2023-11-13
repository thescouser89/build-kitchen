INSERT INTO Artifact (id, purl)
VALUES (100, 'pkg:maven/foo.bar/foo@12.0.3.redhat-00001'),
       (200, 'pkg:maven/foo.bar/bar@12.0.3.redhat-00001'),
       (300, 'pkg:maven/foo.bar/shaded@12.0.3.redhat-00001');

INSERT INTO ShadedArtifact (id, artifact_id)
VALUES (100, 200);

INSERT INTO shadedartifact_artifact (shadedartifact_id, shadedartifacts_id)
VALUES (100, 300);

INSERT INTO BuildTool (id, identifier, version)
VALUES (100, 'JAVA', '11'),
       (200, 'MAVEN', '3.6');

INSERT INTO ScmInfo (id, buildScmUrl, buildCommitId, originScmUrl, originCommitId, originRevision)
VALUES (100, 'foo', 'bar', 'baz', 'xen', 'zan');

INSERT INTO BuildRecipe (id, memoryrequired, scminfo_id, buildscript, discrepancywithupstream)
VALUES (100, 4294967296, 100, 'mvn clean deploy', NULL);

INSERT INTO buildrecipe_buildtool (buildrecipe_id, buildtools_id)
VALUES (100, 100), (100, 200);

INSERT INTO buildrecipe_shadedartifact (buildrecipe_id, shadeddependencies_id)
VALUES (100, 100);

INSERT INTO Build (id, generator, buildduration, buildtime, memory, recipe_id, buildid, image, versiongenerated)
VALUES (100, 'PNC', 9015, '2023-12-24T01:02:03Z', 4294967296, 100, 'AAAMYWACIZZAA', 'pnc-builder-java11-maven362', '12.0.3.redhat-00001');

INSERT INTO build_artifact (build_id, builtartifacts_id)
VALUES (100, 100), (100, 200);

INSERT INTO build_buildtools (build_id, buildtools)
VALUES (100, 'java-11-openjdk-11.0.20.0.8-1.fc38.x86_64'),
       (100, 'maven-3.6.6-4.fc38.noarch');
