package weka.scout;

import lynceus.TestSet;
import lynceus.WekaConfiguration;
import lynceus.scout.ScoutVMConfig;
import weka.AbstractConfigWekaTestSet;
import weka.WekaModelSample;
import weka.core.Instance;
import weka.core.Instances;

/**
 * @author Diego Didona
 * @email diego.didona@epfl.ch
 * @since 09.04.18
 */
public class ScoutVMConfigWekaTestSet extends AbstractConfigWekaTestSet<ScoutVMConfig, WekaModelSample> {
   public ScoutVMConfigWekaTestSet(Instances set, String arff) {
      super(set, arff);
   }

   public ScoutVMConfigWekaTestSet(String arff) {
      super(arff);
   }

   @Override
   public TestSet<ScoutVMConfig, WekaModelSample> clone() {
      return new ScoutVMConfigWekaTestSet(new Instances(this.set), this.arff);
   }

   @Override
   public ScoutVMConfig buildFromInstance(Instance i) {
      return new WekaScoutVMConfig(i);
   }


   @Override
   protected WekaConfiguration buildFromConfigAndSet(ScoutVMConfig c, Instances set) {
      return new WekaScoutVMConfig(c, set);
   }

}
