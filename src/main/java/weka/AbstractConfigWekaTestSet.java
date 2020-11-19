package weka;

import lynceus.Configuration;
import lynceus.ModelSample;
import lynceus.Pair;
import lynceus.TestSet;
import lynceus.WekaConfiguration;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

import java.io.File;
import java.io.FileReader;

/**
 * @author Diego Didona
 * @email diego.didona@epfl.ch
 * @since 20.03.18
 */
public abstract class AbstractConfigWekaTestSet<C extends Configuration, S extends ModelSample> implements TestSet<C, S>, WekaSet<C> {
   protected Instances set;
   protected String arff;

   public AbstractConfigWekaTestSet(Instances set, String arff) {
      this.set = set;
      this.arff = arff;
   }

   public AbstractConfigWekaTestSet(String arff) {
      try {
         this.set = new Instances(new FileReader(new File(arff)));
         this.set.setClassIndex(set.numAttributes() - 1);
         this.arff = arff;
      } catch (Exception e) {
         e.printStackTrace();  // TODO: Customise this generated block
         throw new RuntimeException();
      }
   }

   public final Instance getInstance(C config) {
      for (Instance ii : set) {
         C c = buildFromInstance(ii);
         if (config.equals(c)) {
            Instance i =  new DenseInstance(ii);
            i.setDataset(this.set);
            return i;
         }
      }
      throw new RuntimeException("Config " + config + " not found in test set for return");
   }

   @Override
   public void printAll() {
      System.out.println(this.set);
   }

   public Pair<C, Double> getConfigAndTarget(int index) {
      if(true)throw new UnsupportedOperationException("Not to be used. IDK when we set the target or not");
      Instance ii = set.get(index);
      C c = getConfig(index);
      return new Pair<>(c, ii.classValue());
   }

   public final void removeConfig(C config) {
      Instance removed = null;
      final int size = set.size();
      for (int i = 0; i < set.size(); i++) {
         Instance ii = set.get(i);
         C c = buildFromInstance(ii);
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


   @Override
   public final C removeAndReturn(int index) {
      final Instance i = set.instance(index);
      final C c = buildFromInstance(i);
      set.remove(index);
      return c;
   }


   public Instances instances() {
      return this.set;
   }

   public WekaModelSample at(int i) {
      return null;  // TODO: Customise this generated block
   }

   public final C getConfig(int i) {   
      return buildFromInstance(this.set.get(i));
   }


   public abstract TestSet<C, S> clone();

   protected abstract WekaConfiguration buildFromConfigAndSet(C c, Instances i);

   public abstract C buildFromInstance(Instance i);


   public final void addTestSampleWithTarget(C c, double y) {
      Instance i = (((WekaConfiguration) c).toInstance());
      i.setDataset(this.set);
      i.setClassValue(y);
      this.set.add(i);
   }


   public final void addTestSample(C c) {
      //The VMConfig is not necessarily linked to a set, because it might not be a weka config yet
      //So we hve to create the instance, set the current set and then add it
      WekaConfiguration w = this.buildFromConfigAndSet(c, this.set);
      final Instance i = w.toInstance();
      i.setDataset(this.set);
      this.set.add(i);
   }

   public Instances getInstancesCopy() {
      return new Instances(this.set);
   }

   public String arff() {
      return this.arff;
   }

   public int size() {
      return set.size();
   }
}
