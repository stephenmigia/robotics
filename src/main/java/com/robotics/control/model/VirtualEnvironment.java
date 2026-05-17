package com.robotics.control.model;

import com.robotics.control.sensor.LidarSensor;
import com.robotics.control.sensor.ProximitySensor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class VirtualEnvironment {
    private final int width = 21;
    private final int height = 21;
    private final int obstacleCount = 40;

    private final boolean[][] grid = new boolean[width][height];

    public VirtualEnvironment() {
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getObstacleCount() {
        return obstacleCount;
    }

    public List<Position> getObstacleLocations() {
        List<Position> list = new ArrayList<>();
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (grid[x][y]) {
                    list.add(new Position(x, y));
                }
            }
        }
        return list;
    }

    public boolean isWithinBounds(Position pos) {
        return pos.getX() >= 0 && pos.getX() < width && pos.getY() >= 0 && pos.getY() < height;
    }

    public boolean isObstacleAt(Position pos) {
        if (pos == null || !isWithinBounds(pos)) {
            return false;
        }
        return grid[pos.getX()][pos.getY()];
    }

    public boolean isMoveAvailable(Position target) {
        return isWithinBounds(target) && !isObstacleAt(target);
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

    /**
     * Legacy wrapper for compatibility.
     */
    public List<Position> updateObstacles(Position currentPos, ProximitySensor proximity, LidarSensor lidar) {
        return validateAndUpdateObstacles(currentPos, proximity, lidar);
    }

    /**
     * Validator method that takes sensor data while robot is idle and re-adds or rearranges 
     * obstacles around the object to make up for network latency.
     * 
     * Uses the exact recorded idleOrigin position where the sensor API call was initiated.
     * Clears any false/distorted obstacles along the free path rays and sets the true obstacle coordinates.
     */
    public synchronized List<Position> validateAndUpdateObstacles(Position idleOrigin, ProximitySensor proximity, LidarSensor lidar) {
        int rx = idleOrigin.getX();
        int ry = idleOrigin.getY();

        List<Position> newlyAdded = new ArrayList<>();

        // Ensure the robot's origin cell is free of obstacles
        if (isWithinBounds(idleOrigin)) {
            grid[rx][ry] = false;
        }

        int[] angles = { 0, 45, 90, 135, 180, 225, 270, 315 };
        for (int theta : angles) {
            double rawD = lidar.getRawReadingAtDegree(theta);

            if (rawD <= 0 || rawD >= lidar.getMaxRange()) {
                // If no obstacle is detected within maxRange, validate and clear any false obstacles along this ray up to maxRange
                int stepX = getStepX(theta);
                int stepY = getStepY(theta);
                for (int s = 1; s < lidar.getMaxRange(); s++) {
                    int cx = rx + s * stepX;
                    int cy = ry + s * stepY;
                    Position checkPos = new Position(cx, cy);
                    if (isWithinBounds(checkPos) && !(cx == 0 && cy == 0)) {
                        grid[cx][cy] = false;
                    }
                }
                continue;
            }

            int stepX = getStepX(theta);
            int stepY = getStepY(theta);
            int steps = (int) Math.ceil(rawD);

            // 1. VALIDATOR STEP: Clear any distorted/false obstacles in the free space between robot and obstacle
            for (int s = 1; s < steps; s++) {
                int cx = rx + s * stepX;
                int cy = ry + s * stepY;
                Position checkPos = new Position(cx, cy);
                if (isWithinBounds(checkPos) && !(cx == 0 && cy == 0)) {
                    grid[cx][cy] = false;
                }
            }

            // 2. RE-ADD / REARRANGE STEP: Set the true obstacle at the verified coordinate
            int ox = rx + steps * stepX;
            int oy = ry + steps * stepY;
            Position obsPos = new Position(ox, oy);

            if (isWithinBounds(obsPos) && !(ox == 0 && oy == 0)) {
                if (!grid[ox][oy]) {
                    System.out.println("Validator: Re-adding/Rearranging obstacle at (" + ox + ", " + oy +
                            ") | raw=" + rawD + " steps=" + steps + " angle=" + theta +
                            " from idleOrigin(" + rx + ", " + ry + ")");
                    grid[ox][oy] = true;
                    newlyAdded.add(obsPos);
                }
            }
        }
        return newlyAdded;
    }

    private int getStepX(int theta) {
        switch (theta) {
            case 0: case 45: case 315: return 1;
            case 135: case 180: case 225: return -1;
            default: return 0;
        }
    }

    private int getStepY(int theta) {
        switch (theta) {
            case 45: case 90: case 135: return 1;
            case 225: case 270: case 315: return -1;
            default: return 0;
        }
    }
}
