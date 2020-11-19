package weka.cherrypick;

import lynceus.TrainingSet;
import lynceus.cherrypick.CherrypickConfig;
import weka.AbstractConfigWekaTrainingSet;
import weka.WekaModelSample;
import weka.core.Instance;
import weka.core.Instances;

public class CherrypickConfigWekaTrainingSet extends AbstractConfigWekaTrainingSet<CherrypickConfig,  WekaModelSample> {
	
	/* class constructors */
	protected CherrypickConfigWekaTrainingSet(String arff) {
		super(arff);
		// TODO Auto-generated constructor stub
	}
	
	protected CherrypickConfigWekaTrainingSet(Instances i) {
		super(i);
		// TODO Auto-generated constructor stub
	}

	@Override
	public TrainingSet<CherrypickConfig, WekaModelSample> clone() {
		return new CherrypickConfigWekaTrainingSet(this.set);
	}

	@Override
	protected CherrypickConfig buildConfigFromInstance(Instance i) {
		return new WekaCherrypickConfig(i);
	}

}
