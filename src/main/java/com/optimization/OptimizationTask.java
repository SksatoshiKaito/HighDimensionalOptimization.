package com.optimization;

import java.util.concurrent.Callable;

/**
 * Step 5: Concurrency and Multi-Core Architecture
 * A task that runs the optimizer from a specific starting point,
 * and records profiling data (execution time + memory allocation).
 */
public class OptimizationTask implements Callable<OptimizationResult> {
    private final int taskId;
    private final Optimizer optimizer;
    private final Vector startPoint;
    private final ObjectiveFunction function;
    private final int maxIterations;
    private final double initialTemperature;
    private final double coolingRate;

    public OptimizationTask(int taskId, Optimizer optimizer, Vector startPoint, ObjectiveFunction function,
                            int maxIterations, double initialTemperature, double coolingRate) {
        this.taskId = taskId;
        this.optimizer = optimizer;
        this.startPoint = startPoint;
        this.function = function;
        this.maxIterations = maxIterations;
        this.initialTemperature = initialTemperature;
        this.coolingRate = coolingRate;
    }

    @Override
    public OptimizationResult call() {
        @SuppressWarnings("deprecation")
        long threadId = Thread.currentThread().getId();
        long memBefore = PerformanceProfiler.getThreadAllocatedBytes(threadId);
        long startTime = System.currentTimeMillis();

        System.out.println("  Thread " + taskId + " started at " + startPoint);

        Vector bestPoint = optimizer.optimize(startPoint, maxIterations, initialTemperature, coolingRate);
        double bestValue = function.evaluate(bestPoint);

        long endTime = System.currentTimeMillis();
        long memAfter = PerformanceProfiler.getThreadAllocatedBytes(threadId);

        long execTime = endTime - startTime;
        long memUsed = (memBefore >= 0 && memAfter >= 0) ? (memAfter - memBefore) : -1;

        System.out.println("  Thread " + taskId + " done | Time: " + execTime + "ms | Memory: "
                + PerformanceProfiler.formatMemory(memUsed)
                + " | Peak: " + String.format("%.4f", bestValue));

        return new OptimizationResult(taskId, bestPoint, bestValue, execTime, memUsed);
    }
}
