package org.bf2.cos.fleetshard.support.client;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.Event;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectReference;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.utils.KubernetesResourceUtil;

@ApplicationScoped
public class EventClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(EventClient.class);

    @Inject
    KubernetesClient kubernetesClient;

    public enum EventType {

        NORMAL("Normal"),
        WARNING("Warning");

        static final EventType[] VALS = values();
        private String text;

        EventType(String text) {
            this.text = text;
        }

        public String getText() {
            return text;
        }

        public static Optional<EventType> fromText(String text) {
            for (int i = 0; i < VALS.length; i++) {
                if (VALS[i].getText().equals(text)) {
                    return Optional.of(VALS[i]);
                }
            }
            return Optional.empty();
        }
    }

    public void broadcastNormal(String reason, String message, Object... args) {
        final String formattedMessage = String.format(message, args);
        broadcast(EventType.NORMAL, reason, formattedMessage);
    }

    public void broadcastWarning(String reason, String message, Object... args) {
        final String formattedMessage = String.format(message, args);
        broadcast(EventType.WARNING, reason, formattedMessage);
    }

    public void broadcastNormal(String reason, String message, HasMetadata involved, Object... args) {
        final String formattedMessage = String.format(message, args);
        broadcast(EventType.NORMAL, reason, formattedMessage, involved);
    }

    public void broadcastWarning(String reason, String message, HasMetadata involved, Object... args) {
        final String formattedMessage = String.format(message, args);
        broadcast(EventType.WARNING, reason, formattedMessage, involved);
    }

    public void broadcast(EventType type, String reason, String message, HasMetadata involved) {
        Event event = createEvent(type, reason, message);

        ObjectReference ref = new ObjectReference();
        ref.setApiVersion(involved.getApiVersion());
        ref.setKind(involved.getKind());
        ref.setName(involved.getMetadata().getName());
        ref.setNamespace(involved.getMetadata().getNamespace());
        ref.setUid(involved.getMetadata().getUid());
        event.setInvolvedObject(ref);

        KubernetesResourceUtil.getOrCreateMetadata(event).setNamespace(involved.getMetadata().getNamespace());

        broadcast(event);
    }

    public void broadcast(EventType type, String reason, String message) {
        broadcast(createEvent(type, reason, message));
    }

    private Event createEvent(EventType type, String reason, String message) {
        Event event = new Event();

        event.setType(type.getText());
        event.setReason(reason);
        event.setMessage(message);
        event.setLastTimestamp(ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT));

        KubernetesResourceUtil.getOrCreateMetadata(event).setGenerateName("cos-fleetshard-sync");
        return event;
    }

    public void broadcast(Event event) {
        EventType eventType = EventType.fromText(event.getType()).orElse(EventType.NORMAL);
        switch (eventType) {
            case NORMAL:
                LOGGER.info("Broadcasting message ({}). Event: {}", event.getMessage(), event);
                break;
            case WARNING:
                LOGGER.warn("Broadcasting message ({}). Event: {}", event.getMessage(), event);
                break;
        }

        try {
            kubernetesClient.v1()
                .events()
                .inNamespace(event.getMetadata().getNamespace())
                .create(event);
        } catch (Exception e) {
            LOGGER.warn("Error while broadcasting events", e);
        }
    }

}
