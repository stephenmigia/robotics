package com.robotics.control.sensor;

import tools.jackson.databind.ObjectMapper;

import java.util.Arrays;

public class LidarSensor implements Sensor {
    private int[] degreeReadings = new int[360];
    private double[] rawDegreeReadings = new double[360]; // Raw readings for accurate obstacle placement
    private final int maxRange = 10;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public LidarSensor() {
        Arrays.fill(degreeReadings, 10);
        Arrays.fill(rawDegreeReadings, 10.0);
    }

    public int[] getDegreeReadings() {
        return degreeReadings;
    }

    public void setDegreeReadings(int[] degreeReadings) {
        this.degreeReadings = degreeReadings;
    }

    /**
     * Store raw double readings for accurate obstacle placement.
     * The int[] degreeReadings (ceiling'd) are used for display only.
     */
    public void setRawDegreeReadings(double[] rawReadings) {
        this.rawDegreeReadings = rawReadings;
        // Also update the int (ceiling) array for display
        this.degreeReadings = Arrays.stream(rawReadings).mapToInt(d -> {
            if (d < 0) d = 0;
            return (int) Math.ceil(d);
        }).toArray();
    }

    public double[] getRawDegreeReadings() {
        return rawDegreeReadings;
    }

    /**
     * Get the raw (double) reading at a specific degree for obstacle placement.
     * Returns the un-ceiled value so grid placement uses floor-based math.
     */
    public double getRawReadingAtDegree(int degree) {
        try {
            int idx = (degree % 360 + 360) % 360;
            if (rawDegreeReadings != null && idx >= 0 && idx < rawDegreeReadings.length) {
                return rawDegreeReadings[idx];
            }
        } catch (Exception e) {
            // Fallback
        }
        return maxRange;
    }

    public int getReadingAtDegree(int degree) {
        try {
            int idx = (degree % 360 + 360) % 360;
            if (degreeReadings != null && idx >= 0 && idx < degreeReadings.length) {
                return degreeReadings[idx];
            }
        } catch (Exception e) {
            // Gracefully fall back to max range if index or initialization issues occur
        }
        return maxRange;
    }

    public int getMaxRange() {
        return maxRange;
    }

    @Override
    public void updateReadings(String rawJsonData) {
        try {
            double[] data = objectMapper.readValue(rawJsonData, double[].class);
            // Store raw readings
            if (data.length == 360) {
                this.rawDegreeReadings = Arrays.copyOf(data, 360);
            } else {
                this.rawDegreeReadings = new double[360];
                Arrays.fill(this.rawDegreeReadings, maxRange);
                for (int i = 0; i < Math.min(360, data.length); i++) {
                    this.rawDegreeReadings[i] = data[i];
                }
            }
            // Derive ceiling'd int array for display
            this.degreeReadings = Arrays.stream(this.rawDegreeReadings).mapToInt(d -> {
                if (d < 0) d = 0;
                return (int) Math.ceil(d);
            }).toArray();
        } catch (Exception e) {
            try {
                double[] doubleData = objectMapper.readValue(rawJsonData, double[].class);
                this.rawDegreeReadings = new double[360];
                Arrays.fill(this.rawDegreeReadings, maxRange);
                for (int i = 0; i < Math.min(360, doubleData.length); i++) {
                    this.rawDegreeReadings[i] = doubleData[i];
                    this.degreeReadings[i] = (int) Math.ceil(doubleData[i]);
                }
            } catch (Exception ex) {
                // Fallback fails
            }
        }
    }
}
