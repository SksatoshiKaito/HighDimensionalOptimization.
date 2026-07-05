package com.optimization;

import java.util.Arrays;
import java.util.Random;

/**
 * ╔══════════════════════════════════════════════════════════════════╗
 * ║   CMA-ES: Covariance Matrix Adaptation Evolution Strategy       ║
 * ║                                                                  ║
 * ║   The mathematically proven gold standard for continuous        ║
 * ║   black-box optimization. Winner of CEC/BBOB competitions.      ║
 * ║                                                                  ║
 * ║   Key ideas:                                                     ║
 * ║   1. Maintain a multivariate Gaussian N(m, σ²C)                 ║
 * ║   2. Learn the full covariance matrix C from history             ║
 * ║   3. Adapt step size σ with Cumulative Step-size Adaptation     ║
 * ║   4. Adapt C with rank-1 + rank-μ updates (CMA)                ║
 * ║   5. Restart with increasing population (IPOP) on stagnation    ║
 * ╚══════════════════════════════════════════════════════════════════╝
 *
 * Reference: Hansen, N. (2016). "The CMA Evolution Strategy: A Tutorial"
 * arXiv:1604.00772 — the de facto textbook for modern optimization.
 */
public class CMAESOptimizer {

    private final ObjectiveFunction function;
    private final int dim;
    private final Random rng;

    // CMA-ES state variables
    private double[] mean;          // Distribution mean (current best estimate)
    private double sigma;           // Global step size
    private double[] pc;            // Evolution path for covariance matrix C
    private double[] ps;            // Evolution path for step size (CSA)
    private double[][] C;           // Covariance matrix
    private double[][] B;           // Eigenvectors of C
    private double[] D;             // Square roots of eigenvalues of C

    // Strategy parameters (computed from lambda)
    private final int lambda;       // Offspring count per generation
    private final int mu;           // Parents (best half selected)
    private final double[] weights; // Recombination weights
    private final double mueff;     // Effective number of parents
    private final double cc;        // Learning rate for cumulative path
    private final double cs;        // Learning rate for step size path
    private final double c1;        // Learning rate for rank-1 update
    private final double cmu;       // Learning rate for rank-μ update
    private final double damps;     // Step size damping
    private final double chiN;      // Expected ||N(0,I)||

    // IPOP (Increasing POPulation) restart tracking
    private int restartLambda;
    private int numRestarts;
    private static final int MAX_RESTARTS = 3;

    // Eigendecomposition update frequency
    private int eigenUpdateFreq;
    private int lastEigenUpdate;

    public CMAESOptimizer(ObjectiveFunction function, long seed) {
        this.function = function;
        this.dim = function.getDimension();
        this.rng = new Random(seed);
        this.restartLambda = 4 + (int)(3 * Math.log(dim));
        this.lambda = restartLambda;
        this.mu = lambda / 2;

        // Recombination weights (log-sum normalized)
        weights = new double[mu];
        double sumW = 0, sumW2 = 0;
        for (int i = 0; i < mu; i++) {
            weights[i] = Math.log((lambda + 1.0) / 2.0) - Math.log(i + 1.0);
            sumW += weights[i];
        }
        for (int i = 0; i < mu; i++) { weights[i] /= sumW; sumW2 += weights[i] * weights[i]; }
        mueff = 1.0 / sumW2;

        // Adaptation parameters (from Hansen 2016 tutorial)
        cc     = (4.0 + mueff / dim) / (dim + 4.0 + 2.0 * mueff / dim);
        cs     = (mueff + 2.0) / (dim + mueff + 5.0);
        c1     = 2.0 / ((dim + 1.3) * (dim + 1.3) + mueff);
        cmu    = Math.min(1 - c1, 2.0 * (mueff - 2.0 + 1.0 / mueff) / ((dim + 2.0) * (dim + 2.0) + mueff));
        damps  = 1.0 + 2.0 * Math.max(0, Math.sqrt((mueff - 1.0) / (dim + 1.0)) - 1.0) + cs;
        chiN   = Math.sqrt(dim) * (1.0 - 1.0 / (4.0 * dim) + 1.0 / (21.0 * dim * dim));
        eigenUpdateFreq = (int) Math.max(1, 1.0 / ((c1 + cmu) * dim * 10.0));
    }

