package com.peanubnutter.collectionlogluck.luck.probability;

import java.util.List;

abstract class AbstractCustomProbabilityDistribution {

    protected final List<Double> probabilities;

    public AbstractCustomProbabilityDistribution(List<Double> probabilities) {
        for (double p : probabilities) {
            if (p < 0 || p > 1 || Double.isNaN(p)) {
                throw new IllegalArgumentException("Probability out of range [0, 1]: " + p);
            }
        }

        this.probabilities = probabilities;
    }

}
