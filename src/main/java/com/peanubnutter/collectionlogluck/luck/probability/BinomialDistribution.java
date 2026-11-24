package com.peanubnutter.collectionlogluck.luck.probability;

/**
 * Self-contained Binomial distribution implementation replacing Apache Commons Math's BinomialDistribution.
 *
 * <ul>
 *   <li>PMF uses Catherine Loader's (2000) saddlepoint expansion (Stirling error + deviance) to avoid
 *       catastrophic cancellation even for very large trial counts (n >= 10^8).</li>
 *   <li>CDF uses the regularized incomplete beta function I_{1-p}(n - k, k + 1) = 1 - I_p(k + 1, n - k),
 *       evaluated via symmetry and modified Lentz's continued fraction with the prefactor computed
 *       through Loader's saddlepoint log-PMF (avoiding log-gamma cancellation at large n).</li>
 * </ul>
 */
public class BinomialDistribution {

    private static final double HALF_LOG_2_PI = 0.5 * Math.log(2.0 * Math.PI);

    // Exact Stirling error delta(k) = ln(k!) - (k*ln(k) - k + 0.5*ln(2*pi*k)) for k = 0..15.
    // (delta(0) is unused; included for 1-based indexing).
    private static final double[] STIRLING_ERROR = {
            0.0,
            0.081061466795327258219670264,
            0.041340695955409294093822081,
            0.0276779256849983391487892927,
            0.020790672103765093111522771,
            0.0166446911898211921631948653,
            0.013876128823070747998745727,
            0.0118967099458917700950557241,
            0.010411265261972096497478567,
            0.0092554621827127329177286366,
            0.008330563433362871256469318,
            0.0075736754879518407949720242,
            0.006942840107209529865664152,
            0.0064089941880042070684396310,
            0.005951370112758847735624416,
            0.0055547335519628013710386899
    };

    private static final double S0 = 1.0 / 12.0;
    private static final double S1 = 1.0 / 360.0;
    private static final double S2 = 1.0 / 1260.0;
    private static final double S3 = 1.0 / 1680.0;
    private static final double S4 = 1.0 / 1188.0;

    private static final double CF_EPSILON = 1e-14;
    private static final double CF_TINY = 1e-300;
    private static final int CF_MAX_ITERATIONS = 100_000;

    private final int numberOfTrials;
    private final double probabilityOfSuccess;

    public BinomialDistribution(int trials, double p) {
        if (trials < 0) {
            throw new IllegalArgumentException("Number of trials must be non-negative: " + trials);
        }
        if (p < 0.0 || p > 1.0 || Double.isNaN(p)) {
            throw new IllegalArgumentException("Probability must be in [0, 1]: " + p);
        }
        this.numberOfTrials = trials;
        this.probabilityOfSuccess = p;
    }

    public int getNumberOfTrials() {
        return numberOfTrials;
    }

    public double getProbabilityOfSuccess() {
        return probabilityOfSuccess;
    }

    /**
     * Probability mass function (PMF) P(X = x).
     */
    public double probability(int x) {
        if (x < 0 || x > numberOfTrials) {
            return 0.0;
        }
        if (numberOfTrials == 0) {
            return 1.0;
        }
        if (probabilityOfSuccess == 0.0) {
            return x == 0 ? 1.0 : 0.0;
        }
        if (probabilityOfSuccess == 1.0) {
            return x == numberOfTrials ? 1.0 : 0.0;
        }
        return Math.exp(logProbability(numberOfTrials, probabilityOfSuccess, x));
    }

    /**
     * Cumulative distribution function (CDF) P(X <= x).
     */
    public double cumulativeProbability(int x) {
        if (x < 0) {
            return 0.0;
        }
        if (x >= numberOfTrials) {
            return 1.0;
        }
        if (probabilityOfSuccess == 0.0) {
            return 1.0;
        }
        if (probabilityOfSuccess == 1.0) {
            return 0.0;
        }
        if (x == 0) {
            return Math.exp(numberOfTrials * Math.log1p(-probabilityOfSuccess));
        }
        if (x == numberOfTrials - 1) {
            return -Math.expm1(numberOfTrials * Math.log(probabilityOfSuccess));
        }

        // P(X <= x) = I_{1 - p}(n - x, x + 1) = 1 - I_p(x + 1, n - x).
        // Use the symmetry branch where the continued fraction converges fastest:
        // For I_z(a, b), evaluate directly when z <= (a + 1) / (a + b + 2).
        // For a = x + 1, b = n - x, z = p: z <= (x + 2) / (n + 3) <=> (x + 2) >= (n + 3) * p.
        double p = probabilityOfSuccess;
        double q = 1.0 - p;
        int n = numberOfTrials;
        if ((x + 2.0) > (n + 3.0) * p) {
            // Right tail: P(X > x) = I_p(x + 1, n - x)
            return 1.0 - regularizedBetaInteger(p, q, n, x + 1, n - x, true);
        } else {
            // Left tail: P(X <= x) = I_{1 - p}(n - x, x + 1)
            return regularizedBetaInteger(q, p, n, n - x, x + 1, false);
        }
    }

