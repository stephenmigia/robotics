package com.robotics.control.client;

import com.robotics.control.model.Position;
import java.util.List;


public interface ApiClient {
    StatusResponse fetchStatus();
    String postMove(Position pos);
    SensorResponse fetchSensors();
    String postReset();
    MapResponse fetchMap();
    
    // Client DTOs
    public static class PositionDto {
        public int x;
        public int y;
        public PositionDto() {}
        public PositionDto(int x, int y) { this.x = x; this.y = y; }
    }

    public static class StatusResponse {
        public String id;
        public PositionDto position;
        public double battery;
        public String status;
    }

    public static class ProximityDto {
        public int N;
        public int S;
        public int E;
        public int W;
    }

    public static class SensorResponse {
        @com.fasterxml.jackson.annotation.JsonProperty("N")
        public int N;
        @com.fasterxml.jackson.annotation.JsonProperty("S")
        public int S;
        @com.fasterxml.jackson.annotation.JsonProperty("E")
        public int E;
        @com.fasterxml.jackson.annotation.JsonProperty("W")
        public int W;
        public double[] lidar;
    }

    public static class MapResponse {
        public int width;
        public int height;
        public int[][] grid;
    }
}
