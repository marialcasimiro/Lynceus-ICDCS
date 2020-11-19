package lynceus;

import org.apache.commons.math3.distribution.NormalDistribution;

public class TruncatedNormalDistribution {
	
	/* class attributes */
	protected double mu;		// mean of the normal distribution
	protected double sigma;		// standard deviation of the normal distribution
	protected double a;			// bound of the lower truncated tail
	protected double b;			// bound of the upper truncated tail
	protected double alpha;		// alpha = (a - mu) / sigma
	protected double beta;		// beta = (b - mu) / sigma
	
	protected double cdf_beta;	// CDF(beta) = CDF((b - mu) / sigma)
	protected double cdf_alpha;	// CDF(alpha) = CDF((a - mu) / sigma)
	protected double Z;			// Z = CDF(beta) - CDF(alpha)
	
	protected double pdf_beta;	// PDF(beta) = PDF((b - mu) / sigma)
	protected double pdf_alpha;	// PDF(alpha) = PDF((a - mu) / sigma)
	
	NormalDistribution originalDistribution;	// original normal distribution with mean mu and standard deviation sigma which will be truncted
	NormalDistribution standardNormal;
	/* class constructors */

	/**
	 * class constructor
	 * @param mu:	 mean of the original normal distribution
	 * @param sigma: variance of the original normal distribution
	 * @param a:	 bound of the lower truncated tail
	 * @param b:	 bound of the upper truncated tail
	 */
	public TruncatedNormalDistribution (double mu, double sigma, double a, double b) {		
		this.mu = mu;
		this.sigma = sigma;
		this.a = a;
		this.b = b;
		initClassAttributes();
		
		originalDistribution = new NormalDistribution(this.mu, this.sigma);
	}
		
	/**
	 * class constructor
	 * @param originalDistribution:	original normal distribution that one wants to truncate
	 * @param a:	 				bound of the lower truncated tail
	 * @param b:				 	bound of the upper truncated tail
	 */
	public TruncatedNormalDistribution (NormalDistribution originalDistribution, double a, double b) {
		
		this.originalDistribution = originalDistribution;
		this.mu = originalDistribution.getMean();
		this.sigma = originalDistribution.getStandardDeviation();
		
		this.a = a;
		this.b = b;
		
		initClassAttributes();
	}
		
	/**
	 * This method is used by the constructor to compute other class attributes based on the bounds and on the
	 * original mean and variance
	 * @param mu:	 mean of the original normal distribution
	 * @param sigma: variance of the original normal distribution
	 * @param a:	 bound of the lower truncated tail
	 * @param b:	 bound of the upper truncated tail
	 */
	private void initClassAttributes () {
		if (this.sigma == 0) {
            //System.out.println("STDV is zero.");
            //TODO: should we do something specific when std = 0?
            this.sigma = 0.000000000000000000001;
        }
		
		this.standardNormal = new NormalDistribution(0, 1);
		
		this.alpha = (this.a - mu) / sigma;
		if (this.a == Double.NEGATIVE_INFINITY) {
			this.cdf_alpha = 0;
			this.pdf_alpha = 0;
		} else {
			this.cdf_alpha = standardNormal.cumulativeProbability(this.alpha);
			this.pdf_alpha = standardNormal.density(this.alpha);
		}
		
		this.beta = (this.b - mu) / sigma;
		if (this.b == Double.POSITIVE_INFINITY) {
			this.cdf_beta = 1;
			this.pdf_beta = 0;
		} else {
			this.cdf_beta = standardNormal.cumulativeProbability(this.beta);
			this.pdf_beta = standardNormal.density(this.beta);
		}
	
		this.Z = this.cdf_beta - this.cdf_alpha;	

//		System.out.println("mu=" + this.mu + " ; sigma=" + this.sigma + " ; a=" + this.a + " ; b=" + this.b);
//		System.out.println("alpha=" + this.alpha + " ; cdf_alpha=" + this.cdf_alpha + " ; pdf_alpha=" + this.pdf_alpha);
//		System.out.println("beta=" + this.beta + " ; cdf_beta=" + this.cdf_beta + " ; pdf_beta=" + this.pdf_beta);
//		System.out.println("Z=" + this.Z);
	}

	/* getters */
	
	/**
	 * 
	 * @param x
	 * @return
	 */
	public double getPDF (double x) {
		double xi = (x - this.mu) / this.sigma;
		double xi_pdf = standardNormal.density(xi);
						
		if (x < a || x > b) {
			return 0.0;
		} else {
			return xi_pdf / (this.sigma * this.Z);
		}
	}
	
	/**
	 * @param x
	 * @return the probability of getting x 
	 */
	public double getCDF (double x) {
		double xi = (x - this.mu) / this.sigma;
		
		double xi_cdf = standardNormal.cumulativeProbability(xi);
		if (x < a) {
			return 0.0;
		} else if (x <= b ) {
			return (xi_cdf - this.cdf_alpha) / this.Z;
		} else {
			return 1.0;
		}
	}
	
	/**
	 * @return mean of the truncated normal distribution
	 */
	public double getMean () {
		return mu + sigma * ((pdf_alpha - pdf_beta) / Z);
	}
	
	/**
	 * @return mode of the truncated normal distribution
	 */
	public double getMode () {
		if (a > mu) {
			return a;
		} else if (a <= mu && mu <= b) {
			return mu;
		} else {
			return b;
		}
	}
	
	/**
	 * @return variance of the truncated normal distribution
	 */
	public double getVariance () {
		return Math.pow(sigma, 2) * (1 + ((alpha*pdf_alpha - beta*pdf_beta) / Z) - Math.pow((pdf_alpha - pdf_beta) / Z, 2));
	}

	
	/**
	 * @return mean of the (original) normal distribution
	 */
	public double getMu() {
		return mu;
	}

	
	/**
	 * @return standard deviation of the (original) normal distribution
	 */
	public double getSigma() {
		return sigma;
	}

	
	/** 
	 * @return bound of the lower truncated tail
	 */
	public double getA() {
		return a;
	}

	
	/**
	 * @return bound of the upper truncated tail
	 */
	public double getB() {
		return b;
	}

	
	
	/* only for testing purposes */
//	public static void main(String[] args) {
//		
//		/* comparing against the values of page 20 of
//		 * https://people.sc.fsu.edu/~jburkardt/presentations/truncated_normal.pdf 
//		 * normal gaussian with mu = 100, sigma = 25, a = 50 and b = 150 */
//		NormalDistribution normalDist = new NormalDistribution(100, 25);
//		TruncatedNormalDistribution truncatedDist = new TruncatedNormalDistribution(normalDist, 50, 150);
//
//		double[] x = {40.032, 81.63, 137.962, 122.367, 103.704, 94.899, 65.8326, 84.5743, 71.5672, 62.0654, 108.155, 170.356};
////		double[] x = {90, 92, 94, 96, 98, 100, 102, 104, 106, 108, 110};
////		double[] x = {70, 73, 76, 79, 82, 85, 88, 91, 94, 97, 100, 103};
//		
//		for(int i = 0; i < x.length; i++) {
//			System.out.println("x=" + x[i]);
////			System.out.println("\tnormalPDF=" + normalDist.density(x[i]) + " ; normalCDF=" + normalDist.cumulativeProbability(x[i]));
//			System.out.println("\ttruncPDF=" + truncatedDist.getPDF(x[i]) + " ; truncCDF=" + truncatedDist.getCDF(x[i]) + " ; truncMean=" + truncatedDist.getMean());
//		}
//		
//	}
	
}
