package org.example;

public class Renderer implements IRenderer {
    private static final Object consoleLock = new Object();
    private static final int BAR_LENGTH = 50;
    private final ThreadInfo[] infos;
    private final long threadsCount;

    public Renderer(long threadsCount, ThreadInfo[] infos) {
        this.infos = infos;
        this.threadsCount = threadsCount;
    }

    public void initDisplay() {
        synchronized (consoleLock) {
            System.out.print("\033[H\033[3J");

            for (int i = 0; i < threadsCount; ++i) {
                System.out.println();
            }
            System.out.flush();
        }
    }

    @Override
    public void renderString(int num) {
        synchronized (consoleLock) {
            ThreadInfo info = infos[num - 1];

            System.out.printf("\033[%d;1H", info.serialNumber);
            System.out.print("\033[2K");

            String line = getString(info);

            if (info.completed) {
                long duration = info.endTime - info.startTime;
                line += String.format(" (%d ms)", duration);
            }

            System.out.println(line);
            System.out.flush();
        }
    }

    private static String getString(ThreadInfo info) {
        int filled = (info.progress * BAR_LENGTH) / info.CALCULATION_STEPS;
        StringBuilder bar = new StringBuilder("[");
        for (int i = 0; i < BAR_LENGTH; ++i) {
            bar.append(i < filled ? "█" : "░");
        }
        bar.append("]");

        return String.format("Thread %3d: [ThreadID: %3d] %s %3d%%",
                info.serialNumber,
                info.threadId,
                bar.toString(),
                (info.progress * 100) / info.CALCULATION_STEPS);
    }

    public void hideCursor() {
        System.out.print("\033[?25l");
    }

    public void showCursor() {
        System.out.print("\033[?25h");
    }
}
