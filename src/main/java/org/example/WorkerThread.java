package org.example;

public class WorkerThread extends Thread {
    private final ThreadInfo info;
    private final IRenderer renderer;

    public WorkerThread(ThreadInfo info, IRenderer renderer) {
        this.info = info;
        this.renderer = renderer;
    }


    @Override
    public void run() {
        info.threadId = Thread.currentThread().threadId();
        info.startTime = System.currentTimeMillis();

        for (int i = 0; i < info.CALCULATION_STEPS; ++i) {
            info.progress = i + 1;

            renderer.renderString(info.serialNumber);

            try {
                Thread.sleep(info.DELAY_MS);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        info.completed = true;
        info.endTime = System.currentTimeMillis();
        renderer.renderString(info.serialNumber);
    }
}