    /**
     * Main optimization loop.
     * @param maxEvaluations Total number of function evaluations budget.
     * @param searchRadius   Initial search radius (σ₀ = searchRadius/3).
     * @param tracker        Optional convergence tracker.
     * @return Best solution vector found.
     */
    public Vector optimize(int maxEvaluations, double searchRadius, ConvergenceTracker tracker) {
        Vector globalBest = null;
        double globalBestValue = Double.NEGATIVE_INFINITY;
        numRestarts = 0;
        int totalEvals = 0;
        int currentLambda = restartLambda;

        while (totalEvals < maxEvaluations && numRestarts <= MAX_RESTARTS) {
            // Initialize / Restart state
            mean = randomPoint(searchRadius);
            sigma = searchRadius / 3.0;
            pc = new double[dim];
            ps = new double[dim];
            C = identityMatrix(dim);
            B = identityMatrix(dim);
            D = onesArray(dim);
            lastEigenUpdate = 0;

            int gen = 0;
            int stagnationCount = 0;
            double prevBestValue = Double.NEGATIVE_INFINITY;
            int currentLambdaLocal = currentLambda;

            while (totalEvals < maxEvaluations) {
                // ── STEP 1: Sample λ offspring from N(m, σ²C) ─────────────────
                double[][] offspring = new double[currentLambdaLocal][dim];
                double[] fitnessVals = new double[currentLambdaLocal];

                for (int k = 0; k < currentLambdaLocal; k++) {
                    offspring[k] = sampleFromDistribution();
                    Vector v = arrayToVector(offspring[k]);
                    fitnessVals[k] = function.evaluate(v);
                    totalEvals++;

                    if (fitnessVals[k] > globalBestValue) {
                        globalBestValue = fitnessVals[k];
                        globalBest = v.copy();
                    }
                }

                // ── STEP 2: Sort by fitness (descending = maximization) ────────
                Integer[] sortIdx = sortIndices(fitnessVals);

                // ── STEP 3: Update mean (weighted centroid of best μ) ──────────
                double[] oldMean = mean.clone();
                mean = new double[dim];
                for (int i = 0; i < mu; i++) {
                    for (int d = 0; d < dim; d++) {
                        mean[d] += weights[i] * offspring[sortIdx[i]][d];
                    }
                }

                // ── STEP 4: Cumulative Step-size Adaptation (CSA) ─────────────
                double[] meanDiff = vectorDiff(mean, oldMean);
                double[] BInvDInv_y = backSubstitute(B, D, meanDiff, sigma);

                // Update ps = (1-cs)*ps + sqrt(cs*(2-cs)*mueff) * B*D^{-1}*(m-m_old)/σ
                double csWeight = Math.sqrt(cs * (2.0 - cs) * mueff);
                for (int d = 0; d < dim; d++) {
                    ps[d] = (1 - cs) * ps[d] + csWeight * BInvDInv_y[d];
                }

                // ── STEP 5: Covariance Matrix Adaptation (CMA) ────────────────
                // hsig: stall indicator (suppress rank-1 update if step size is too large)
                double psNorm = norm(ps);
                double threshold = (1.4 + 2.0 / (dim + 1.0)) * chiN;
                int hsig = (psNorm / Math.sqrt(1 - Math.pow(1 - cs, 2 * (gen + 1))) < threshold) ? 1 : 0;

                // Update pc = (1-cc)*pc + hsig * sqrt(cc*(2-cc)*mueff) * (m-m_old)/σ
                double ccWeight = Math.sqrt(cc * (2.0 - cc) * mueff);
                for (int d = 0; d < dim; d++) {
                    pc[d] = (1 - cc) * pc[d] + hsig * ccWeight * (mean[d] - oldMean[d]) / sigma;
                }

                // C = (1-c1-cmu)*C + c1*(pc*pc^T + (1-hsig)*cc*(2-cc)*C) + cmu * Σ w_i * y_i*y_i^T
                double[][] pcOuter = outerProduct(pc, pc);
                for (int i2 = 0; i2 < dim; i2++) {
                    for (int j = 0; j < dim; j++) {
                        double rankMuSum = 0;
                        for (int i = 0; i < mu; i++) {
                            double yi = (offspring[sortIdx[i]][i2] - oldMean[i2]) / sigma;
                            double yj = (offspring[sortIdx[i]][j] - oldMean[j]) / sigma;
                            rankMuSum += weights[i] * yi * yj;
                        }
                        C[i2][j] = (1 - c1 - cmu) * C[i2][j]
                                 + c1 * (pcOuter[i2][j] + (1 - hsig) * cc * (2 - cc) * C[i2][j])
                                 + cmu * rankMuSum;
                    }
                }

                // ── STEP 6: Adapt step size σ ──────────────────────────────────
                sigma = sigma * Math.exp((cs / damps) * (psNorm / chiN - 1.0));
                sigma = Math.min(sigma, searchRadius * 2); // Cap for stability

                // ── STEP 7: Eigendecomposition of C (periodically) ────────────
                if (gen - lastEigenUpdate >= eigenUpdateFreq) {
                    performEigenDecomposition();
                    lastEigenUpdate = gen;
                }

                gen++;

                // Track convergence
                if (tracker != null) tracker.record(totalEvals, globalBestValue);

                // Stagnation detection
                double bestThisGen = fitnessVals[sortIdx[0]];
                if (Math.abs(bestThisGen - prevBestValue) < 1e-10) stagnationCount++;
                else stagnationCount = 0;
                prevBestValue = bestThisGen;

                // Stopping conditions
                if (sigma < 1e-10 || stagnationCount > 100 ||
                    (gen > 100 + 50 * (int)Math.log(dim) && totalEvals < maxEvaluations * 0.8)) {
                    break;
                }
            }

            // IPOP restart: double the population
            numRestarts++;
            currentLambda = currentLambdaLocal * 2;
        }

        return globalBest != null ? globalBest : arrayToVector(mean);
    }

