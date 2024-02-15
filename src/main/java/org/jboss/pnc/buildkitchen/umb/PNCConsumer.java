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
package org.jboss.pnc.buildkitchen.umb;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.reactive.messaging.annotations.Blocking;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.jboss.pnc.buildkitchen.PncImporter;
import org.jboss.pnc.dto.Build;

import java.util.concurrent.CompletionStage;

@ApplicationScoped
@Slf4j
public class PNCConsumer {

    @Inject
    ObjectMapper objectMapper;

    @Inject
    PncImporter pncImporter;

    @Incoming("builds")
    @Blocking(ordered = false)
    public CompletionStage<Void> process(Message<String> message) {

        log.debug("Received new message via the AMQP consumer");
        log.debug("Message content: {}", message.getPayload());

        Build build;
        try {
            build = objectMapper.readValue(message.getPayload(), BuildStatusChanged.class).getBuild();
        } catch (JsonProcessingException e) {
            log.error("Unable to deserialize PNC build finished message, this is unexpected", e);
            return message.nack(e);
        }
        log.debug("Message properly deserialized");

        pncImporter.importBuild(build.getId());

        return message.ack();
    }
}
