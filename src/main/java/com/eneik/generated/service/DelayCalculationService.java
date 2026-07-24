package com.eneik.generated.service;

import org.springframework.stereotype.Service;
import java.util.Random;

@Service
public class DelayCalculationService {

    private final Random random;

    public DelayCalculationService() {
        this.random = new Random();
    }

    // Allows seeding/dependency injection for predictability in tests
    public DelayCalculationService(Random random) {
        this.random = random;
    }

    /**
     * Calculates action delay via an exponential distribution.
     * The probability of generating a value is defined by:
     *   delay = - meanDelaySeconds * ln(1 - u)
     * where u is a uniform random variable in [0, 1).
     * To avoid ln(0) (when u=1), we can use (1.0 - u) which is in (0, 1].
     *
     * @param meanDelaySeconds the expected average delay
     * @return the computed non-linear delay in seconds
     */
    public double calculateExponentialDelay(double meanDelaySeconds) {
        if (meanDelaySeconds <= 0) {
            throw new IllegalArgumentException("Mean delay seconds must be greater than zero");
        }
        double u = random.nextDouble();
        // Since u is in [0, 1), 1.0 - u is in (0, 1] which is safe for logarithm
        return - meanDelaySeconds * Math.log(1.0 - u);
    }
}
