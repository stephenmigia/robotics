package com.robotics.control.websocket;

import com.robotics.control.event.TelemetryUpdateEvent;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class TelemetryWebSocketClient {

    private static final Logger log = LoggerFactory.getLogger(TelemetryWebSocketClient.class);

    private final String simulatorWsUrl;
    private final ApplicationEventPublisher eventPublisher;
    private WebSocketSession webSocketSession;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public TelemetryWebSocketClient(@Value("${simulator.ws.url:ws://localhost:5000/ws/telemetry}") String simulatorWsUrl,
                                    ApplicationEventPublisher eventPublisher) {
        this.simulatorWsUrl = simulatorWsUrl;
        this.eventPublisher = eventPublisher;
    }

    @PostConstruct
    public void connect() {
        StandardWebSocketClient client = new StandardWebSocketClient();
        try {
            client.execute(new TextWebSocketHandler() {
                @Override
                protected void handleTextMessage(WebSocketSession session, TextMessage message) {
                    eventPublisher.publishEvent(new TelemetryUpdateEvent(this, message.getPayload()));
                }

                @Override
                public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
                    log.warn("Simulator WebSocket connection closed ({}). Scheduling reconnect...", status);
                    scheduleReconnect();
                }

                @Override
                public void handleTransportError(WebSocketSession session, Throwable exception) {
                    log.error("Simulator WebSocket transport error: {}", exception.getMessage());
                }
            }, simulatorWsUrl).whenComplete((result, ex) -> {
                if (ex != null) {
                    log.debug("Simulator WebSocket handshake failed. Retrying in 5s...");
                    scheduleReconnect();
                } else {
                    log.info("--- Successfully connected to Simulator WebSocket at {} ---", simulatorWsUrl);
                    webSocketSession = result;
                }
            });
        } catch (Exception e) {
            log.debug("Simulator WebSocket connection attempt failed. Retrying in 5s...");
            scheduleReconnect();
        }
    }

    private void scheduleReconnect() {
        scheduler.schedule(this::connect, 5, TimeUnit.SECONDS);
    }

    @PreDestroy
    public void disconnect() {
        try {
            if (webSocketSession != null && webSocketSession.isOpen()) {
                webSocketSession.close();
            }
            scheduler.shutdownNow();
        } catch (Exception e) {
            // ignore
        }
    }
}
