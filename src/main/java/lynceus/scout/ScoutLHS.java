package lynceus.scout;

import java.util.ArrayList;
import java.util.List;

import lynceus.Configuration;
import lynceus.LHS;
import lynceus.aws.AWSDirectory;
import lynceus.aws.AWSDirectory.AWSInstanceType;
import weka.scout.WekaScoutVMConfig;


public class ScoutLHS<C extends Configuration> extends LHS{
	
	/* class attributes */
	private ArrayList<double[]> dims;
	
	
	/* class constructor */
	public ScoutLHS(int samples) {
		super(samples);
	}

	
	/* superclass abstract methods to be implemented */
	
	/**
	 * Initialise the dimension arrays
	 * @return a list with the initialised arrays
	 */
	@Override
	protected ArrayList<double[]> initDims() {
		this.dims = new ArrayList<double[]>();
		
		
		double[] type = {0, 1, 2};	// C4 = 0, M4 = 1, R4 = 2
		double[] size = {0, 1, 2};	// large = 0, xlarge = 1, 2xlarge = 2
		double[] num_instances = {4, 6, 8, 10, 12, 16, 20, 24, 32, 40, 48}; // xlarge only goes up to 24 and 2xlarge to 12
																			// only xlarge has 20 machines
		this.dims.add(num_instances);
		this.dims.add(size);
		this.dims.add(type);

		//System.out.println("dims: " + dims);
		return this.dims;
	}

	
	/**
	 * Retrieve a new initial sample with values corresponding to index 'index'
	 * of all dimension arrays
	 */
	@Override
	protected Configuration newSample(int index) {
		double[] dim = new double[11];
		int auxIndex = -1;	// auxiliar to maintain the initial value of index when 
							// we need to do round-robin
		
		double num_instances = 0;
	    AWSDirectory.AWSInstanceSize size = null;
		AWSDirectory.AWSInstanceType type = null;
	    double vcpus = 0;
	    double ecus;
	    double frequency;
	    double ram;
	    double ebs_band;
	    String net_band;
	    
		//System.out.println("index = " + index);
		
		/* for each dimension array of a configuration, pick the value
		 * corresponding to the given index. If the array is smaller, do it
		 * round-robin */
		int i = 0;
		while (i < this.dims.size()) {
			if (this.dims.get(i).length == 1) {
				auxIndex = 0;
			} else if (index >= this.dims.get(i).length) {
				auxIndex = index - this.dims.get(i).length;
				while (auxIndex >= this.dims.get(i).length) {
					auxIndex = auxIndex - this.dims.get(i).length;
				}
			} else {
				auxIndex = index;
			}
			
			dim = this.dims.get(i);
			
			if (i == 0) {	// selecting number of instances
				num_instances = dim[auxIndex];
			} else if (i == 1) { // selecting size
				if (num_instances == 20) {	// only size xlarge has 20 VMs
					size = AWSDirectory.AWSInstanceSize.xlarge;
					vcpus = 4;
				} else if (dim[auxIndex] == 0 || num_instances > 24) {
					size = AWSDirectory.AWSInstanceSize.large;
					vcpus = 2;
				} else if (dim[auxIndex] == 1 && num_instances <= 24) {
					size = AWSDirectory.AWSInstanceSize.xlarge;
					vcpus = 4;
				} else if (dim[auxIndex] == 2 && num_instances <= 12){
					size = AWSDirectory.AWSInstanceSize.x2large;
					vcpus = 8;
				} else if (dim[auxIndex] == 2 && num_instances > 12 && num_instances <= 24) {
					if(Math.random() < 0.5) {
					    size = AWSDirectory.AWSInstanceSize.large;
					    vcpus = 2;
					} else {
						size = AWSDirectory.AWSInstanceSize.xlarge;
						vcpus = 4;
					}
				}
			} else if (i == 2) {	// selecting type
				if (dim[auxIndex] == 0) {
					type = AWSDirectory.AWSInstanceType.C4;
				} else if (dim[auxIndex] == 1) {
					type = AWSDirectory.AWSInstanceType.M4;
				} else {
					type = AWSDirectory.AWSInstanceType.R4;
				}
			}
			i++;
		}
		
		ecus = AWSDirectory.ecusFor(size, type);
		frequency = AWSDirectory.cpuFrequencyFor(size, type);
		ram = AWSDirectory.ramFor(size, type);
		ebs_band = AWSDirectory.ebsBandwidthFor(size, type);
		net_band  =AWSDirectory.networkBandwidthFor(size, type);
		
		ScoutVMConfig newConfig = new ScoutVMConfig(type, size, vcpus, ecus, frequency, ram, ebs_band, net_band, num_instances);
		//System.out.println("newConfig = " + newConfig);
		
		return new WekaScoutVMConfig(newConfig, super.dataset);
	}


