package com.robotics.control.event;

import org.springframework.context.ApplicationEvent;

public class TelemetryUpdateEvent extends ApplicationEvent {
    private final String payload;

    public TelemetryUpdateEvent(Object source, String payload) {
        super(source);
        this.payload = payload;
    }

    public String getPayload() {
        return payload;
    }
}
