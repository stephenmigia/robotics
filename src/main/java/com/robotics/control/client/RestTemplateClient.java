package com.robotics.control.client;

import com.robotics.control.model.Position;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class RestTemplateClient implements ApiClient {

    private final RestClient restClient;

    public RestTemplateClient(@Value("${simulator.url}") String simulatorUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(simulatorUrl)
                .build();
    }

    @Override
    public StatusResponse fetchStatus() {
        return restClient.get()
                .uri("/api/status")
                .retrieve()
                .body(StatusResponse.class);
    }

    @Override
    public String postMove(Position pos) {
        return restClient.post()
                .uri("/api/move")
                .contentType(MediaType.APPLICATION_JSON)
                .body(pos)
                .retrieve()
                .body(String.class);
    }

    @Override
    public SensorResponse fetchSensors() {
        return restClient.get()
                .uri("/api/sensor")
                .retrieve()
                .body(SensorResponse.class);
    }

    @Override
    public String postReset() {
        return restClient.post()
                .uri("/api/reset")
                .retrieve()
                .body(String.class);
    }

    @Override
    public MapResponse fetchMap() {
        return restClient.get()
                .uri("/api/map")
                .retrieve()
                .body(MapResponse.class);
    }
}
