package lynceus.cherrypick;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import lynceus.Configuration;
import lynceus.LHS;
import lynceus.aws.AWSDirectory;
import lynceus.aws.AWSDirectory.AWSInstanceSpeed;
import lynceus.aws.AWSDirectory.AWSInstanceType;
import lynceus.aws.DiskDirectory;
import lynceus.aws.DiskDirectory.DiskSpeed;
import lynceus.aws.DiskDirectory.DiskType;
import weka.cherrypick.WekaCherrypickConfig;

public class CherrypickLHS<C extends Configuration> extends LHS {

	/* class attributes */
	private ArrayList<double[]> dims;
	private static int targetWkld;
	
	/* class constructor */
	public CherrypickLHS(int samples) {
		super(samples);
	}

	/* setters */
	public static void setTargetWkld(int wkld) {
		targetWkld = wkld;
	}
	
	/* superclass abstract methods to be implemented */
	
	/**
	 * Initialise the dimension arrays
	 * @return a list with the initialised arrays
	 */
	@Override
	protected ArrayList initDims() {
		this.dims = new ArrayList<double[]>();
		
		double[] vm_type = {0, 1, 2, 3};	// 0 = C4 ; 1 = M4 ; 2 = R3 ; 3 = I2
		double[] vcpus = {2, 4, 8};
		double[] disk_count = {2, 3};
		
		this.dims.add(vm_type);
		this.dims.add(vcpus);
		this.dims.add(disk_count);
		
		return this.dims;
	}

	/**
	 * Retrieve a new initial sample with values corresponding to index 'index'
	 * of all dimension arrays
	 */
	@Override
	protected Configuration newSample(int index) {
		double[] dim = new double[15];
		int auxIndex = -1;	// auxiliar to maintain the initial value of index when 
							// we need to do round-robin
		
		/* attributes of a 'cherrypick' configuration */
		AWSDirectory.AWSInstanceType vm_type = null;
		AWSDirectory.AWSInstanceSize vm_size = null;
		AWSDirectory.AWSInstanceSpeed vm_speed = null;
		double vcpus = 0;
		double ram = 0;
		int num_instances = 0;
		DiskDirectory.DiskType disk_type = null;
		DiskDirectory.DiskSpeed disk_speed = null;
		int disk_count = -1;
		
		/* for each dimension array of a configuration, pick the value
		 * corresponding to the given index. If the array is smaller, do it
		 * round-robin */
		int i = 0;
		while (i < this.dims.size()) {
			
			/* current dimension */
			dim = this.dims.get(i);
			
			if (dim.length == 1) {
				auxIndex = 0;
			} else if (index >= dim.length) {
				auxIndex = index - dim.length;
				while (auxIndex >= dim.length) {
					auxIndex = auxIndex - dim.length;
				}
			} else {
				auxIndex = index;
			}
			
			
			if (i == 0) {	// selecting vm_type
				if (dim[auxIndex] == 0) {
					vm_type = AWSInstanceType.C4;
				} else if (dim[auxIndex] == 1) {
					vm_type = AWSInstanceType.M4;
				} else if (dim[auxIndex] == 2) {
					vm_type = AWSInstanceType.R3;
				} else {
					vm_type = AWSInstanceType.I2;
				}
			} else if (i == 1) {	// selecting vcpus
				if (vm_type == AWSInstanceType.I2) {
					Random r = new Random(super.seed);
					if(r.nextBoolean()) {
						vcpus = 4;
					} else {
						vcpus = 8;
					}
				} else {
					vcpus = (int) dim[auxIndex];
				}
			} else {	// selecting disk count
						// dataset 1 only has configs with 2 disks
						// only VMs of type I2.2xlarge have 3 disks, no matter the dataset
				if (targetWkld != 1 && vm_type == AWSInstanceType.I2 && vcpus == 8) {
					disk_count = 3;
				} else {
					disk_count = 2;
				}
			}
			i++;
		}
		
		vm_size = AWSDirectory.sizeFromTypeAndCPU(vm_type, vcpus);
		vm_speed = AWSDirectory.speedForType(vm_type);
		ram = AWSDirectory.ramFor(vm_size, vm_type);
		num_instances = numInstancesFor(vm_type, vm_size, index);
		disk_type = DiskDirectory.diskTypeForVMType(vm_type);
		disk_speed = DiskDirectory.diskSpeedForDiskType(disk_type);
		
		CherrypickConfig newConfig = new CherrypickConfig(vm_type, vm_size, vm_speed, vcpus, ram, num_instances, disk_type, disk_speed, disk_count);
		
		return new WekaCherrypickConfig(newConfig, super.dataset);
	}