    // ── Mathematical internals ────────────────────────────────────────────────

    /** Sample x ~ N(m, σ²C) = m + σ * B * D * z,  z ~ N(0,I) */
    private double[] sampleFromDistribution() {
        double[] z = new double[dim];
        for (int d = 0; d < dim; d++) z[d] = rng.nextGaussian();

        // y = B * diag(D) * z
        double[] y = new double[dim];
        for (int i = 0; i < dim; i++) {
            double sum = 0;
            for (int j = 0; j < dim; j++) sum += B[i][j] * D[j] * z[j];
            y[i] = sum;
        }

        double[] x = new double[dim];
        for (int d = 0; d < dim; d++) x[d] = mean[d] + sigma * y[d];
        return x;
    }

    /**
     * Symmetric Eigendecomposition of C using Jacobi iterations.
     * C = B * diag(D²) * B^T  →  B holds eigenvectors, D holds sqrt(eigenvalues)
     */
    private void performEigenDecomposition() {
        // Enforce symmetry
        for (int i = 0; i < dim; i++)
            for (int j = 0; j < i; j++) { C[i][j] = C[j][i] = (C[i][j] + C[j][i]) / 2.0; }

        // Copy C into working matrix
        double[][] A = copyMatrix(C);
        double[][] V = identityMatrix(dim);

        // Jacobi iterations (converges for symmetric matrices)
        int maxIter = 100 * dim * dim;
        for (int iter = 0; iter < maxIter; iter++) {
            // Find largest off-diagonal element
            int p = 0, q = 1;
            double maxVal = Math.abs(A[0][1]);
            for (int i = 0; i < dim - 1; i++) {
                for (int j = i + 1; j < dim; j++) {
                    if (Math.abs(A[i][j]) > maxVal) { maxVal = Math.abs(A[i][j]); p = i; q = j; }
                }
            }
            if (maxVal < 1e-14) break;

            // Compute Jacobi rotation
            double theta = (A[q][q] - A[p][p]) / (2.0 * A[p][q]);
            double t = (theta >= 0 ? 1.0 : -1.0) / (Math.abs(theta) + Math.sqrt(1 + theta * theta));
            double c = 1.0 / Math.sqrt(1 + t * t);
            double s = t * c;

            // Apply rotation to A
            double app = A[p][p], aqq = A[q][q], apq = A[p][q];
            A[p][p] = app - t * apq;
            A[q][q] = aqq + t * apq;
            A[p][q] = A[q][p] = 0;
            for (int r = 0; r < dim; r++) {
                if (r != p && r != q) {
                    double arp = A[r][p], arq = A[r][q];
                    A[r][p] = A[p][r] = c * arp - s * arq;
                    A[r][q] = A[q][r] = s * arp + c * arq;
                }
            }
            // Apply rotation to eigenvector matrix V
            for (int r = 0; r < dim; r++) {
                double vrp = V[r][p], vrq = V[r][q];
                V[r][p] = c * vrp - s * vrq;
                V[r][q] = s * vrp + c * vrq;
            }
        }

        // Extract eigenvalues (diagonal of A) and eigenvectors (columns of V)
        B = new double[dim][dim];
        D = new double[dim];
        for (int i = 0; i < dim; i++) {
            D[i] = Math.sqrt(Math.max(1e-20, A[i][i])); // sqrt of eigenvalue, clamp > 0
            for (int j = 0; j < dim; j++) B[j][i] = V[j][i];
        }
    }

