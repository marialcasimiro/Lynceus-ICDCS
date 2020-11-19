package weka.cherrypick;

import java.util.ArrayList;

import lynceus.Pair;
import lynceus.WekaConfiguration;
import lynceus.aws.AWSDirectory;
import lynceus.aws.AWSDirectory.AWSInstanceSize;
import lynceus.aws.AWSDirectory.AWSInstanceSpeed;
import lynceus.aws.AWSDirectory.AWSInstanceType;
import lynceus.aws.DiskDirectory.DiskSpeed;
import lynceus.aws.DiskDirectory.DiskType;
import lynceus.cherrypick.CherrypickConfig;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

public class WekaCherrypickConfig extends CherrypickConfig implements WekaConfiguration {

	/* class attributes */
	private Instances dataset;
	
	/* class constructors */
	public WekaCherrypickConfig (AWSInstanceType vm_type, AWSInstanceSize vm_size, AWSInstanceSpeed vm_speed,
			double vcpus, double ram, int num_instances, DiskType disk_type, DiskSpeed disk_speed, int disk_count, Instances d) {
		super(vm_type, vm_size, vm_speed, vcpus, ram, num_instances, disk_type, disk_speed, disk_count);
		dataset = d;
		if (this.dataset == null)
	    	throw new RuntimeException("[WekaTensorflowConfig] Cannot be null");
	}
	
	public WekaCherrypickConfig (Instance i) {
		super(AWSDirectory.AWSInstanceType.valueOf(i.stringValue(0)), AWSDirectory.sizeFromTypeAndCPU(i.stringValue(0), i.value(2)), AWSDirectory.AWSInstanceSpeed.valueOf(i.stringValue(1)),
				(double) i.value(2), (double)i.value(3), (int)i.value(4), DiskType.valueOf(i.stringValue(5)), DiskSpeed.valueOf(i.stringValue(6)), (int)i.value(7));
		this.dataset = i.dataset();
	    if (this.dataset == null)
	    	throw new RuntimeException("[WekaTensorflowConfig] Cannot be null");
	}
	
	public WekaCherrypickConfig (CherrypickConfig c, Instances dataset) {
		super(c.getVm_type(), c.getVm_size(), c.getVm_speed(), c.getVcpus(), c.getRam(), c.getNum_instances(), c.getDisk_type(), c.getDisk_speed(), c.getDisk_count());
		this.dataset = dataset;
		if (this.dataset == null)
	    	throw new RuntimeException("[WekaTensorflowConfig] Cannot be null");
	}
	
	@Override
	public Instance toInstance() {
		Instance rr = new DenseInstance(numAttributes() + 1);  // + 1 for the target attribute
        rr.setDataset(this.dataset); //first set dataset, otherwise following statements fail with "no dataset associated" exception
        rr.setValue(0, this.getVm_type().toString());
        rr.setValue(1, this.getVm_speed().toString());
        rr.setValue(2, this.getVcpus());
        rr.setValue(3, this.getRam());
        rr.setValue(4, this.getNum_instances());
        rr.setValue(5, this.getDisk_type().toString());
        rr.setValue(6, this.getDisk_speed().toString());
        rr.setValue(7, this.getDisk_count());
        
        return rr;
	}

	@Override
    public String toString() {
		return super.toString();
    }
	
	@Override
	public ArrayList<WekaConfiguration> neighbourhood() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean findPair(ArrayList<Pair> searchSpace, Pair pair) {
		// TODO Auto-generated method stub
		return false;
	}

}
