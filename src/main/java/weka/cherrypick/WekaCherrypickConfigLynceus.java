package weka.cherrypick;

import java.util.HashSet;
import java.util.Set;

import lynceus.CostGenerator;
import lynceus.LHS;
import lynceus.Lynceus;
import lynceus.Pair;
import lynceus.PredictiveModel;
import lynceus.TestSet;
import lynceus.TrainingSet;
import lynceus.cherrypick.CherrypickConfig;
import lynceus.cherrypick.CherrypickConfigCostGenerator;
import lynceus.cherrypick.CherrypickLHS;
import lynceus.tensorflow.TensorflowConfig;
import weka.HackGaussianProcess;
import weka.WekaGaussianProcess;
import weka.WekaModelSample;
import weka.WekaSet;
import weka.tensorflow.WekaTensorflowConfigFactory;
import weka.tuning.ModelParams;

public class WekaCherrypickConfigLynceus extends Lynceus<CherrypickConfig, WekaModelSample> {
	
	private static boolean printed = false;
	
	public WekaCherrypickConfigLynceus(long h, double b, double epsilon, double gamma, optimizer opt, long wkldid, earlyStopOptions es) {
		super(h, b, epsilon, gamma, opt, wkldid, es);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected CostGenerator<CherrypickConfig> buildCostGenerator(long seed) {
		if (costGenerator == null) {
	         costGenerator = new CherrypickConfigCostGenerator();
		}
		return costGenerator;
	}

	@Override
	protected TestSet<CherrypickConfig, WekaModelSample> initialTestSet() {
		if (!printed) {
	         printAll();
	         printed = true;
	      }
		return WekaCherrypickConfigFactory.buildInitTestSet("files/cherrypick.arff");
	}

	@Override
	protected PredictiveModel<CherrypickConfig, WekaModelSample> buildPredictiveModel(
			TrainingSet<CherrypickConfig, WekaModelSample> trainingSet, ModelParams params) {
		return new WekaGaussianProcess<CherrypickConfig>((WekaSet) trainingSet,params);
	}

	@Override
	protected PredictiveModel<CherrypickConfig, WekaModelSample> buildPredictiveModelForApprox(
			TrainingSet<CherrypickConfig, WekaModelSample> trainingSet) {
		return new HackGaussianProcess<CherrypickConfig>((WekaSet) trainingSet);
	}

	@Override
	protected TrainingSet<CherrypickConfig, WekaModelSample> emptyTrainingSet() {
		final CherrypickConfigWekaTestSet ts = (CherrypickConfigWekaTestSet) this.testSet;
	    assert ts != null;
	    return new CherrypickConfigWekaTrainingSet(ts.arff());
	}

	@Override
	protected TrainingSet<CherrypickConfig, WekaModelSample> emptyTrainingSetForApprox() {
		if(true)throw  new RuntimeException("NO");
	      return new CherrypickConfigWekaTrainingSet("files/cherrypick_ei.arff");
	}

	@Override
	protected TestSet<CherrypickConfig, WekaModelSample> fullTestSet() {
		return WekaCherrypickConfigFactory.buildInitTestSet("files/cherrypick.arff");
	}

	@Override
	protected LHS<CherrypickConfig> instantiateLHS(int initTrainSamples) {
		return new CherrypickLHS(initTrainSamples);
	}

	private TestSet<CherrypickConfig, WekaModelSample> printAll() {
        
		TestSet<CherrypickConfig, WekaModelSample> testSet = WekaCherrypickConfigFactory.buildInitTestSet("files/cherrypick.arff");
        if (costGenerator == null) {
           throw new RuntimeException("[WekaCherrypickConfigLynceus] Cost generator is null");
        }
        System.out.println("[WekaCherrypickConfigLynceus] PRE  Total test set size = " + testSet.size());

        Set<Pair<CherrypickConfig, Double>> set = new HashSet<Pair<CherrypickConfig, Double>>();
        for (int i = 0; i < testSet.size(); i++) {
        	CherrypickConfig c = testSet.getConfig(i);
           double runningCost = costGenerator.deploymentCost(null, c);
           set.add(new Pair<CherrypickConfig, Double>(c, runningCost));
        }

        for (Pair<CherrypickConfig, Double> p : set) {
           testSet.removeConfig(p.getFst());
           testSet.addTestSampleWithTarget(p.getFst(), p.getSnd());
           //System.out.println("added " + p.fst + ", " + p.snd);
        }

        System.out.println("[WekaCherrypickConfigLynceus] POST Total test set size = " + testSet.size());
        testSet.printAll();
        return testSet;
   }
	
}
