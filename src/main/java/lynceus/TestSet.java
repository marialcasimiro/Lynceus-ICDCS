package lynceus;

import lynceus.Pair;

/**
 * @author Diego Didona
 * @email diego.didona@epfl.ch
 * @since 11.03.18
 */
public interface TestSet<C extends Configuration, S extends ModelSample> {
   int size();

   C removeAndReturn(int index);

   void addTestSample(C c);

   void addTestSampleWithTarget(C c, double y);

   void removeConfig(C config);

   C getConfig(int i);

   Pair<C, Double> getConfigAndTarget(int index);

    TestSet<C, S> clone();

    void printAll();
}
