package weka.tensorflow;

import lynceus.TestSet;
import lynceus.WekaConfiguration;
import lynceus.tensorflow.TensorflowConfig;
import weka.AbstractConfigWekaTestSet;
import weka.WekaModelSample;
import weka.core.Instance;
import weka.core.Instances;

public class TensorflowConfigWekaTestSet extends AbstractConfigWekaTestSet<TensorflowConfig, WekaModelSample>{

	/* class constructors */
	public TensorflowConfigWekaTestSet(Instances set, String arff) {
		super(set, arff);
	}

	public TensorflowConfigWekaTestSet(String arff){
		super(arff);
	}
	
	/* superclass abstract methods to be implemented */
	@Override
	public TestSet<TensorflowConfig, WekaModelSample> clone() {
		return new TensorflowConfigWekaTestSet(new Instances(this.set), this.arff);
	}

	@Override
	protected WekaConfiguration buildFromConfigAndSet(TensorflowConfig c, Instances i) {
		return new WekaTensorflowConfig(c, i);
	}

	@Override
   public WekaTensorflowConfig buildFromInstance(Instance i) {
		return new WekaTensorflowConfig(i);
	}
	
}
