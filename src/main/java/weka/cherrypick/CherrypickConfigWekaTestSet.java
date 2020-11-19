package weka.cherrypick;

import lynceus.TestSet;
import lynceus.WekaConfiguration;
import lynceus.cherrypick.CherrypickConfig;
import weka.AbstractConfigWekaTestSet;
import weka.WekaModelSample;
import weka.core.Instance;
import weka.core.Instances;

public class CherrypickConfigWekaTestSet extends AbstractConfigWekaTestSet<CherrypickConfig, WekaModelSample> {
	
	/* class constructors */
	public CherrypickConfigWekaTestSet(String arff) {
		super(arff);
	}

	public CherrypickConfigWekaTestSet(Instances set, String arff) {
		super(set, arff);
	}

	@Override
	public TestSet<CherrypickConfig, WekaModelSample> clone() {
		return new CherrypickConfigWekaTestSet(new Instances(this.set), this.arff);
	}

	@Override
	protected WekaConfiguration buildFromConfigAndSet(CherrypickConfig c, Instances i) {
		return new WekaCherrypickConfig(c, i);
	}

	@Override
   public CherrypickConfig buildFromInstance(Instance i) {
		return new WekaCherrypickConfig(i);
	}

}
