package org.example;

public class ThreadInfo {
    public int serialNumber;
    public long threadId;
    public int progress;
    public boolean completed;
    public long startTime;
    public long endTime;
    public final int CALCULATION_STEPS;
    public final int DELAY_MS;

    public ThreadInfo(int serialNumber, int calculationSteps, int delayMs) {
        this.serialNumber = serialNumber;
        this.progress = 0;
        this.completed = false;
        this.CALCULATION_STEPS = calculationSteps;
        this.DELAY_MS = delayMs;
    }
}
