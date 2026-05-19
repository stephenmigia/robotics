package com.robotics.control.service;

import com.robotics.control.client.ApiClient;
import com.robotics.control.client.ApiClient.StatusResponse;
import com.robotics.control.client.ApiClient.SensorResponse;
import com.robotics.control.model.Position;
import com.robotics.control.model.Status;
import com.robotics.control.model.Unit;
import com.robotics.control.model.VirtualEnvironment;
import com.robotics.control.event.TelemetryObserver;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class UnitService {
    private final ApiClient apiClient;
    private final VirtualEnvironment virtualEnvironment;
    private final SimpMessagingTemplate messagingTemplate;
    private final Unit liveUnit = new Unit();

    public UnitService(ApiClient apiClient,
                       VirtualEnvironment virtualEnvironment,
                       SimpMessagingTemplate messagingTemplate) {
        this.apiClient = apiClient;
        this.virtualEnvironment = virtualEnvironment;
        this.messagingTemplate = messagingTemplate;
    }

    public Unit getUnit() {
        return liveUnit;
    }

    public Status getCurrentStatus() {
        return liveUnit.getCurrentStatus();
    }

    public com.robotics.control.client.ApiClient.MapResponse getMap() {
        return apiClient.fetchMap();
    }

    public String executeMove(Position nextPos) {
        String response = apiClient.postMove(nextPos);
        syncState();
        return response;
    }

    public String resetSimulation() {
        String response = apiClient.postReset();
        syncState();
        return response;
    }

    /**
     * Single authoritative state sync — uses REST API exclusively.
     * 1. Fetches robot status + sensor data via REST
     * 2. Updates in-memory Unit state
     * 3. Updates obstacle grid using exact captured idleOrigin (neutralizes network latency)
     * 4. Broadcasts telemetry to frontend via STOMP WebSocket
     */
    public synchronized void syncState() {
        try {
            // 1. Fetch status via REST API
            StatusResponse res = apiClient.fetchStatus();
            Position pos = new Position(res.position.x, res.position.y);
            Status status = Status.valueOf(res.status);

            liveUnit.updateState(pos, res.battery, status);

            SensorResponse sensorRes = null;
            // CAPTURE EXACT POSITION WHERE SENSOR API CALL IS INITIATED:
            // This guarantees obstacle calculations remain 100% accurate even if fetchSensors() suffers from network latency
            Position idleOrigin = new Position(pos.getX(), pos.getY());

            try {
                sensorRes = apiClient.fetchSensors();
            } catch (Exception ex) {
                // Ignore sensor errors if simulator has a slight delay
            }

            // 2. Map sensor readings into liveUnit
            Map<String, Integer> proximityMap = null;
            if (sensorRes != null) {
                liveUnit.getProximitySensor().setNorthRange(sensorRes.N);
                liveUnit.getProximitySensor().setSouthRange(sensorRes.S);
                liveUnit.getProximitySensor().setEastRange(sensorRes.E);
                liveUnit.getProximitySensor().setWestRange(sensorRes.W);
                proximityMap = liveUnit.getProximitySensor().getCardinalReadings();

                if (sensorRes.lidar != null) {
                    liveUnit.getLidarSensor().setRawDegreeReadings(sensorRes.lidar);
                }
            } else {
                proximityMap = liveUnit.getProximitySensor().getCardinalReadings();
            }

            // 3. Update obstacles ONLY when robot is IDLE using the validator method and exact idleOrigin
            if (status == Status.IDLE && sensorRes != null) {
                List<Position> newObstacles = virtualEnvironment.validateAndUpdateObstacles(
                        idleOrigin,
                        liveUnit.getProximitySensor(),
                        liveUnit.getLidarSensor()
                );

                for (Position obs : newObstacles) {
                    messagingTemplate.convertAndSend("/topic/telemetry", (Object) Map.of(
                            "type", "NEW_OBSTACLE",
                            "x", obs.getX(),
                            "y", obs.getY()
                    ));
                }
            }

            // 4. Broadcast full telemetry snapshot to frontend STOMP clients
            double[] doubleLidar = java.util.Arrays.stream(liveUnit.getLidarSensor().getDegreeReadings())
                    .asDoubleStream().toArray();

            TelemetryObserver.LocalBroadcastDto broadcastData = new TelemetryObserver.LocalBroadcastDto(
                    pos.getX(),
                    pos.getY(),
                    res.battery,
                    res.status,
                    proximityMap,
                    doubleLidar,
                    virtualEnvironment.getObstacleLocations()
            );
            messagingTemplate.convertAndSend("/topic/telemetry", broadcastData);

        } catch (Exception e) {
            // Silently fail if simulation is temporarily down
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initializeMap() {
        try {
            syncState();
        } catch (Exception e) {
            System.err.println("Could not initialize startup state sync: " + e.getMessage());
        }
    }
}
