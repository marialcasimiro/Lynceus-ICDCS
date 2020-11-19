package weka.tensorflow;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import lynceus.CostGenerator;
import lynceus.LHS;
import lynceus.Lynceus;
import lynceus.Pair;
import lynceus.PredictiveModel;
import lynceus.TestSet;
import lynceus.TrainingSet;
import lynceus.tensorflow.TensorflowConfig;
import lynceus.tensorflow.TensorflowConfigCostGenerator;
import lynceus.tensorflow.TensorflowLHS;
import weka.HackGaussianProcess;
import weka.WekaGaussianProcess;
import weka.WekaModelSample;
import weka.WekaSet;
import weka.tuning.ModelParams;

public class WekaTensorflowConfigLynceus extends Lynceus<TensorflowConfig, WekaModelSample>{

	private static boolean printed = false;
	private static String datasetFile;
	
	/* class constructor */
	public WekaTensorflowConfigLynceus(long h, double b, double epsilon, double gamma, optimizer opt, long wkldid, earlyStopOptions es) {
		super(h, b, epsilon, gamma, opt, wkldid, es);
	}

	/* superclass abstract methods to be implemented */
	@Override
	protected CostGenerator<TensorflowConfig> buildCostGenerator(long seed) {
		if (costGenerator == null) {
			try {
				costGenerator = new TensorflowConfigCostGenerator(datasetFile);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    }
	    return costGenerator;
	}

	@Override
	protected TestSet<TensorflowConfig, WekaModelSample> initialTestSet() {
		if (!printed) {
	         printAll();
	         printed = true;
	      }
	      return WekaTensorflowConfigFactory.buildInitTestSet("files/tensorflow.arff");
	}

	@Override
	protected PredictiveModel<TensorflowConfig, WekaModelSample> buildPredictiveModel(TrainingSet<TensorflowConfig, WekaModelSample> trainingSet, ModelParams params) {
		return new WekaGaussianProcess<>((WekaSet) trainingSet,params);
	}

	@Override
	protected PredictiveModel<TensorflowConfig, WekaModelSample> buildPredictiveModelForApprox(TrainingSet<TensorflowConfig, WekaModelSample> trainingSet) {
		return new HackGaussianProcess<TensorflowConfig>((WekaSet) trainingSet);
		//throw new RuntimeException("[WekaConfigLynceus] buildPredictiveModelForApprox not supported yet");
	}

	@Override
	protected TrainingSet<TensorflowConfig, WekaModelSample> emptyTrainingSet() {
		final TensorflowConfigWekaTestSet ts = (TensorflowConfigWekaTestSet) this.testSet;
	    return new TensorflowConfigWekaTrainingSet(ts.arff());
	}

	@Override
	protected TrainingSet<TensorflowConfig, WekaModelSample> emptyTrainingSetForApprox() {
		//throw new RuntimeException("[WekaConfigLynceus] emptyTrainingSetForApprox not supported yet");
		return new TensorflowConfigWekaTrainingSet("files/tensorflow_ei.arff");
	}

	@Override
	protected TestSet<TensorflowConfig, WekaModelSample> fullTestSet() {
		return WekaTensorflowConfigFactory.buildInitTestSet("files/tensorflow.arff");
	}

	/* other methods */
	private TestSet<TensorflowConfig, WekaModelSample> printAll() {
        
		TestSet<TensorflowConfig, WekaModelSample> testSet = WekaTensorflowConfigFactory.buildInitTestSet("files/tensorflow.arff");
        if (costGenerator == null) {
           throw new RuntimeException("[WekaTensorflowConfigLynceus] Cost generator is null");
        }
        System.out.println("[WekaTensorflowConfigLynceus] PRE  Total test set size = " + testSet.size());

        Set<Pair<TensorflowConfig, Double>> set = new HashSet<Pair<TensorflowConfig, Double>>();
        for (int i = 0; i < testSet.size(); i++) {
           TensorflowConfig c = testSet.getConfig(i);
           double runningCost = costGenerator.deploymentCost(null, c);
           set.add(new Pair<TensorflowConfig, Double>(c, runningCost));
        }

        for (Pair<TensorflowConfig, Double> p : set) {
           testSet.removeConfig(p.getFst());
           testSet.addTestSampleWithTarget(p.getFst(), p.getSnd());
           //System.out.println("added " + p.fst + ", " + p.snd);
        }

        System.out.println("[WekaTensorflowConfigLynceus] POST Total test set size = " + testSet.size());
        testSet.printAll();
        return testSet;
   }
	
	public static void setDatasetFile(String file) {
		datasetFile = file;
	}

	@Override
	protected LHS<TensorflowConfig> instantiateLHS(int initTrainSamples) {
		return new TensorflowLHS(initTrainSamples);
	}

	
}
