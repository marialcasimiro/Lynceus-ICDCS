package weka;

import lynceus.ModelSample;
import weka.core.Instance;

/**
 * @author Diego Didona
 * @email diego.didona@epfl.ch
 * @since 12.03.18
 */
public class WekaModelSample implements ModelSample {
   private Instance instance;

   public WekaModelSample(Instance instance) {
      this.instance = instance;
   }

   public Instance getInstance() {
      return instance;
   }
}
