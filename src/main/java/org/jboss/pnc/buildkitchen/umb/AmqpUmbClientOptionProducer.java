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
package org.jboss.pnc.buildkitchen.umb;

import io.smallrye.common.annotation.Identifier;
import io.vertx.amqp.AmqpClientOptions;
import io.vertx.core.net.PfxOptions;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.nio.file.Path;

@Slf4j
@ApplicationScoped
public class AmqpUmbClientOptionProducer {

    @ConfigProperty(name = "build-kitchen.amqp.key")
    Path keyPath;
    @ConfigProperty(name = "build-kitchen.amqp.pass")
    String pass;

    @Produces
    @Identifier("umb")
    public AmqpClientOptions getClientOptions() {
        log.info("Setting up AMQP client options");

        return new AmqpClientOptions().setSsl(true)
                .setConnectTimeout(30 * 1000)
                .setReconnectInterval(5 * 1000)
                .setPfxKeyCertOptions(new PfxOptions().setPath(keyPath.toString()).setPassword(pass));
    }
}
