package weka;

import lynceus.Configuration;
import weka.classifiers.trees.RandomForest;

/**
 * @author Diego Didona
 * @email diego.didona@epfl.ch
 * @since 28.03.18
 */

public class HackGaussianProcess<C extends Configuration> extends WekaGaussianProcess<C> {

   public HackGaussianProcess(WekaSet<C> set) {
      this.trainingSet = set;
      this.model = new RandomForest();
   }

   public double stdv(C config) {
      throw new UnsupportedOperationException("STDV not supported by hack");
   }
}
