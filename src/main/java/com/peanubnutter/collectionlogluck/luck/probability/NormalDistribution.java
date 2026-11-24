package com.peanubnutter.collectionlogluck.luck.probability;

/**
 * Self-contained Normal (Gaussian) distribution implementation replacing Apache Commons Math's NormalDistribution.
 * Uses W. J. Cody's (1969) rational Chebyshev approximations for erf/erfc to achieve full double precision
 * without iterative loops.
 */
public class NormalDistribution {

    private static final double SQRT_2 = Math.sqrt(2.0);
    private static final double INV_SQRT_PI = 0.56418958354775628695;
    private static final double SQRT_2_PI = Math.sqrt(2.0 * Math.PI);

    // Must be initialized after SQRT_2 and SQRT_2_PI
    public static final NormalDistribution STANDARD = new NormalDistribution(0.0, 1.0);

    // W. J. Cody (1969), "Rational Chebyshev Approximations for the Error Function",
    // Mathematics of Computation, Vol. 23, No. 107, pp. 631-637.
    private static final double THRESH = 0.46875;

    // Interval 1: |x| <= 0.46875 (erf)
    private static final double[] A = {
            3.16112374387056560e00,
            1.13864154151050156e02,
            3.77485237685302021e02,
            3.20937758913846947e03,
            1.85777706184603153e-1
    };
    private static final double[] B = {
            2.36012909523441209e01,
            2.44024637934444173e02,
            1.28261652607737228e03,
            2.84423683343917062e03
    };

    // Interval 2: 0.46875 < |x| <= 4.0 (erfc)
    private static final double[] C = {
            5.64188496988670089e-1,
            8.88314979438837594e00,
            6.61191906371416295e01,
            2.98635138197400131e02,
            8.81952221241769090e02,
            1.71204761263407058e03,
            2.05107837782607147e03,
            1.23033935479799725e03,
            2.15311535474403846e-8
    };
    private static final double[] D = {
            1.57449261107098347e01,
            1.17693950891312499e02,
            5.37181101862009858e02,
            1.62138957456669019e03,
            3.29079923573345963e03,
            4.36261909014324716e03,
            3.43936767414372164e03,
            1.23033935480374942e03
    };

    // Interval 3: 4.0 < |x| < 27.3 (erfc)
    private static final double[] P = {
            1.63153871373020978e-2,
            3.05326634961232344e-1,
            3.60344899949804439e-1,
            1.25781726111229246e-1,
            1.60837851487422766e-2,
            6.58749161529837803e-4
    };
    private static final double[] Q = {
            2.56852019228982242e00,
            1.87295284992346047e00,
            5.27905102951428412e-1,
            6.05183413124413191e-2,
            2.33520497626869185e-3
    };

    private final double mean;
    private final double standardDeviation;
    private final double invStdDev;
    private final double invStdDevSqrt2;
    private final double pdfNormFactor;

    public NormalDistribution() {
        this(0.0, 1.0);
    }

    public NormalDistribution(double mean, double standardDeviation) {
        if (Double.isNaN(mean)) {
            throw new IllegalArgumentException("Mean must not be NaN");
        }
        if (standardDeviation <= 0.0 || Double.isNaN(standardDeviation)) {
            throw new IllegalArgumentException("Standard deviation must be positive: " + standardDeviation);
        }
        this.mean = mean;
        this.standardDeviation = standardDeviation;
        this.invStdDev = 1.0 / standardDeviation;
        this.invStdDevSqrt2 = 1.0 / (standardDeviation * SQRT_2);
        this.pdfNormFactor = 1.0 / (standardDeviation * SQRT_2_PI);
    }

    public double getMean() {
        return mean;
    }

    public double getStandardDeviation() {
        return standardDeviation;
    }

    /**
     * Probability density function (PDF) evaluated at x.
     */
    public double density(double x) {
        double z = (x - mean) * invStdDev;
        return pdfNormFactor * Math.exp(-0.5 * z * z);
    }

    /**
     * Cumulative distribution function (CDF) P(X <= x).
     */
    public double cumulativeProbability(double x) {
        if (Double.isNaN(x)) {
            return Double.NaN;
        }
        double dev = x - mean;
        if (Math.abs(dev) > 40.0 * standardDeviation) {
            return dev < 0.0 ? 0.0 : 1.0;
        }
        if (dev == 0.0) {
            return 0.5;
        }
        double z = dev * invStdDevSqrt2;
        if (Math.abs(z) <= THRESH) {
            return 0.5 + 0.5 * erfSmall(z);
        }
        double erfcAbsZ = erfcLarge(Math.abs(z));
        return z < 0.0 ? 0.5 * erfcAbsZ : 1.0 - 0.5 * erfcAbsZ;
    }

    /**
     * Computes erf(x) for |x| <= 0.46875 using Cody's rational Chebyshev approximation.
     */
    private static double erfSmall(double x) {
        double ysq = x * x;
        double xnum = A[4] * ysq;
        double xden = ysq;
        for (int i = 0; i < 3; i++) {
            xnum = (xnum + A[i]) * ysq;
            xden = (xden + B[i]) * ysq;
        }
        return x * (xnum + A[3]) / (xden + B[3]);
    }

    /**
     * Computes erfc(y) for y > 0.46875 using Cody's rational Chebyshev approximations.
     */
    private static double erfcLarge(double y) {
        if (y <= 4.0) {
            double xnum = C[8] * y;
            double xden = y;
            for (int i = 0; i < 7; i++) {
                xnum = (xnum + C[i]) * y;
                xden = (xden + D[i]) * y;
            }
            double res = (xnum + C[7]) / (xden + D[7]);
            double ysq = Math.floor(y * 16.0) / 16.0;
            double del = (y - ysq) * (y + ysq);
            return Math.exp(-ysq * ysq) * Math.exp(-del) * res;
        } else if (y < 27.3) {
            double w = 1.0 / (y * y);
            double xnum = P[0];
            double xden = 1.0;
            for (int i = 0; i < 4; i++) {
                xnum = xnum * w + P[i + 1];
                xden = xden * w + Q[i];
            }
            xnum = xnum * w + P[5];
            xden = xden * w + Q[4];
            double res = (INV_SQRT_PI - w * xnum / xden) / y;
            double ysq = Math.floor(y * 16.0) / 16.0;
            double del = (y - ysq) * (y + ysq);
            return Math.exp(-ysq * ysq) * Math.exp(-del) * res;
        } else {
            return 0.0;
        }
    }
}