	@Override
	protected ArrayList dimensions(Configuration config) {
		ArrayList<Double> dimensionsArray = new ArrayList<Double>();
		
		AWSDirectory.AWSInstanceType vm_type = (AWSInstanceType) config.at(0);
		AWSDirectory.AWSInstanceSpeed vm_speed = (AWSInstanceSpeed) config.at(1);
		double vcpus = (double) config.at(2);
		double ram = (double) config.at(3);
		int num_instances = (int) config.at(4);
		DiskDirectory.DiskType disk_type = (DiskType) config.at(5);
		DiskDirectory.DiskSpeed disk_speed = (DiskSpeed) config.at(6);
		int disk_count = (int) config.at(7);
		
		if (vm_type == AWSInstanceType.C4) {
			dimensionsArray.add(1.0);
		} else if (vm_type == AWSInstanceType.M4) {
			dimensionsArray.add(2.0);
		} else if (vm_type == AWSInstanceType.R3) {
			dimensionsArray.add(3.0);
		} else {
			dimensionsArray.add(4.0);
		}
		
		if (vm_speed == AWSInstanceSpeed.slow) {
			if (vcpus == 2) {	// ==> size == large
				dimensionsArray.add(1.0);	// because of size
				dimensionsArray.add(1.0);	// becausee of speed
				dimensionsArray.add(1.0);	// because of vcpus
			} else if (vcpus == 4) {	// ==> size = xlarge
				dimensionsArray.add(2.0);	// because of size
				dimensionsArray.add(1.0);	// becausee of speed
				dimensionsArray.add(2.0);	// because of vcpus
			} else {	// ==> size = 2xlarge
				dimensionsArray.add(3.0);	// because of size
				dimensionsArray.add(1.0);	// becausee of speed
				dimensionsArray.add(3.0);	// because of vcpus
			}
		} else {
			if (vcpus == 2) {	// ==> size == large
				dimensionsArray.add(1.0);	// because of size
				dimensionsArray.add(2.0);	// becausee of speed
				dimensionsArray.add(1.0);	// because of vcpus
			} else if (vcpus == 4) {	// ==> size = xlarge
				dimensionsArray.add(2.0);	// because of size
				dimensionsArray.add(2.0);	// becausee of speed
				dimensionsArray.add(2.0);	// because of vcpus
			} else {	// ==> size = 2xlarge
				dimensionsArray.add(3.0);	// because of size
				dimensionsArray.add(2.0);	// becausee of speed
				dimensionsArray.add(3.0);	// because of vcpus
			}
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
		
		if (num_instances == 2) {
			dimensionsArray.add(1.0);
		} else if (num_instances == 4) {
			dimensionsArray.add(2.0);
		} else if (num_instances == 6) {
			dimensionsArray.add(3.0);
		} else if (num_instances == 8) {
			dimensionsArray.add(4.0);
		} else if (num_instances == 10) {
			dimensionsArray.add(5.0);
		} else if (num_instances == 12) {
			dimensionsArray.add(6.0);
		} else if (num_instances == 14) {
			dimensionsArray.add(7.0);
		} else if (num_instances == 16) {
			dimensionsArray.add(8.0);
		} else if (num_instances == 20) {
			dimensionsArray.add(9.0);
		} else if (num_instances == 24) {
			dimensionsArray.add(10.0);
		} else if (num_instances == 28) {
			dimensionsArray.add(11.0);
		} else if (num_instances == 32) {
			dimensionsArray.add(12.0);
		} else if (num_instances == 40) {
			dimensionsArray.add(13.0);
		} else if (num_instances == 48) {
			dimensionsArray.add(14.0);
		} else {
			dimensionsArray.add(15.0);
		}
		
		if (disk_type == DiskType.ebs) {
			dimensionsArray.add(1.0);
		} else {	// local
			dimensionsArray.add(2.0);
		}
		
		if (disk_speed == DiskSpeed.slow) {
			dimensionsArray.add(1.0);
		} else {	// fast
			dimensionsArray.add(2.0);
		}
		
		if (disk_count == 2) {
			dimensionsArray.add(1.0);
		} else {
			dimensionsArray.add(2.0);
		}
		
		return dimensionsArray;
	}

	@Override
	protected void checkSamples(List samples, String type, int seed) {
		// TODO Auto-generated method stub
		
	}
	
	private int numInstancesFor(AWSDirectory.AWSInstanceType vmType, AWSDirectory.AWSInstanceSize vmSize, int index) {
		int[] numPossibleInstances = null;
		int auxIndex = 0;
				
		switch(targetWkld) {
			case 1:
				switch(vmType) {
					case C4:
						switch(vmSize) {
							case x2large:
								numPossibleInstances = new int[] {4, 6, 8, 10, 14};
								break;
							case xlarge:
								numPossibleInstances = new int[] {8, 12, 16, 20, 28};
								break;
							case large:
								numPossibleInstances = new int[] {16, 24};
								break;
						}
						break;
					case M4:
						switch(vmSize) {
							case x2large:
								numPossibleInstances = new int[] {4, 6, 10, 14};
								break;
							case xlarge:
								numPossibleInstances = new int[] {8, 12, 20, 28};
								break;
							case large:
								numPossibleInstances = new int[] {16, 24, 40, 56};
								break;
						}
						break;
					case R3:
						switch(vmSize) {
							case x2large:
								numPossibleInstances = new int[] {4, 6, 8, 10, 14};
								break;
							case xlarge:
								numPossibleInstances = new int[] {8, 12, 16, 20, 28};
								break;
							case large:
								numPossibleInstances = new int[] {16, 32, 40, 56};
								break;
						}
						break;
					case I2:
						switch(vmSize) {
							case x2large:
								numPossibleInstances = new int[] {4, 6, 8, 14};
								break;
							case xlarge:
								numPossibleInstances = new int[] {8, 12, 16, 20, 28};
								break;
						}
						break;
				}
				break;
			case 2:
				switch(vmType) {
					case C4:
						switch(vmSize) {
							case x2large:
								numPossibleInstances = new int[] {2, 4, 6, 8, 14};
								break;
							case xlarge:
								numPossibleInstances = new int[] {4, 8, 16, 20, 28};
								break;
							case large:
								numPossibleInstances = new int[] {8, 16, 32};
								break;
						}
						break;
					case M4:
						switch(vmSize) {
							case x2large:
								numPossibleInstances = new int[] {2, 4, 6, 10, 14};
								break;
							case xlarge:
								numPossibleInstances = new int[] {4, 8, 12, 16, 20, 28};
								break;
							case large:
								numPossibleInstances = new int[] {8, 16, 24, 32, 40, 56};
								break;
						}
						break;
					case R3:
						switch(vmSize) {
							case x2large:
								numPossibleInstances = new int[] {2, 4, 6, 8, 10, 14};
								break;
							case xlarge:
								numPossibleInstances = new int[] {4, 8, 12, 16, 20, 28};
								break;
							case large:
								numPossibleInstances = new int[] {8, 16, 32, 40, 56};
								break;
						}
						break;
					case I2:
						switch(vmSize) {
							case x2large:
								numPossibleInstances = new int[] {2, 4, 6, 8, 10, 14};
								break;
							case xlarge:
								numPossibleInstances = new int[] {4, 8, 12, 16, 20, 28};
								break;
						}
						break;
				}
				break;
			case 3:
				switch(vmType) {
					case C4:
						switch(vmSize) {
							case x2large:
								numPossibleInstances = new int[] {4, 6, 8, 10, 12, 14};
								break;
							case xlarge:
								numPossibleInstances = new int[] {4, 8, 16, 20, 24, 28};
								break;
							case large:
								numPossibleInstances = new int[] {8, 16, 24, 32, 40, 48, 56};
								break;
						}
						break;
					case M4:
						switch(vmSize) {
							case x2large:
								numPossibleInstances = new int[] {4, 6, 8, 10, 12, 14};
								break;
							case xlarge:
								numPossibleInstances = new int[] {4, 8, 12, 16, 20, 24, 28};
								break;
							case large:
								numPossibleInstances = new int[] {8, 16, 24, 32, 40, 48, 56};
								break;
						}
						break;
					case R3:
						switch(vmSize) {
							case x2large:
								numPossibleInstances = new int[] {4, 6, 8, 10, 12, 14};
								break;
							case xlarge:
								numPossibleInstances = new int[] {4, 8, 12, 16, 20, 24, 28};
								break;
							case large:
								numPossibleInstances = new int[] {8, 16, 24, 32, 40, 48, 56};
								break;
						}
						break;
					case I2:
						switch(vmSize) {
							case x2large:
								numPossibleInstances = new int[] {2, 4, 6, 8, 10, 12, 14};
								break;
							case xlarge:
								numPossibleInstances = new int[] {4, 8, 12, 16, 20, 28};
								break;
						}
						break;
				}
				break;
			case 4:
				switch(vmType) {
					case C4:
						switch(vmSize) {
							case x2large:
								numPossibleInstances = new int[] {2, 4, 6, 8, 10};
								break;
							case xlarge:
								numPossibleInstances = new int[] {4, 8, 12, 20};
								break;
							case large:
								numPossibleInstances = new int[] {8, 16, 24, 32};
								break;
						}
						break;
					case M4:
						switch(vmSize) {
							case x2large:
								numPossibleInstances = new int[] {2, 4, 6, 8, 10, 14};
								break;
							case xlarge:
								numPossibleInstances = new int[] {4, 8, 12, 16, 20, 28};
								break;
							case large:
								numPossibleInstances = new int[] {8, 16, 24, 32, 40, 56};
								break;
						}
						break;
					case R3:
						switch(vmSize) {
							case x2large:
								numPossibleInstances = new int[] {2, 4, 6, 8, 10, 14};
								break;
							case xlarge:
								numPossibleInstances = new int[] {4, 8, 12, 16, 20, 28};
								break;
							case large:
								numPossibleInstances = new int[] {8, 16, 24, 32, 40, 56};
								break;
						}
						break;
					case I2:
						switch(vmSize) {
							case x2large:
								numPossibleInstances = new int[] {2, 4, 6, 8, 10, 14};
								break;
							case xlarge:
								numPossibleInstances = new int[] {4, 8, 12, 16, 20, 28};
								break;
						}
						break;
				}
				break;
			case 5:
				switch(vmType) {
					case C4:
						switch(vmSize) {
							case x2large:
								numPossibleInstances = new int[] {2, 4, 6, 8, 10, 14};
								break;
							case xlarge:
								numPossibleInstances = new int[] {4, 8, 12, 16, 20, 28};
								break;
							case large:
								numPossibleInstances = new int[] {8, 16, 24};
								break;
						}
						break;
					case M4:
						switch(vmSize) {
							case x2large:
								numPossibleInstances = new int[] {2, 4, 6, 8, 10};
								break;
							case xlarge:
								numPossibleInstances = new int[] {4, 8, 12, 16, 20};
								break;
							case large:
								numPossibleInstances = new int[] {8, 16};
								break;
						}
						break;
					case R3:
						switch(vmSize) {
							case x2large:
								numPossibleInstances = new int[] {2, 4, 6, 8, 10};
								break;
							case xlarge:
								numPossibleInstances = new int[] {4, 8, 12, 16, 20};
								break;
							case large:
								numPossibleInstances = new int[] {8, 16, 24, 32, 40};
								break;
						}
						break;
					case I2:
						switch(vmSize) {
							case x2large:
								numPossibleInstances = new int[] {2, 4, 6, 8, 10};
								break;
							case xlarge:
								numPossibleInstances = new int[] {4, 8, 12, 16, 20};
								break;
						}
						break;
				}
				break;
			default:
				throw new RuntimeException("inexistent wkld " + targetWkld);
		}
		
		if (numPossibleInstances.length == 1) {
			auxIndex = 0;
		} else if (index >= numPossibleInstances.length) {
			auxIndex = index - numPossibleInstances.length;
			while (auxIndex >= numPossibleInstances.length) {
				auxIndex = auxIndex - numPossibleInstances.length;
			}
		} else {
			auxIndex = index;
		}
		
		return numPossibleInstances[auxIndex];
	}

	

	
	private double[] numInstancesForWkld () {
		
		switch(targetWkld) {
			case 1:
				double[] numInstances = {4, 6, 8, 10, 12, 14, 16, 20, 24, 28, 32, 40, 56};
				return numInstances;
			case 2:
				double[] numInstances1 = {2, 4, 6, 8, 10, 12, 14, 16, 20, 24, 28, 32, 40, 56};
				return numInstances1;
			case 3:
				double[] numInstances2 = {2, 4, 6, 8, 10, 12, 14, 16, 20, 24, 28, 32, 40, 48, 56};
				return numInstances2;
			case 4:
				double[] numInstances3 = {2, 4, 6, 8, 10, 12, 14, 16, 20, 24, 28, 32, 40, 56};
				return numInstances3;
			case 5:
				double[] numInstances4 = {2, 4, 6, 8, 10, 12, 14, 16, 20, 24, 28, 32, 40};
				return numInstances4;
			default:
				throw new RuntimeException("inexistent wkld " + targetWkld);
		}
	}
	
}
