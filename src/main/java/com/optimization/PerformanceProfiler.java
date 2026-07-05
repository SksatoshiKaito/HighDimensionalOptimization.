package com.optimization;

import java.lang.management.ManagementFactory;
import com.sun.management.ThreadMXBean;

/**
 * Performance Profiler: Tracks execution time and memory allocation per thread.
 */
public class PerformanceProfiler {

    private static final ThreadMXBean threadMXBean;

    static {
        ThreadMXBean bean = (ThreadMXBean) ManagementFactory.getThreadMXBean();
        // Enable thread memory tracking if supported
        if (bean.isThreadAllocatedMemorySupported()) {
            bean.setThreadAllocatedMemoryEnabled(true);
        }
        threadMXBean = bean;
    }

    /**
     * Returns the memory allocated (in bytes) by a specific thread up to this point.
     */
    public static long getThreadAllocatedBytes(long threadId) {
        if (threadMXBean.isThreadAllocatedMemorySupported()) {
            return threadMXBean.getThreadAllocatedBytes(threadId);
        }
        return -1L;
    }

    /**
     * Converts bytes to human-readable MB string.
     */
    public static String formatMemory(long bytes) {
        if (bytes < 0) return "N/A";
        return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
    }

    /**
     * Calculates speedup ratio: single-thread time / multi-thread time.
     */
    public static double calculateSpeedup(long singleThreadTimeMs, long multiThreadTimeMs) {
        if (multiThreadTimeMs == 0) return 0;
        return (double) singleThreadTimeMs / multiThreadTimeMs;
    }
}
