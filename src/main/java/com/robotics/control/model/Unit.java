package com.robotics.control.model;

public class Unit {
    private String robotId = "CMP9134-VRobot";
    private Position currentPosition = new Position(0, 0);
    private double currentBattery = 100.0;
    private Status currentStatus = Status.IDLE;

    private final double batteryCapacity = 100.0;
    private final double drainRatePerStep = 0.5;
    private final double chargeRatePerSecond = 2.0;
    private final double lowBatteryThreshold = 20.0;

    public Unit() {}

    public String getRobotId() { return robotId; }
    public void setRobotId(String robotId) { this.robotId = robotId; }

    public Position getCurrentPosition() { return currentPosition; }
    public void setCurrentPosition(Position currentPosition) { this.currentPosition = currentPosition; }

    public double getCurrentBattery() { return currentBattery; }
    public void setCurrentBattery(double currentBattery) { 
        this.currentBattery = Math.max(0.0, Math.min(batteryCapacity, currentBattery)); 
        updateStatusBasedOnBattery();
    }

    public Status getCurrentStatus() { return currentStatus; }
    public void setCurrentStatus(Status currentStatus) { this.currentStatus = currentStatus; }

    public double getBatteryCapacity() { return batteryCapacity; }
    public double getDrainRatePerStep() { return drainRatePerStep; }
    public double getChargeRatePerSecond() { return chargeRatePerSecond; }
    public double getLowBatteryThreshold() { return lowBatteryThreshold; }

    public void updateState(Position pos, double battery, Status status) {
        this.currentPosition = pos;
        this.currentBattery = Math.max(0.0, Math.min(batteryCapacity, battery));
        this.currentStatus = status;
        updateStatusBasedOnBattery();
    }

    private void updateStatusBasedOnBattery() {
        if (this.currentBattery < lowBatteryThreshold && this.currentStatus == Status.IDLE) {
            this.currentStatus = Status.LOW_BATTERY;
        }
    }
}
