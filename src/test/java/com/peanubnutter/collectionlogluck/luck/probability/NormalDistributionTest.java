package com.peanubnutter.collectionlogluck.luck.probability;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class NormalDistributionTest {

    @Test
    public void standardNormal_referenceValues() {
        NormalDistribution dist = new NormalDistribution();

        // Double-precision reference values (Python math.erfc / math.exp) across all three Cody intervals
        // (|z/sqrt(2)| <= 0.46875, <= 4, > 4)
        double[][] cases = {
                // { z, expectedCdf, expectedPdf }
                {-10.0, 7.61985302416059451e-24, 7.69459862670641994e-23},
                {-8.2, 1.20193515427358981e-16, 9.99837874849717935e-16},
                {-6.0, 9.86587645037701606e-10, 6.07588284982328609e-09},
                {-4.0, 3.16712418331199650e-05, 1.33830225764885368e-04},
                {-3.0, 1.34989803163009588e-03, 4.43184841193800753e-03},
                {-2.0, 2.27501319481792155e-02, 5.39909665131880628e-02},
                {-1.96, 2.49978951482204351e-02, 5.84409443334514764e-02},
                {-1.0, 1.58655253931457074e-01, 2.41970724519143365e-01},
                {-0.5, 3.08537538725986937e-01, 3.52065326764299524e-01},
                {0.0, 0.5, 3.98942280401432703e-01},
                {0.5, 6.91462461274013007e-01, 3.52065326764299524e-01},
                {1.0, 8.41344746068542815e-01, 2.41970724519143365e-01},
                {1.96, 9.75002104851779627e-01, 5.84409443334514764e-02},
                {2.0, 9.77249868051820902e-01, 5.39909665131880628e-02},
                {3.0, 9.98650101968369897e-01, 4.43184841193800753e-03},
                {4.0, 9.99968328758166880e-01, 1.33830225764885368e-04},
                {6.0, 9.99999999013412300e-01, 6.07588284982328609e-09},
                {8.2, 9.99999999999999889e-01, 9.99837874849717935e-16}
        };

        for (double[] c : cases) {
            double z = c[0];
            double expectedCdf = c[1];
            double expectedPdf = c[2];

            double actualCdf = dist.cumulativeProbability(z);
            double actualPdf = dist.density(z);

            assertEquals("CDF mismatch at z=" + z, expectedCdf, actualCdf, expectedCdf * 1e-12);
            assertEquals("PDF mismatch at z=" + z, expectedPdf, actualPdf, expectedPdf * 1e-12);
        }
    }

    @Test
    public void nonStandardNormal_matchesStandardRescaling() {
        double mean = 1234.5;
        double sd = 67.89;
        NormalDistribution dist = new NormalDistribution(mean, sd);

        assertEquals(0.5, dist.cumulativeProbability(mean), 0.0);

        for (int i = -100; i <= 100; i++) {
            double z = i * 0.1;
            double x = mean + z * sd;
            assertEquals(NormalDistribution.STANDARD.cumulativeProbability(z), dist.cumulativeProbability(x), 1e-14);
            assertEquals(NormalDistribution.STANDARD.density(z) / sd, dist.density(x), 1e-14);
        }
    }

    @Test
    public void symmetryAndMonotonicity() {
        NormalDistribution dist = NormalDistribution.STANDARD;
        double prev = -1.0;

        for (int i = -100; i <= 100; i++) {
            double z = i * 0.1;
            double cdf = dist.cumulativeProbability(z);
            assertTrue("CDF not monotonic at z=" + z, cdf >= prev);
            prev = cdf;

            assertEquals("CDF symmetry failed at z=" + z, 1.0, cdf + dist.cumulativeProbability(-z), 1e-15);
            assertEquals("PDF symmetry failed at z=" + z, dist.density(z), dist.density(-z), 1e-15);
        }
    }

    @Test
    public void extremeAndSpecialValues() {
        NormalDistribution dist = new NormalDistribution(10.0, 2.0);

        assertEquals(0.0, dist.cumulativeProbability(Double.NEGATIVE_INFINITY), 0.0);
        assertEquals(1.0, dist.cumulativeProbability(Double.POSITIVE_INFINITY), 0.0);
        assertEquals(0.0, dist.cumulativeProbability(-1000.0), 0.0);
        assertEquals(1.0, dist.cumulativeProbability(1000.0), 0.0);
        assertTrue(Double.isNaN(dist.cumulativeProbability(Double.NaN)));

        assertEquals(0.0, dist.density(Double.NEGATIVE_INFINITY), 0.0);
        assertEquals(0.0, dist.density(Double.POSITIVE_INFINITY), 0.0);
        assertTrue(Double.isNaN(dist.density(Double.NaN)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_rejectsZeroStdDev() {
        new NormalDistribution(0.0, 0.0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_rejectsNegativeStdDev() {
        new NormalDistribution(0.0, -1.0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_rejectsNaNMean() {
        new NormalDistribution(Double.NaN, 1.0);
    }
}