    /** B^{-1} * D^{-1} * y  (B is orthogonal, so B^{-1} = B^T) */
    private double[] backSubstitute(double[][] Bmat, double[] Dvec, double[] y, double sigmaVal) {
        double[] result = new double[dim];
        for (int i = 0; i < dim; i++) {
            double sum = 0;
            for (int j = 0; j < dim; j++) sum += Bmat[j][i] * y[j]; // B^T * y
            result[i] = sum / (Dvec[i] * sigmaVal);
        }
        return result;
    }

    // ── Utility methods ───────────────────────────────────────────────────────
    private double[] randomPoint(double r) {
        double[] p = new double[dim];
        for (int d = 0; d < dim; d++) p[d] = (rng.nextDouble() * 2 - 1) * r;
        return p;
    }
    private double norm(double[] v) {
        double s = 0; for (double x : v) s += x * x; return Math.sqrt(s);
    }
    private double[] vectorDiff(double[] a, double[] b) {
        double[] r = new double[dim]; for (int i = 0; i < dim; i++) r[i] = a[i] - b[i]; return r;
    }
    private double[][] identityMatrix(int n) {
        double[][] m = new double[n][n]; for (int i = 0; i < n; i++) m[i][i] = 1.0; return m;
    }
    private double[] onesArray(int n) { double[] a = new double[n]; Arrays.fill(a, 1.0); return a; }
    private double[][] outerProduct(double[] a, double[] b) {
        double[][] m = new double[dim][dim];
        for (int i = 0; i < dim; i++) for (int j = 0; j < dim; j++) m[i][j] = a[i] * b[j];
        return m;
    }
    private double[][] copyMatrix(double[][] m) {
        double[][] c = new double[dim][dim];
        for (int i = 0; i < dim; i++) c[i] = m[i].clone();
        return c;
    }
    private Vector arrayToVector(double[] a) {
        Vector v = new Vector(dim); for (int i = 0; i < dim; i++) v.set(i, a[i]); return v;
    }
    private Integer[] sortIndices(double[] arr) {
        Integer[] idx = new Integer[arr.length];
        for (int i = 0; i < arr.length; i++) idx[i] = i;
        Arrays.sort(idx, (a, b) -> Double.compare(arr[b], arr[a])); // descending
        return idx;
    }
}