	@Override
	protected void checkSamples(List samples, String type, int seed) {
		// TODO Auto-generated method stub
		
	}


	@Override
	protected ArrayList<Double> dimensions(Configuration config) {
		ArrayList<Double> dimensionsArray = new ArrayList<Double>();
		
	    double vcpus = (double) config.at(0);
	    double ecus = (double) config.at(1);
	    double frequency = (double) config.at(2);
	    double ram = (double) config.at(3);
	    double ebs_band = (double) config.at(4);
	    String net_band = (String) config.at(5);
	    double num_instances = (double) config.at(6);
		AWSDirectory.AWSInstanceType type = (AWSInstanceType) config.at(7);
		
		switch ((int) vcpus) {
			case 2:
				dimensionsArray.add(1.0);
				break;
			case 4:
				dimensionsArray.add(2.0);
				break;
			case 8:
				dimensionsArray.add(3.0);
				break;
		}
		
		if (ecus == 8) {
			dimensionsArray.add(1.0);
		} else if (ecus == 16) {
			dimensionsArray.add(2.0);
		} else if (ecus == 31) {
			dimensionsArray.add(3.0);
		} else if (ecus == 6.5) {
			dimensionsArray.add(4.0);
		} else if (ecus == 13) {
			dimensionsArray.add(5.0);
		} else if (ecus == 26) {
			dimensionsArray.add(6.0);
		} else if (ecus == 7) {
			dimensionsArray.add(7.0);
		} else if (ecus == 13.5) {
			dimensionsArray.add(8.0);
		} else { // (ecus == 27) {
			dimensionsArray.add(9.0);
		}
		
		if (ram == 3.75) {
			dimensionsArray.add(1.0);
		} else if (ram == 7.5) {
			dimensionsArray.add(2.0);
		} else if (ram == 15) {
			dimensionsArray.add(3.0);
		} else if (ram == 8) {
			dimensionsArray.add(4.0);
		} else if (ram == 16) {
			dimensionsArray.add(5.0);
		} else if (ram == 32) {
			dimensionsArray.add(6.0);
		} else if (ram == 15.25) {
			dimensionsArray.add(7.0);
		} else if (ram == 30.5) {
			dimensionsArray.add(8.0);
		} else { // (ram == 61) {
			dimensionsArray.add(9.0);
		}

		if (ebs_band == 425) {
			dimensionsArray.add(1.0);
		} else if (ebs_band == 450) {
			dimensionsArray.add(2.0);
		} else if (ebs_band == 500) {
			dimensionsArray.add(3.0);
		} else if (ebs_band == 750) {
			dimensionsArray.add(4.0);
		} else if (ebs_band == 850) {
			dimensionsArray.add(5.0);
		} else if (ebs_band == 1000) {
			dimensionsArray.add(6.0);
		} else {// (ebs_band == 1700) {
			dimensionsArray.add(7.0);
		}
		
		if (net_band.compareTo("MODERATE") == 0) {
			dimensionsArray.add(1.0);
		} else if (net_band.compareTo("HIGH") == 0) {
			dimensionsArray.add(2.0);
		} else {	// "UP_TO_10"
			dimensionsArray.add(3.0);
		}
		
		// {4, 6, 8, 10, 12, 16, 20, 24, 32, 40, 48}
		switch ((int) num_instances) {
			case 4:
				dimensionsArray.add(1.0);
				break;
			case 6:
				dimensionsArray.add(2.0);
				break;
			case 8:
				dimensionsArray.add(3.0);
				break;
			case 10:
				dimensionsArray.add(4.0);
				break;
			case 12:
				dimensionsArray.add(5.0);
				break;
			case 16:
				dimensionsArray.add(6.0);
				break;
			case 20:
				dimensionsArray.add(7.0);
				break;
			case 24:
				dimensionsArray.add(8.0);
				break;
			case 32:
				dimensionsArray.add(9.0);
				break;
			case 40:
				dimensionsArray.add(10.0);
				break;
			case 48:
				dimensionsArray.add(11.0);
				break;
		}
		
		if (type == AWSDirectory.AWSInstanceType.C4) {
			dimensionsArray.add(1.0);
		} else if (type == AWSDirectory.AWSInstanceType.R4) {
			dimensionsArray.add(2.0);
		} else {
			dimensionsArray.add(3.0);
		}
		
		return dimensionsArray;
	}

}
