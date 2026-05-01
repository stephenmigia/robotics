package com.robotics.control.websocket;

import com.robotics.control.event.TelemetryUpdateEvent;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class TelemetryWebSocketClient {

    private static final Logger log = LoggerFactory.getLogger(TelemetryWebSocketClient.class);

    private final String simulatorWsUrl;
    private final ApplicationEventPublisher eventPublisher;
    private WebSocketSession webSocketSession;

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
            }, simulatorWsUrl).whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Simulator WebSocket initial connection failed: {}", ex.getMessage());
                } else {
                    log.info("--- Connected to Simulator WebSocket (Non-reconnecting) ---");
                    webSocketSession = result;
                }
            });
        } catch (Exception e) {
            log.error("WebSocket connection failure: {}", e.getMessage());
        }
    }

    @PreDestroy
    public void disconnect() {
        try {
            if (webSocketSession != null && webSocketSession.isOpen()) {
                webSocketSession.close();
            }
        } catch (Exception e) {
            // ignore
        }
    }
}
