package com.robotics.control;

import com.robotics.control.model.Position;
import com.robotics.control.model.VirtualEnvironment;
import com.robotics.control.sensor.LidarSensor;
import com.robotics.control.sensor.ProximitySensor;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class SensorAndEnvironmentTests {

    @Test
    void testLidarSensorIndexingFix() {
        LidarSensor lidar = new LidarSensor();
        int[] dummyReadings = new int[360];
        Arrays.fill(dummyReadings, 10);
        // Set specific obstacle distances for cardinal directions
        dummyReadings[0] = 3;   // East (0 degrees)
        dummyReadings[90] = 4;  // North (90 degrees)
        dummyReadings[180] = 5; // West (180 degrees)
        dummyReadings[270] = 6; // South (270 degrees)
        lidar.setDegreeReadings(dummyReadings);

        // Verify correct direct mapping without off-by-one shifts
        assertEquals(3, lidar.getReadingAtDegree(0));
        assertEquals(4, lidar.getReadingAtDegree(90));
        assertEquals(5, lidar.getReadingAtDegree(180));
        assertEquals(6, lidar.getReadingAtDegree(270));

        // Test normal modulo wrapping logic for negative degrees and > 360 degrees
        assertEquals(3, lidar.getReadingAtDegree(360));
        assertEquals(3, lidar.getReadingAtDegree(-360));
        assertEquals(4, lidar.getReadingAtDegree(450));
    }

    @Test
    void testVirtualEnvironmentSpaceClearing() {
        VirtualEnvironment env = new VirtualEnvironment();
        LidarSensor lidar = new LidarSensor();
        ProximitySensor proximity = new ProximitySensor();

        // Place a pre-existing obstacle at the robot's target position and intermediate cells
        Position currentPos = new Position(5, 5);
        env.setObstacle(5, 5, true);
        env.setObstacle(6, 5, true);
        env.setObstacle(7, 5, true);
        env.setObstacle(8, 5, true);

        assertTrue(env.isObstacleAt(new Position(5, 5)));
        assertTrue(env.isObstacleAt(new Position(6, 5)));
        assertTrue(env.isObstacleAt(new Position(7, 5)));
        assertTrue(env.isObstacleAt(new Position(8, 5)));

        // Setup Lidar to detect an obstacle at 3 units to the East (degree 0)
        double[] dummyReadings = new double[360];
        Arrays.fill(dummyReadings, 10.0);
        dummyReadings[0] = 3.0; // Obstacle at East (0 degrees) at distance 3.0
        lidar.setRawDegreeReadings(dummyReadings);

        // Run updateObstacles
        env.updateObstacles(currentPos, proximity, lidar);

        // Robot's current position (5, 5) must be cleared!
        assertFalse(env.isObstacleAt(new Position(5, 5)));

        // Intermediate cells (6, 5) and (7, 5) must be cleared along the ray!
        assertFalse(env.isObstacleAt(new Position(6, 5)));
        assertFalse(env.isObstacleAt(new Position(7, 5)));

        // Target obstacle position (rx + k*dx, ry + k*dy) -> (5 + 3, 5 + 0) = (8, 5) must be marked as blocked!
        assertTrue(env.isObstacleAt(new Position(8, 5)));
    }
}
