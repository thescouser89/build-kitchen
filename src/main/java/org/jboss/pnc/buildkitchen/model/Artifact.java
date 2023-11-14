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
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import org.jboss.pnc.buildkitchen.api.PurlSha;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"purl", "sha256"}))
public class Artifact extends PanacheEntity {

    @NotNull
    public String purl;

    @NotNull
    public String sha256;

    public PurlSha getPurlSha(){
        return new PurlSha(purl, sha256);
    }

    public Artifact() {
    }

    public Artifact(PurlSha purlSha) {
        this.purl = purlSha.purl();
        this.sha256 = purlSha.sha256();
    }

    public static Map<PurlSha, Artifact> findByPurls(Set<PurlSha> purls) {
        List<PurlSha> purlShas = new ArrayList<>(purls);
        String query = "FROM Artifact a WHERE ";
        Map<String, String> params = new HashMap<>();
        for (int i = 0; i< purlShas.size(); i++){
            if(i > 0) {
                query += " OR ";
            }
            String purlKey = "purl" + i;
            String shaKey = "sha" + i;
            query += "(a.purl = :" + purlKey + " AND a.sha256 = :" + shaKey + ")";
            PurlSha purlSha = purlShas.get(i);
            params.put(purlKey, purlSha.purl());
            params.put(shaKey, purlSha.sha256());
        }


        return find(query, params).<Artifact>stream()
                .collect(Collectors.toMap(Artifact::getPurlSha, Function.identity()));
    }
}
