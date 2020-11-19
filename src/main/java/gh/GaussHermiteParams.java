package gh;

/**
 * @author Diego Didona
 * @email diego.didona@epfl.ch
 * @since 10.03.18
 */
public class GaussHermiteParams {

   /* class attributes */
   double[] weights;
   double[] values;

   /* class constructor */
   public GaussHermiteParams(int p) {
      this.weights = new double[p];
      this.values = new double[p];
   }


   /* getters */
   public double getWeight(int i) {
      return weights[i];
   }


   public double getValue(int i) {
      return values[i];
   }


   /* other methods */
   public void checkIntegral(double target) {
      double sum = 0;
      for (int i = 0; i < weights.length; i++) {
         sum += weights[i] * values[i];
      }
      //System.out.println("Checking. Value is " + sum + " target is " + target);
      if ((Math.abs(sum - target) / target) > 0.01)
         throw new RuntimeException("Approx error in GH is higher than 1%");
      /*
      else
         System.out.println(sum+ " vs "+target);
         */
   }

   public void addValues(int i, double weight, double value) {
      this.weights[i] = weight;
      this.values[i] = value;
   }


   public int cardinality() {
      return weights.length;
   }


   /* check behavior */
   public static void main(String[] args) {
      /* test accuracy in integrating*/
      double mu = 0.04;
      double s_squared = 0.04;
      double s = Math.sqrt(s_squared);

      GaussHermiteParams p = NormalGaussHermiteQuadrature.decompose(5, mu, s_squared);

      double sum = 0;
      for (int i = 0; i < p.cardinality(); i++) {
         System.out.println(i + "-th value =" + p.getValue(i));
         if (p.getValue(i) > 0)
            sum += p.getValue(i) * p.getWeight(i);
      }
      System.out.println("Integral value w/o negatives is " + sum + " (should be)" + mu);


      /*
      From wikipedia
       */
      sum = 0;
      sum += 0.0199532 * (Math.sqrt(2.0 * s) * -2.02018 + mu);
      sum += 0.393619 * (Math.sqrt(2.0 * s) * -0.958573 + mu);
      sum += 0.945309 * (Math.sqrt(2.0 * s) * 0 + mu);
      sum += 0.393619 * (Math.sqrt(2.0 * s) * 0.958573 + mu);
      sum += 0.0199532 * (Math.sqrt(2.0 * s) * 2.02018 + mu);
      sum /= Math.sqrt(Math.PI);

      //I am computing the expected value here!
      System.out.println("Integral value is " + sum + " (should be )" + mu);


      /*
      I have two choices
      1// use the change of variable in wikipedia
      2// add the ex^2. But then: the weight has to stay the same, and my y_i becomes
      abscissa * exp(abscissa^2)
       */

   }
}
