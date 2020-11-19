package weka.scout;

import java.util.ArrayList;

import lynceus.Pair;
import lynceus.WekaConfiguration;
import lynceus.aws.AWSDirectory;
import lynceus.scout.ScoutVMConfig;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

/**
 * @author Diego Didona
 * @email diego.didona@epfl.ch
 * @since 12.03.18
 */
public class WekaScoutVMConfig extends ScoutVMConfig implements WekaConfiguration {

   @Override
   public WekaScoutVMConfig clone() {
      return new WekaScoutVMConfig(this, this.dataSet);   //NB: this is *not* copying the instances
   }

   private Instances dataSet; //this is only for creating instances

   public WekaScoutVMConfig(Instance i) {

      super(AWSDirectory.AWSInstanceType.valueOf(i.stringValue(0)),
            AWSDirectory.sizeFromAttributes(i.stringValue(0), i.value(1), i.value(2), i.value(4)),
            i.value(1), i.value(2), i.value(3), i.value(4), i.value(5), i.toString(6), i.value(7));

      this.dataSet = i.dataset();
      if (this.dataSet == null)
         throw new RuntimeException("Cannot be null");
   }


   public Instance toInstance() {

      Instance rr = new DenseInstance(numAttributes() + 1);  // + 1 for the target attribute
      rr.setDataset(this.dataSet); //first set dataset, otherwise following statements fail with "no dataset associated" exception
      rr.setValue(0, this.getType().toString());
      rr.setValue(1, this.getVcpus());
      rr.setValue(2, this.getEcus());
      rr.setValue(3, this.getFrequency());
      rr.setValue(4, this.getRam());
      rr.setValue(5, this.getEbs_band());
      rr.setValue(6, this.getNet_band());
      rr.setValue(7, this.getNum_instances());
      return rr;
   }

   public WekaScoutVMConfig(ScoutVMConfig c, Instances d) {
      super(c.getType(), c.getSize(), c.getVcpus(), c.getEcus(), c.getFrequency(), c.getRam(), c.getEbs_band(), c.getNet_band(), c.getNum_instances());
      this.dataSet = d;
      if (this.dataSet == null)
         throw new RuntimeException("Cannot be null");
   }


   @Override
   public String toString() {
      return super.toString();
   }


   @Override
   public ArrayList<WekaConfiguration> neighbourhood() {
	   // TODO Auto-generated method stub
	   return null;
   }


   @Override
   public boolean findPair(ArrayList<Pair> searchSpace, Pair pair) {
	   // TODO Auto-generated method stub
	   return false;
   }
}
