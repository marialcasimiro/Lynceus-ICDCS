package lynceus;

import weka.tuning.ModelParams;

/**
 * @author Diego Didona
 * @email diego.didona@epfl.ch
 * @since 11.03.18
 */
public class State<C extends Configuration, S extends ModelSample> {
   private double budget;
   private TrainingSet<C, S> trainingSet;
   private TestSet<C, S> testSet;
   private C currentConfiguration;

   private ModelParams params = null;

   public ModelParams getParams() {
      return params;
   }


   public void setParams(ModelParams params) {
      this.params = params;
   }

   public void resetParams() {
      this.params = null;
   }

   public State<C, S> clone() {
      State<C, S> ret = new State<C, S>();
      ret.setBudget(budget);
      //TODO: beware of the clones. TestSet has to be cloned with a copy of the instances.
      //Training set is not, because it copies the instances on the constructor
      //I guess things should be more explicit than that
      ret.setTrainingSet(trainingSet.clone());
      ret.setTestSet(testSet.clone());
      ret.setCurrentConfiguration(currentConfiguration); //we don't clone the config. It is never modified
      ret.setParams(params);
      return ret;
   }

   public void setBudget(double budget) {
      this.budget = budget;
   }

   public void setTrainingSet(TrainingSet<C, S> trainingSet) {
      this.trainingSet = trainingSet;
   }

   public void setTestSet(TestSet<C, S> testSet) {
      this.testSet = testSet;
   }

   public void setCurrentConfiguration(C currentConfiguration) {
      this.currentConfiguration = currentConfiguration;
   }

   public double getBudget() {
      return budget;
   }

   public TrainingSet<C, S> getTrainingSet() {
      return trainingSet;
   }

   public TestSet<C, S> getTestSet() {
      return testSet;
   }

   public C getCurrentConfiguration() {
      return currentConfiguration;
   }

   public void addTrainingSample(C config, double target) {
      trainingSet.add(config, target);
   }

   public void removeTestSample(C config) {
      testSet.removeConfig(config);
   }

   public void removeTrainingSample(C config) {
      trainingSet.removeConfig(config);
   }

   @Override
   public String toString() {
      return "State. Budget " + budget + " trainSetSize " + trainingSet.size() + " testSetSize " + testSet.size() + " currConfig " + currentConfiguration;
   }
}
