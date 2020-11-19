package weka.tuning;

/**
 * @author Diego Didona
 * @email diego.didona@epfl.ch
 * @since 2019-02-19
 */
public class ModelParams {
   private double theta;
   private double sigma;

   public double getSigma() {
      return sigma;
   }


   public ModelParams(double theta, double sigma) {
      this.theta = theta;
      this.sigma = sigma;
   }

   public double getTheta() {
      return theta;
   }


   @Override
   public String toString() {
      return "ModelParams{" +
            "theta=" + theta +
            ", sigma=" + sigma +
            '}';
   }
}
