package com.optimization;

import java.util.ArrayList;
import java.util.List;

/**
 * Tracks the convergence history (best value per iteration snapshot).
 * Used for plotting and analysis.
 */
public class ConvergenceTracker {
    private final List<Double> history = new ArrayList<>();
    private final int snapshotInterval;

    public ConvergenceTracker(int snapshotInterval) {
        this.snapshotInterval = snapshotInterval;
    }

    public void record(int iteration, double bestValue) {
        if (iteration % snapshotInterval == 0) {
            history.add(bestValue);
        }
    }

    public List<Double> getHistory() { return history; }

    public double getBestEver() {
        return history.stream().mapToDouble(Double::doubleValue).max().orElse(Double.NEGATIVE_INFINITY);
    }

    /**
     * Convergence speed: how many snapshots until 90% of the best value is achieved.
     */
    public int getConvergenceSnapshot() {
        if (history.isEmpty()) return -1;
        double best = getBestEver();
        double target = best * 0.9;
        for (int i = 0; i < history.size(); i++) {
            if (history.get(i) >= target) return i;
        }
        return history.size();
    }
}
