package weka.scout;

import lynceus.TrainingSet;
import lynceus.scout.ScoutVMConfig;
import weka.AbstractConfigWekaTrainingSet;
import weka.WekaModelSample;
import weka.core.Instance;
import weka.core.Instances;

/**
 * @author Diego Didona
 * @email diego.didona@epfl.ch
 * @since 09.04.18
 */
public class ScoutVMConfigWekaTrainingSet extends AbstractConfigWekaTrainingSet<ScoutVMConfig, WekaModelSample> {
   public ScoutVMConfigWekaTrainingSet(String arff) {
      super(arff);
   }


   private ScoutVMConfigWekaTrainingSet(Instances i) {
      super(i);
   }

   @Override
   public TrainingSet<ScoutVMConfig, WekaModelSample> clone() {
      return new ScoutVMConfigWekaTrainingSet(this.set);
   }


   @Override
   protected ScoutVMConfig buildConfigFromInstance(Instance i) {
      return new WekaScoutVMConfig(i);
   }
}
