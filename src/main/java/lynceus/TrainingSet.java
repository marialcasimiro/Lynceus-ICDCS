package lynceus;

import weka.core.Instances;

/**
 * @author Diego Didona
 * @email diego.didona@epfl.ch
 * @since 11.03.18
 */
public interface TrainingSet<C extends Configuration, S extends ModelSample> {
   
   void add(C c, double target);

   Pair<C, Double> getConfig(int i);

   int size();

   TrainingSet<C, S> clone();
   
   void removeConfig(C config);

   void printAll();
   
   Instances instances();
}
