package io.github.skix.stintcast.forecast.regression;

import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression;

import java.util.Arrays;

/**
 * Linear regression with the Huber loss, fitted by iteratively reweighted least squares.
 * Laps with large residuals get a lower weight, so they pull the fit less than in OLS.
 */
final class HuberRegression {

    private static final double TUNING = 1.345;
    private static final int MAX_ITERATIONS = 50;
    private static final double TOLERANCE = 1e-6;

    private HuberRegression() {}

    /**
     * Returns the coefficients, intercept first, like OLSMultipleLinearRegression.
     */
    static double[] fit(double[][] x, double[] y) {
        double[] weights = new double[y.length];
        Arrays.fill(weights, 1.0);
        double[] beta = weightedFit(x, y, weights);

        for (int iteration = 0; iteration < MAX_ITERATIONS; iteration++) {
            double[] residuals = residuals(x, y, beta);
            double threshold = TUNING * scale(residuals);
            if (threshold == 0) break;

            for (int i = 0; i < y.length; i++) {
                double r = Math.abs(residuals[i]);
                weights[i] = r <= threshold ? 1.0 : threshold / r;
            }

            double[] next = weightedFit(x, y, weights);
            double change = 0;
            for (int j = 0; j < beta.length; j++)
                change = Math.max(change, Math.abs(next[j] - beta[j]));
            beta = next;
            if (change < TOLERANCE) break;
        }
        return beta;
    }

    // Weighted least squares: each row is scaled by the square root of its weight
    private static double[] weightedFit(double[][] x, double[] y, double[] weights) {
        int n = y.length, p = x[0].length;
        double[][] wx = new double[n][p + 1];
        double[] wy = new double[n];
        for (int i = 0; i < n; i++) {
            double s = Math.sqrt(weights[i]);
            wx[i][0] = s; // intercept column, weighted too
            for (int j = 0; j < p; j++)
                wx[i][j + 1] = s * x[i][j];
            wy[i] = s * y[i];
        }
        OLSMultipleLinearRegression ols = new OLSMultipleLinearRegression();
        ols.setNoIntercept(true);
        ols.newSampleData(wy, wx);
        return ols.estimateRegressionParameters();
    }

    private static double[] residuals(double[][] x, double[] y, double[] beta) {
        double[] r = new double[y.length];
        for (int i = 0; i < y.length; i++) {
            double predicted = beta[0];
            for (int j = 0; j < x[i].length; j++)
                predicted += beta[j + 1] * x[i][j];
            r[i] = y[i] - predicted;
        }
        return r;
    }

    // Robust estimate of the residual standard deviation (median absolute deviation)
    private static double scale(double[] residuals) {
        double[] abs = Arrays.stream(residuals).map(Math::abs).sorted().toArray();
        int n = abs.length;
        double median = n % 2 == 1 ? abs[n / 2] : (abs[n / 2 - 1] + abs[n / 2]) / 2;
        return median / 0.6745;
    }
}