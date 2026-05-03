package com.robotics.control.sensor;

import com.fasterxml.jackson.annotation.JsonProperty;
import tools.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

public class ProximitySensor implements Sensor {
    private int northRange = 5;
    private int southRange = 5;
    private int eastRange = 5;
    private int westRange = 5;
    private final int maxRange = 5;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public ProximitySensor() {}

    public ProximitySensor(int north, int south, int east, int west) {
        this.northRange = north;
        this.southRange = south;
        this.eastRange = east;
        this.westRange = west;
    }

    public int getNorthRange() { return northRange; }
    public void setNorthRange(int northRange) { this.northRange = northRange; }

    public int getSouthRange() { return southRange; }
    public void setSouthRange(int southRange) { this.southRange = southRange; }

    public int getEastRange() { return eastRange; }
    public void setEastRange(int eastRange) { this.eastRange = eastRange; }

    public int getWestRange() { return westRange; }
    public void setWestRange(int westRange) { this.westRange = westRange; }

    public int getMaxRange() { return maxRange; }

    public Map<String, Integer> getCardinalReadings() {
        Map<String, Integer> readings = new HashMap<>();
        readings.put("N", northRange);
        readings.put("S", southRange);
        readings.put("E", eastRange);
        readings.put("W", westRange);
        return readings;
    }

    @Override
    public void updateReadings(String rawJsonData) {
        try {
            ProximityData data = objectMapper.readValue(rawJsonData, ProximityData.class);
            this.northRange = data.N;
            this.southRange = data.S;
            this.eastRange = data.E;
            this.westRange = data.W;
        } catch (Exception e) {
            // Fail silently or log
        }
    }

    private static class ProximityData {
        @JsonProperty("N")
        public int N;
        @JsonProperty("S")
        public int S;
        @JsonProperty("E")
        public int E;
        @JsonProperty("W")
        public int W;
    }
}
