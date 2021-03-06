/******************************************************************************
 *                   Confidential Proprietary                                 *
 *         (c) Copyright Haifeng Li 2011, All Rights Reserved                 *
 ******************************************************************************/

package smile.stat.distribution;

import java.util.ArrayList;
import java.util.List;
import smile.math.Math;

/**
 * The finite mixture of distributions from discrete exponential family.
 * The EM algorithm is provided to learn the mixture model from data.
 *
 * @author Haifeng Li
 */
public class DiscreteExponentialFamilyMixture extends DiscreteMixture {

    /**
     * Constructor.
     */
    DiscreteExponentialFamilyMixture() {
        super();
    }

    /**
     * Constructor.
     * @param mixture a list of discrete exponential family distributions.
     */
    public DiscreteExponentialFamilyMixture(List<Component> mixture) {
        super(mixture);

        for (Component component : mixture) {
            if (component.distribution instanceof DiscreteExponentialFamily == false)
                throw new IllegalArgumentException("Component " + component + " is not of discrete exponential family.");
        }
    }

    /**
     * Constructor. The mixture model will be learned from the given data with the
     * EM algorithm.
     * @param mixture the initial guess of mixture. Components may have
     * different distribution form.
     * @param data the training data.
     */
    public DiscreteExponentialFamilyMixture(List<Component> mixture, int[] data) {
        this(mixture);

        EM(components, data);
    }

    /**
     * Standard EM algorithm which iteratively alternates
     * Expectation and Maximization steps until convergence.
     *
     * @param mixture the initial configuration.
     * @param x the input data.
     * @return log Likelihood
     */
    double EM(List<Component> mixture, int[] x) {
        return EM(mixture, x, 0.2);
    }

    /**
     * Standard EM algorithm which iteratively alternates
     * Expectation and Maximization steps until convergence.
     *
     * @param mixture the initial configuration.
     * @param x the input data.
     * @param gamma the regularization parameter.
     * @return log Likelihood
     */
    double EM(List<Component> mixture, int[] x, double gamma) {
        return EM(mixture, x, gamma, Integer.MAX_VALUE);
    }

    /**
     * Standard EM algorithm which iteratively alternates
     * Expectation and Maximization steps until convergence.
     *
     * @param components the initial configuration.
     * @param x the input data.
     * @param gamma the regularization parameter.
     * @param maxIter the maximum number of iterations. If maxIter &le; 0, then the
     * algorithm iterates until converge.
     * @return log Likelihood
     */
    double EM(List<Component> components, int[] x , double gamma, int maxIter) {
        if (x.length < components.size() / 2)
                throw new IllegalArgumentException("Too many components");

        if (gamma < 0.0 || gamma > 0.2)
            throw new IllegalArgumentException("Invalid regularization factor gamma.");

        if (maxIter <= 0)
            maxIter = Integer.MAX_VALUE;

        int n = x.length;
        int m = components.size();

        double[][] posteriori = new double[m][n];

        // Log Likelihood
        double L = 0.0;
        for (double xi : x) {
            double p = 0.0;
            for (Component c : components)
                p += c.priori * c.distribution.p(xi);
            if (p > 0) L += Math.log(p);
        }

        // EM loop until convergence
        int iter = 0;
        for (; iter < maxIter; iter++) {

            // Expectation step
            for (int i = 0; i < m; i++) {
                Component c = components.get(i);

                for (int j = 0; j < n; j++) {
                    posteriori[i][j] = c.priori * c.distribution.p(x[j]);
                }
            }

            // Normalize posteriori probability.
            for (int j = 0; j < n; j++) {
                double p = 0.0;

                for (int i = 0; i < m; i++) {
                    p += posteriori[i][j];
                }

                for (int i = 0; i < m; i++) {
                    posteriori[i][j] /= p;
                }

                // Adjust posterior probabilites based on Regularized EM algorithm.
                if (gamma > 0) {
                    for (int i = 0; i < m; i++) {
                        posteriori[i][j] *= (1 + gamma * Math.log2(posteriori[i][j]));
                        if (Double.isNaN(posteriori[i][j]) || posteriori[i][j] < 0.0) {
                            posteriori[i][j] = 0.0;
                        }
                    }
                }
            }

            // Maximization step
            List<Component> newConfig = new ArrayList<Component>();
            for (int i = 0; i < m; i++)
                newConfig.add(((DiscreteExponentialFamily) components.get(i).distribution).M(x, posteriori[i]));

            double sumAlpha = 0.0;
            for (int i = 0; i < m; i++)
                sumAlpha += newConfig.get(i).priori;

            for (int i = 0; i < m; i++)
                newConfig.get(i).priori /= sumAlpha;

            double newL = 0.0;
            for (double xi : x) {
                double p = 0.0;
                for (Component c : newConfig) {
                    p += c.priori * c.distribution.p(xi);
                }
                if (p > 0) newL += Math.log(p);
            }

            if (newL > L) {
                L = newL;
                components.clear();
                components.addAll(newConfig);
            } else {
                break;
            }
        }

        return L;
    }
}
