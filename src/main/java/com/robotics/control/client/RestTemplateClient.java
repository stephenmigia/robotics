package com.robotics.control.client;

import com.robotics.control.model.Position;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Component
public class RestTemplateClient implements ApiClient {

    private static final Logger log = LoggerFactory.getLogger(RestTemplateClient.class);
    private final RestClient restClient;

    public RestTemplateClient(@Value("${simulator.url}") String simulatorUrl) {
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(2))
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(3));

        this.restClient = RestClient.builder()
                .baseUrl(simulatorUrl)
                .requestFactory(requestFactory)
                .requestInterceptor((request, body, execution) -> {
                    String bodyStr = (body != null && body.length > 0) 
                            ? new String(body, java.nio.charset.StandardCharsets.UTF_8) 
                            : "";
                    if (!bodyStr.isEmpty()) {
                        log.info("--- [SIMULATOR API REQUEST] {} {} | Body: {} ---", 
                                request.getMethod(), request.getURI(), bodyStr);
                    } else {
                        log.info("--- [SIMULATOR API REQUEST] {} {} ---", 
                                request.getMethod(), request.getURI());
                    }
                    return execution.execute(request, body);
                })
                .build();
    }

    @Override
    @Retryable(retryFor = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 500, multiplier = 1.5))
    public StatusResponse fetchStatus() {
        return restClient.get()
                .uri("/api/status")
                .retrieve()
                .body(StatusResponse.class);
    }

    @Override
    @Retryable(retryFor = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 500, multiplier = 1.5))
    public String postMove(Position pos) {
        return restClient.post()
                .uri("/api/move")
                .contentType(MediaType.APPLICATION_JSON)
                .body(pos)
                .retrieve()
                .body(String.class);
    }

    @Override
    @Retryable(retryFor = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 500, multiplier = 1.5))
    public SensorResponse fetchSensors() {
        return restClient.get()
                .uri("/api/sensor")
                .retrieve()
                .body(SensorResponse.class);
    }

    @Override
    @Retryable(retryFor = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 500, multiplier = 1.5))
    public String postReset() {
        return restClient.post()
                .uri("/api/reset")
                .retrieve()
                .body(String.class);
    }

    @Override
    @Retryable(retryFor = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 500, multiplier = 1.5))
    public MapResponse fetchMap() {
        return restClient.get()
                .uri("/api/map")
                .retrieve()
                .body(MapResponse.class);
    }
}