    /**
     * Evaluates the regularized incomplete beta function I_z(a, b) for positive integers a, b
     * with n = a + b - 1, assuming z <= (a + 1) / (a + b + 2).
     *
     * Uses the exact identity for integer a, b with n = a + b - 1:
     *   z^a * (1 - z)^b / (a * B(a, b)) = (1 - z) * BinomialPMF(n, z, a)
     * so the log prefactor is computed via Loader's saddlepoint log-PMF without cancellation.
     */
    private static double regularizedBetaInteger(double z, double oneMinusZ, int n, int a, int b, boolean zIsSmallP) {
        double logOneMinusZ = zIsSmallP ? Math.log1p(-z) : Math.log(oneMinusZ);
        double logFront = logProbability(n, z, oneMinusZ, a, zIsSmallP) + logOneMinusZ;
        double cf = betaContinuedFraction(z, a, b);

        // Guard against premature underflow when logFront < -700 while cf > 1
        double value;
        if (logFront < -700.0) {
            value = Math.exp(logFront + Math.log(cf));
        } else {
            value = Math.exp(logFront) * cf;
        }
        if (value < 0.0) {
            return 0.0;
        }
        if (value > 1.0) {
            return 1.0;
        }
        return value;
    }

    /**
     * Evaluates the continued fraction 1 / (1 + d_1 / (1 + d_2 / (1 + ...)))
     * for the regularized incomplete beta function I_z(a, b) using modified Lentz's method.
     */
    private static double betaContinuedFraction(double z, double a, double b) {
        double qab = a + b;
        double qap = a + 1.0;
        double qam = a - 1.0;

        double c = 1.0;
        double d = 1.0 - qab * z / qap;
        if (Math.abs(d) < CF_TINY) {
            d = CF_TINY;
        }
        d = 1.0 / d;
        double h = d;

        for (int m = 1; m <= CF_MAX_ITERATIONS; m++) {
            double m2 = 2.0 * m;

            // Even step: d_{2m} = m * (b - m) * z / ((a + 2m - 1) * (a + 2m))
            double aa = (m * (b - m) * z) / ((qam + m2) * (a + m2));
            if (aa == 0.0) {
                break;
            }
            d = 1.0 + aa * d;
            if (Math.abs(d) < CF_TINY) {
                d = CF_TINY;
            }
            c = 1.0 + aa / c;
            if (Math.abs(c) < CF_TINY) {
                c = CF_TINY;
            }
            d = 1.0 / d;
            h *= d * c;

            // Odd step: d_{2m+1} = -(a + m) * (a + b + m) * z / ((a + 2m) * (a + 2m + 1))
            aa = -((a + m) * (qab + m) * z) / ((a + m2) * (qap + m2));
            d = 1.0 + aa * d;
            if (Math.abs(d) < CF_TINY) {
                d = CF_TINY;
            }
            c = 1.0 + aa / c;
            if (Math.abs(c) < CF_TINY) {
                c = CF_TINY;
            }
            d = 1.0 / d;
            double del = d * c;
            h *= del;

            if (Math.abs(del - 1.0) <= CF_EPSILON) {
                break;
            }
        }
        return h;
    }

    /**
     * Computes ln(P(X = x)) for X ~ Binomial(n, p) with 0 < p < 1 and 0 <= x <= n
     * using Catherine Loader's (2000) saddlepoint expansion.
     */
    private static double logProbability(int n, double p, int x) {
        return logProbability(n, p, 1.0 - p, x, true);
    }

    private static double logProbability(int n, double p, double q, int x, boolean useLog1pForQ) {
        if (x == 0) {
            return n * (useLog1pForQ ? Math.log1p(-p) : Math.log(q));
        }
        if (x == n) {
            return n * (useLog1pForQ ? Math.log(p) : Math.log1p(-q));
        }
        double dn = n;
        double dx = x;
        double dnx = n - x;

        double lc = stirlingError(n) - stirlingError(x) - stirlingError(n - x)
                - deviance(dx, dn * p) - deviance(dnx, dn * q);
        double halfLogFactor = 0.5 * (Math.log(dn) - Math.log(dx) - Math.log(dnx)) - HALF_LOG_2_PI;
        return lc + halfLogFactor;
    }

    /**
     * Computes Stirling's approximation error:
     *   delta(n) = ln(n!) - (n*ln(n) - n + 0.5*ln(2*pi*n))
     */
    private static double stirlingError(int n) {
        if (n < STIRLING_ERROR.length) {
            return STIRLING_ERROR[n];
        }
        double dn = n;
        double invN = 1.0 / dn;
        double invN2 = invN * invN;
        return invN * (S0 - invN2 * (S1 - invN2 * (S2 - invN2 * (S3 - invN2 * S4))));
    }

    /**
     * Computes the binomial deviance term:
     *   bd0(x, np) = x * ln(x / np) + np - x
     * Uses a series expansion when x is close to np to prevent catastrophic cancellation.
     */
    private static double deviance(double x, double np) {
        if (Math.abs(x - np) < 0.1 * (x + np)) {
            double v = (x - np) / (x + np);
            double s = (x - np) * v;
            double ej = 2.0 * x * v;
            double v2 = v * v;
            for (int j = 1; j < 1000; j++) {
                ej *= v2;
                double s1 = s + ej / (2 * j + 1);
                if (s1 == s) {
                    return s1;
                }
                s = s1;
            }
            return s;
        }
        return x * Math.log(x / np) + np - x;
    }
}
