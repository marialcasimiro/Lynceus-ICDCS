package weka.scout;

import lynceus.CostGenerator;
import lynceus.LHS;
import lynceus.Lynceus;
import lynceus.PredictiveModel;
import lynceus.TestSet;
import lynceus.TrainingSet;
import lynceus.scout.ScoutLHS;
import lynceus.scout.ScoutVMConfig;
import lynceus.scout.ScoutVMCostGenerator;
import lynceus.scout.ScoutVMConfig;
import lynceus.scout.ScoutVMCostGenerator;
import lynceus.tensorflow.TensorflowLHS;
import weka.HackGaussianProcess;
import weka.WekaGaussianProcess;
import weka.WekaModelSample;
import weka.WekaSet;
import weka.scout.ScoutVMConfigWekaTestSet;
import weka.scout.ScoutVMConfigWekaTrainingSet;
import weka.scout.WekaScoutVMConfigFactory;
import weka.tuning.ModelParams;

/**
 * @author Diego Didona
 * @email diego.didona@epfl.ch
 * @since 09.04.18
 */
public class WekaScoutLynceus extends Lynceus<ScoutVMConfig, WekaModelSample> {
   public WekaScoutLynceus(long h, double b, double epsilon, double gamma, optimizer opt, long wkldid, earlyStopOptions es) {
      super(h, b, epsilon, gamma, opt, wkldid, es);
   }

   @Override
   protected CostGenerator<ScoutVMConfig> buildCostGenerator(long seed) {
      if (costGenerator == null) {
         costGenerator = new ScoutVMCostGenerator();
      }
      return costGenerator;
   }

   @Override
   protected TestSet<ScoutVMConfig, WekaModelSample> initialTestSet() {
      return WekaScoutVMConfigFactory.buildInitTestSet("files/extended_scout.arff");
   }

   @Override
   protected PredictiveModel<ScoutVMConfig, WekaModelSample> buildPredictiveModel(TrainingSet<ScoutVMConfig, WekaModelSample> trainingSet, ModelParams params) {
      return new WekaGaussianProcess<ScoutVMConfig>((WekaSet) trainingSet,params);
   }

   @Override
   protected TrainingSet<ScoutVMConfig, WekaModelSample> emptyTrainingSet() {
      final ScoutVMConfigWekaTestSet ts = (ScoutVMConfigWekaTestSet) this.testSet;
      assert ts != null;
      return new ScoutVMConfigWekaTrainingSet(ts.arff());
   }


   @Override
   protected PredictiveModel<ScoutVMConfig, WekaModelSample> buildPredictiveModelForApprox(TrainingSet<ScoutVMConfig, WekaModelSample> trainingSet) {
      return new HackGaussianProcess<ScoutVMConfig>((WekaSet) trainingSet);
   }

   @Override
   protected TrainingSet<ScoutVMConfig, WekaModelSample> emptyTrainingSetForApprox() {
      if(true)throw  new RuntimeException("NO");
      return new ScoutVMConfigWekaTrainingSet("files/scout_utility.arff");
   }

   @Override
   protected TestSet<ScoutVMConfig, WekaModelSample> fullTestSet() {
      return WekaScoutVMConfigFactory.buildInitTestSet("files/extended_scout.arff");
   }

	@Override
	protected LHS<ScoutVMConfig> instantiateLHS(int initTrainSamples) {
		return new ScoutLHS(initTrainSamples);
	}

  

}
