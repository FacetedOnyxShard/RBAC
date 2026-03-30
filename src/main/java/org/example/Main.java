package org.example;

public class Main {
    private static final int THREAD_COUNT = 10;
    private static final int CALCULATION_STEPS = 20;
    private static final int DELAY_MS = 200;

    public static void main(String[] args) {
        ThreadInfo[] allThreadsInfo = new ThreadInfo[THREAD_COUNT];
        for (int i = 0; i < allThreadsInfo.length; ++i) {
            allThreadsInfo[i] = new ThreadInfo(i + 1, CALCULATION_STEPS, (i % 3 + 1) * 100);
        }

        Renderer renderer = new Renderer(THREAD_COUNT, allThreadsInfo);
        renderer.hideCursor();
        renderer.initDisplay();

        WorkerThread[] workers = new WorkerThread[THREAD_COUNT];
        for (int i = 0; i < workers.length; ++i) {
            workers[i] = new WorkerThread(allThreadsInfo[i], renderer);
            workers[i].start();
        }

        for (int i = 0; i < workers.length; ++i) {
            try {
                workers[i].join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        System.out.printf("\033[%d;1H", THREAD_COUNT + 1);
        renderer.showCursor();
    }
}