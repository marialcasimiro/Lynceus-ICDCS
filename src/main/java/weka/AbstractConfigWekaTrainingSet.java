package weka;

import lynceus.Configuration;
import lynceus.ModelSample;
import lynceus.Pair;
import lynceus.TrainingSet;
import lynceus.WekaConfiguration;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

import java.io.File;
import java.io.FileReader;

/**
 * @author Diego Didona
 * @email diego.didona@epfl.ch
 * @since 21.03.18
 */
public abstract class AbstractConfigWekaTrainingSet<C extends Configuration, S extends ModelSample> implements TrainingSet<C, S>, WekaSet<C> {

   protected Instances set;

   protected AbstractConfigWekaTrainingSet(Instances i) {
      this.set = new Instances(i);
   }

   public AbstractConfigWekaTrainingSet(String arff) {
      try {
         final File f = new File(arff);
         if (!f.exists())
            throw new RuntimeException("FIle " + arff + " does not exist");
         this.set = new Instances(new FileReader(f));
         this.set.setClassIndex(set.numAttributes() - 1);
      } catch (Exception e) {
         e.printStackTrace();  // TODO: Customise this generated block
         throw new RuntimeException();
      }
   }

   public final Instances instances() {
      return this.set;
   }

   public Instance getInstance(C config) {
      for (Instance ii : set) {
         C c = buildConfigFromInstance(ii);
         if (config.equals(c)) {
            Instance i = new DenseInstance(ii);
            i.setDataset(this.set);
            return i;
         }
      }
      throw new RuntimeException("Config " + config + " not found in test set for return");
   }


   public final void add(C config, double target) {
      Instance i = ((WekaConfiguration) config).toInstance();
      i.setDataset(set);
      i.setClassValue(target);
      set.add(i);
   }

   public final int size() {
      return this.set.size();
   }

   public final Instances getInstancesCopy() {
      return new Instances(this.set);
   }

   public final void printAll() {
      System.out.println(this.set);
   }

   public abstract TrainingSet<C, S> clone();

   protected abstract C buildConfigFromInstance(Instance i);
   

   public final Pair<C, Double> getConfig(int index) {
      Instance ii = set.get(index);
      C v = buildConfigFromInstance(ii);
      return new Pair<>(v, ii.classValue());
   }
   
   public final void removeConfig(C config) {
      Instance removed = null;
      final int size = set.size();
      for (int i = 0; i < set.size(); i++) {
         Instance ii = set.get(i);
         C c = buildConfigFromInstance(ii);
         if (config.equals(c)) {
            removed = set.remove(i);
            break;
         }
      }
      if (removed == null || set.size() != size - 1) {
         printAll();
         throw new RuntimeException(config + " not found in test set for removal");
      }
   }


}
