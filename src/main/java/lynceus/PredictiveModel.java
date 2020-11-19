package lynceus;

import weka.WekaSet;

/**
 * @author Diego Didona
 * @email diego.didona@epfl.ch
 * @since 11.03.18
 */
public interface PredictiveModel<C extends Configuration, S extends ModelSample> {
   void train();

   double evaluate(C config);

   double stdv(C config);

   void test(TestSet<C, S> testSet);

   void testOnTrain(TrainingSet<C, S> testSet);
   
   double maxVariance(TestSet<C, S> testSet);

}
