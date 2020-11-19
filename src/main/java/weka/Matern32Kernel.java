package weka;

import weka.classifiers.functions.supportVector.CachedKernel;
import weka.core.Instance;
import weka.core.Instances;

/**
 * @author Diego Didona
 * @email diego.didona@epfl.ch
 * @since 12.03.18
 * <p/>
 * https://ch.mathworks.com/help/stats/kernel-covariance-function-options.html?requestedDomain=true
 */
public class Matern32Kernel extends CachedKernel {

   private final double sl2; //characteristic length scale

   /*
   I now think that this sf2 is only here so that in the cov it gets summed
    */
   private final double sf2; //var of the signal
   private final double sl; //sqrt of the length scale

   public double getSf2() {
      return sf2;
   }

   public Matern32Kernel(double sl2, double sf2) {
      this.sl2 = sl2;
      this.sf2 = sf2;
      this.sl = Math.sqrt(this.sl2);
      //System.out.println("W=Theta " + this.sl2);
   }


   public Matern32Kernel(double sl2) {
      this.sl2 = sl2;
      this.sf2 = 1;
      this.sl = Math.sqrt(this.sl2);
      //System.out.println("W=Theta " + this.sl2);
   }

   public void buildKernel(Instances data) throws Exception {
      // does kernel handle the data?
      if (!getChecksTurnedOff()) {
         getCapabilities().testWithFail(data);
      }

      initVars(data);
   }

   private double distance(Instance i1, Instance i2) {
      //Euclidean
      double sum = 0;
      double diff;
      //-1 b/c we do not include the target in the distance
      for (int i = 0; i < i1.numAttributes() - 1; i++) {
         diff = (i1.value(i) / this.sl2 - i2.value(i) / this.sl2);
         sum += (diff * diff);
      }
      return Math.sqrt(sum);
   }

   public double getSl2() {
      return sl2;
   }

   @Override
   //https://github.com/bnjmn/weka/blob/master/weka/src/main/java/weka/classifiers/functions/supportVector/RBFKernel.java
   //GaussianProcess.java uses -1, inst, inst
   //So one instance is i1, the other is instance
   /**
    *
    * @param id1 the index of instance 1
    * @param id2 the index of instance 2
    * @param inst1 the instance 1 object
    * @return the dot product
    * @throws Exception if something goes wrong
    */
   protected double evaluate(int id1, int id2, Instance instance1) {
      //i can be ==i1 (also -1) to mean that we have to compute the dot prod of the instance
      //with itself
      if (id1 == id2) {
         return this.evaluate(instance1, instance1);
      }
      if (id1 != -1) {
         double d = this.evaluate(m_data.get(id1), m_data.get(id2));
         //System.out.println(m_data.get(id1) + " " + m_data.get(id2) + " " + d);
         return d;
      } else {
         // System.out.println("i = " + i + " i1 = " + i1);
         //System.out.println("3/2 evaluating " + instance + " vs " + m_data.get(i1));

         return this.evaluate(instance1, m_data.get(id2));
      }

   }

   final static private double sqt5 = Math.sqrt(5);


   private double evaluate(Instance i1, Instance i2) {
      final double r = distance(i1, i2);	// distance already applies the square root, so there's no need to apply it further
      double d = r * sqt5;
      d = (1. + d + (d * d) / 3.0) * Math.exp(-d);
      return this.sf2 * d;
      /*
      final double r2 = r * r;

      final double a = 1 + (sqt5 * r / sl) + (5 * r2 / (3 * this.sl2));
      final double e = Math.exp(-sqt5 * r / sl);

      return this.sf2 * a * e;   */
      /*
      double a = 1 + (Math.sqrt(3) * r / this.sl);
      double e = Math.exp(-(Math.sqrt(3) * r / this.sl));
      return this.s2_f * a * e;
      */

   }

   @Override
   public String globalInfo() {
      return null;  // TODO: Customise this generated block
   }
}
