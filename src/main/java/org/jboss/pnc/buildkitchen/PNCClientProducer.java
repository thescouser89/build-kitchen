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

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.pnc.client.BuildClient;
import org.jboss.pnc.client.BuildConfigurationClient;
import org.jboss.pnc.client.Configuration;

import java.net.URL;
import java.util.function.Supplier;

@ApplicationScoped
public class PNCClientProducer {
    private BuildClient buildClient;
    private BuildConfigurationClient buildConfigClient;

    @ConfigProperty(name = "build-kitchen.pnc.url")
    URL pncURL;

    @PostConstruct
    public void initClients() {
        System.err.println("Connecting to " + pncURL);
        Configuration configuration = Configuration.builder()
                .host(pncURL.getHost())
                .port(pncURL.getPort())
                .protocol(pncURL.getProtocol())
                .build();
        buildClient = new BuildClient(configuration);
        buildConfigClient = new BuildConfigurationClient(configuration);
    }

    @Produces
    public BuildClient buildClient() {
        return buildClient;
    }

    @Produces
    public BuildConfigurationClient buildConfigClient() {
        return buildConfigClient;
    }
}
