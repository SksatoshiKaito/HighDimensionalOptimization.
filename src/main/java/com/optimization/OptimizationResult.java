package com.optimization;

/**
 * Holds the full profiling result for a single optimization task.
 */
public class OptimizationResult {
    public final int taskId;
    public final Vector bestPoint;
    public final double bestValue;
    public final long executionTimeMs;
    public final long memoryAllocatedBytes;

    public OptimizationResult(int taskId, Vector bestPoint, double bestValue,
                               long executionTimeMs, long memoryAllocatedBytes) {
        this.taskId = taskId;
        this.bestPoint = bestPoint;
        this.bestValue = bestValue;
        this.executionTimeMs = executionTimeMs;
        this.memoryAllocatedBytes = memoryAllocatedBytes;
    }
}
