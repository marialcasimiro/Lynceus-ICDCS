package weka.tensorflow;

import lynceus.TrainingSet;
import lynceus.tensorflow.TensorflowConfig;
import weka.AbstractConfigWekaTrainingSet;
import weka.WekaModelSample;
import weka.core.Instance;
import weka.core.Instances;

public class TensorflowConfigWekaTrainingSet extends AbstractConfigWekaTrainingSet<TensorflowConfig, WekaModelSample>{

	/* class constructors */
	TensorflowConfigWekaTrainingSet(String arff){
		super(arff);
	}
	
	private TensorflowConfigWekaTrainingSet(Instances i) {
		super(i);
	}

	/* superclass abstract methods to be implemented */
	@Override
	public TrainingSet<TensorflowConfig, WekaModelSample> clone() {
		return new TensorflowConfigWekaTrainingSet(this.set);
	}

	@Override
	protected TensorflowConfig buildConfigFromInstance(Instance i) {
		return new WekaTensorflowConfig(i);
	}
}
