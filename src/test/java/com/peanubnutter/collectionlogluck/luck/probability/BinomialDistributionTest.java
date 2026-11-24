package com.peanubnutter.collectionlogluck.luck.probability;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BinomialDistributionTest {

    @Test
    public void exactReferenceValues() {
        // Exact reference values verified with arbitrary-precision combinatorics
        Object[][] cases = {
                // { n, p, k, expectedPmf, expectedCdf }
                {10, 0.5, 0, 9.76562500000000000e-04, 9.76562500000000000e-04},
                {10, 0.5, 5, 2.46093750000000000e-01, 6.23046875000000000e-01},
                {10, 0.5, 9, 9.76562500000000000e-03, 9.99023437500000000e-01},
                {10, 0.5, 10, 9.76562500000000000e-04, 1.0},
                {100, 0.01, 0, 3.66032341273229478e-01, 3.66032341273229478e-01},
                {100, 0.01, 1, 3.69729637649726772e-01, 7.35761978922956250e-01},
                {100, 0.01, 5, 2.89778712376148172e-03, 9.99465465536007014e-01},
                {1000, 1.0 / 512.0, 0, 1.41559545267758075e-01, 1.41559545267758075e-01},
                {1000, 1.0 / 512.0, 1, 2.77024550426141047e-01, 4.18584095693899150e-01},
                {1000, 1.0 / 512.0, 2, 2.70790142735533201e-01, 6.89374238429432351e-01},
                {1000, 1.0 / 512.0, 5, 3.35199944776785747e-02, 9.85169154753891663e-01},
                {5000, 1.0 / 3000.0, 0, 1.88823133019857897e-01, 1.88823133019857897e-01},
                {5000, 1.0 / 3000.0, 1, 3.14810158419236208e-01, 5.03633291439094077e-01},
                {5000, 1.0 / 3000.0, 3, 1.45755162019011980e-01, 9.11765242710817270e-01},
                {10000, 1.0 / 512.0, 10, 7.30238304879024091e-03, 1.38129108149696641e-02},
                {10000, 1.0 / 512.0, 20, 8.84284485101138745e-02, 6.00697641185654163e-01},
                {10000, 1.0 / 512.0, 35, 4.72669028400416347e-04, 9.99473593553733219e-01},
                {100000, 1.0 / 512.0, 194, 2.85312652425461326e-02, 4.81524138034491456e-01},
                {100000, 1.0 / 512.0, 220, 5.99722224921909498e-03, 9.62374499631485136e-01},
                {10000, 0.5, 4900, 1.07986432943335585e-03, 2.32927638524736934e-02},
                {10000, 0.5, 5000, 7.97864613938215408e-03, 5.03989323069691131e-01},
                {10000, 0.5, 5100, 1.07986432943335585e-03, 9.77787100476959714e-01},
                {50, 0.99, 48, 7.56180422654302992e-02, 8.94353130960306997e-02},
                {50, 0.99, 49, 3.05558619766432671e-01, 3.94993932862463371e-01},
                {50, 0.99, 50, 6.05006067137536685e-01, 1.0}
        };

        for (Object[] c : cases) {
            int n = (Integer) c[0];
            double p = (Double) c[1];
            int k = (Integer) c[2];
            double expectedPmf = (Double) c[3];
            double expectedCdf = (Double) c[4];

            BinomialDistribution dist = new BinomialDistribution(n, p);
            assertEquals("PMF mismatch for n=" + n + ", p=" + p + ", k=" + k,
                    expectedPmf, dist.probability(k), 1e-12);
            assertEquals("CDF mismatch for n=" + n + ", p=" + p + ", k=" + k,
                    expectedCdf, dist.cumulativeProbability(k), 1e-12);
        }
    }

    @Test
    public void cdfMatchesCumulativePmfSum() {
        int[] ns = {1, 5, 15, 16, 25, 100};
        double[] ps = {0.01, 0.25, 0.5, 0.75, 0.99};

        for (int n : ns) {
            for (double p : ps) {
                BinomialDistribution dist = new BinomialDistribution(n, p);
                double runningSum = 0.0;
                for (int k = 0; k <= n; k++) {
                    runningSum += dist.probability(k);
                    assertEquals("CDF != sum(PMF) for n=" + n + ", p=" + p + ", k=" + k,
                            runningSum, dist.cumulativeProbability(k), 1e-12);
                }
                assertEquals("PMF sum != 1 for n=" + n + ", p=" + p, 1.0, runningSum, 1e-12);
            }
        }
    }

    @Test
    public void complementSymmetry() {
        int[] ns = {10, 100, 10_000};
        double[] ps = {0.001, 0.1, 0.3, 0.5, 0.7, 0.99};

        for (int n : ns) {
            for (double p : ps) {
                BinomialDistribution d1 = new BinomialDistribution(n, p);
                BinomialDistribution d2 = new BinomialDistribution(n, 1.0 - p);

                for (int step = 0; step <= 10; step++) {
                    int k = (int) ((long) n * step / 10);
                    assertEquals(d1.probability(k), d2.probability(n - k), 1e-12);
                    assertEquals(d1.cumulativeProbability(k), 1.0 - d2.cumulativeProbability(n - k - 1), 1e-12);
                }
            }
        }
    }

    @Test
    public void largeTrials_remainsNumericallyStable() {
        // Apache Commons Math 3.0 returned -Infinity for these high-n / p=0.5 cases
        int[] largeNs = {100_000, 1_000_000, 100_000_000};
        for (int n : largeNs) {
            BinomialDistribution dist = new BinomialDistribution(n, 0.5);
            int half = n / 2;
            double pmfHalf = dist.probability(half);
            // By exact symmetry of Binomial(2m, 0.5), P(X <= m) = 0.5 + 0.5 * P(X = m)
            double expectedCdfHalf = 0.5 + 0.5 * pmfHalf;
            double actualCdfHalf = dist.cumulativeProbability(half);

            assertTrue("Expected finite CDF at n=" + n, Double.isFinite(actualCdfHalf));
            assertEquals("Symmetric CDF(n/2) mismatch at n=" + n, expectedCdfHalf, actualCdfHalf, 1e-11);
            assertEquals(1.0, dist.cumulativeProbability(n - 1), 1e-12);
            assertEquals(1.0, dist.cumulativeProbability(n), 0.0);
        }
    }

    @Test
    public void boundaryCases() {
        // n = 0
        BinomialDistribution zeroTrials = new BinomialDistribution(0, 0.5);
        assertEquals(0.0, zeroTrials.probability(-1), 0.0);
        assertEquals(1.0, zeroTrials.probability(0), 0.0);
        assertEquals(0.0, zeroTrials.probability(1), 0.0);
        assertEquals(0.0, zeroTrials.cumulativeProbability(-1), 0.0);
        assertEquals(1.0, zeroTrials.cumulativeProbability(0), 0.0);
        assertEquals(1.0, zeroTrials.cumulativeProbability(1), 0.0);

        // p = 0
        BinomialDistribution zeroP = new BinomialDistribution(10, 0.0);
        assertEquals(1.0, zeroP.probability(0), 0.0);
        assertEquals(0.0, zeroP.probability(1), 0.0);
        assertEquals(0.0, zeroP.cumulativeProbability(-1), 0.0);
        assertEquals(1.0, zeroP.cumulativeProbability(0), 0.0);
        assertEquals(1.0, zeroP.cumulativeProbability(5), 0.0);

        // p = 1
        BinomialDistribution oneP = new BinomialDistribution(10, 1.0);
        assertEquals(0.0, oneP.probability(9), 0.0);
        assertEquals(1.0, oneP.probability(10), 0.0);
        assertEquals(0.0, oneP.cumulativeProbability(9), 0.0);
        assertEquals(1.0, oneP.cumulativeProbability(10), 0.0);

        // Out-of-range x
        BinomialDistribution standard = new BinomialDistribution(10, 0.5);
        assertEquals(0.0, standard.probability(-1), 0.0);
        assertEquals(0.0, standard.probability(11), 0.0);
        assertEquals(0.0, standard.cumulativeProbability(-1), 0.0);
        assertEquals(1.0, standard.cumulativeProbability(11), 0.0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_rejectsNegativeTrials() {
        new BinomialDistribution(-1, 0.5);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_rejectsNegativeProbability() {
        new BinomialDistribution(10, -0.01);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_rejectsProbabilityAboveOne() {
        new BinomialDistribution(10, 1.01);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_rejectsNaNProbability() {
        new BinomialDistribution(10, Double.NaN);
    }
}
