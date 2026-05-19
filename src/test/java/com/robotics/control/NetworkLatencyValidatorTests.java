package com.robotics.control;

import com.robotics.control.model.Position;
import com.robotics.control.model.VirtualEnvironment;
import com.robotics.control.sensor.LidarSensor;
import com.robotics.control.sensor.ProximitySensor;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NetworkLatencyValidatorTests {

    @Test
    void testValidatorClearsFalseObstaclesAlongRay() {
        VirtualEnvironment env = new VirtualEnvironment();
        LidarSensor lidar = new LidarSensor();
        ProximitySensor proximity = new ProximitySensor();

        // Simulate network latency distortion: robot was at idleOrigin (5,5).
        // Previous latency glitches erroneously placed false obstacles at (5,6) and (5,7).
        Position idleOrigin = new Position(5, 5);
        env.setObstacle(5, 6, true);
        env.setObstacle(5, 7, true);

        assertTrue(env.isObstacleAt(new Position(5, 6)));
        assertTrue(env.isObstacleAt(new Position(5, 7)));

        // Lidar detects the true obstacle at distance 3.0 North (theta = 90 degrees) -> (5, 8).
        double[] dummyReadings = new double[360];
        Arrays.fill(dummyReadings, 10.0); // Default maxRange
        dummyReadings[90] = 3.0;
        lidar.setRawDegreeReadings(dummyReadings);

        // Run validateAndUpdateObstacles
        List<Position> newObstacles = env.validateAndUpdateObstacles(idleOrigin, proximity, lidar);

        // Verify validator cleared the false obstacles in the free space between robot and true obstacle!
        assertFalse(env.isObstacleAt(new Position(5, 6)), "Validator should clear false obstacle at (5,6)");
        assertFalse(env.isObstacleAt(new Position(5, 7)), "Validator should clear false obstacle at (5,7)");

        // Verify validator successfully placed the true obstacle at (5, 8)!
        assertTrue(env.isObstacleAt(new Position(5, 8)), "Validator should place true obstacle at (5,8)");
        assertEquals(1, newObstacles.size());
        assertEquals(new Position(5, 8), newObstacles.get(0));
    }

    @Test
    void testValidatorHandlesMaxRangeClearing() {
        VirtualEnvironment env = new VirtualEnvironment();
        LidarSensor lidar = new LidarSensor();
        ProximitySensor proximity = new ProximitySensor();

        // Robot at idleOrigin (10,10). Old false obstacles at (11,10) and (12,10).
        Position idleOrigin = new Position(10, 10);
        env.setObstacle(11, 10, true);
        env.setObstacle(12, 10, true);

        assertTrue(env.isObstacleAt(new Position(11, 10)));
        assertTrue(env.isObstacleAt(new Position(12, 10)));

        // Lidar detects NO obstacle East (theta = 0 degrees) -> rawD = 10.0 (maxRange).
        double[] dummyReadings = new double[360];
        Arrays.fill(dummyReadings, 10.0);
        lidar.setRawDegreeReadings(dummyReadings);

        // Run validateAndUpdateObstacles
        List<Position> newObstacles = env.validateAndUpdateObstacles(idleOrigin, proximity, lidar);

        // Verify validator cleared all false obstacles along the clear path up to maxRange!
        assertFalse(env.isObstacleAt(new Position(11, 10)), "Validator should clear false obstacle at (11,10)");
        assertFalse(env.isObstacleAt(new Position(12, 10)), "Validator should clear false obstacle at (12,10)");
        assertTrue(newObstacles.isEmpty());
    }
}
