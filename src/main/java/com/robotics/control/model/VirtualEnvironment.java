package com.robotics.control.model;

import com.robotics.control.sensor.LidarSensor;
import com.robotics.control.sensor.ProximitySensor;

import java.util.ArrayList;
import java.util.List;

public class VirtualEnvironment {
    private final int width = 21;
    private final int height = 21;
    private final boolean[][] grid = new boolean[21][21];

    public VirtualEnvironment() {}

    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public boolean[][] getGrid() { return grid; }

    public boolean isObstacle(int x, int y) {
        if (x >= 0 && x < width && y >= 0 && y < height) {
            return grid[x][y];
        }
        return true;
    }

    public List<Position> getObstacleLocations() {
        List<Position> list = new ArrayList<>();
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (grid[x][y]) list.add(new Position(x, y));
            }
        }
        return list;
    }

    public void clearObstacles() {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                grid[x][y] = false;
            }
        }
    }

    public void setObstacle(int x, int y, boolean isObstacle) {
        if (x >= 0 && x < width && y >= 0 && y < height) {
            grid[x][y] = isObstacle;
        }
    }

    public List<Position> updateObstacles(Position currentPos, ProximitySensor proximity, LidarSensor lidar) {
        return validateAndUpdateObstacles(currentPos, proximity, lidar);
    }

    private boolean isWithinBounds(Position pos) {
        return pos.getX() >= 0 && pos.getX() < width && pos.getY() >= 0 && pos.getY() < height;
    }

    public synchronized List<Position> validateAndUpdateObstacles(Position idleOrigin, ProximitySensor proximity, LidarSensor lidar) {
        int rx = idleOrigin.getX();
        int ry = idleOrigin.getY();
        List<Position> newlyAdded = new ArrayList<>();

        if (isWithinBounds(idleOrigin)) {
            grid[rx][ry] = false;
        }

        // Early Cardinal-Only Proximity Sensor mapping (sparse, diagonal-blind, low-resolution)
        int nRange = proximity.getNorthRange();
        if (nRange > 0 && nRange < 10) {
            Position p = new Position(rx, ry + nRange);
            if (isWithinBounds(p) && !(p.getX() == 0 && p.getY() == 0)) { grid[p.getX()][p.getY()] = true; newlyAdded.add(p); }
        }
        int sRange = proximity.getSouthRange();
        if (sRange > 0 && sRange < 10) {
            Position p = new Position(rx, ry - sRange);
            if (isWithinBounds(p) && !(p.getX() == 0 && p.getY() == 0)) { grid[p.getX()][p.getY()] = true; newlyAdded.add(p); }
        }
        int eRange = proximity.getEastRange();
        if (eRange > 0 && eRange < 10) {
            Position p = new Position(rx + eRange, ry);
            if (isWithinBounds(p) && !(p.getX() == 0 && p.getY() == 0)) { grid[p.getX()][p.getY()] = true; newlyAdded.add(p); }
        }
        int wRange = proximity.getWestRange();
        if (wRange > 0 && wRange < 10) {
            Position p = new Position(rx - wRange, ry);
            if (isWithinBounds(p) && !(p.getX() == 0 && p.getY() == 0)) { grid[p.getX()][p.getY()] = true; newlyAdded.add(p); }
        }
        return newlyAdded;
    }
}
