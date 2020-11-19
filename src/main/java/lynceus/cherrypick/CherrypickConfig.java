package lynceus.cherrypick;

import static lynceus.aws.AWSDirectory.cpuFrequencyFor;
import static lynceus.aws.AWSDirectory.cpusFor;
import static lynceus.aws.AWSDirectory.ebsBandwidthFor;
import static lynceus.aws.AWSDirectory.ecusFor;
import static lynceus.aws.AWSDirectory.networkBandwidthFor;
import static lynceus.aws.AWSDirectory.ramFor;

import org.apache.commons.csv.CSVRecord;

import lynceus.Configuration;
import lynceus.aws.AWSDirectory;
import lynceus.aws.DiskDirectory;


public class CherrypickConfig implements Configuration {

	/* class attributes */
	private AWSDirectory.AWSInstanceType vm_type;
	private AWSDirectory.AWSInstanceSize vm_size;
	private AWSDirectory.AWSInstanceSpeed vm_speed;
	private double vcpus;
	private double ram;
	private int num_instances;
	private DiskDirectory.DiskType disk_type;
	private DiskDirectory.DiskSpeed disk_speed;
	private int disk_count;
	// private double disk_cost;	// I think this should not be part of the configuration
	// private double disk_size;	// it's always 500.0
	
	/* class constructors */
	public CherrypickConfig (AWSDirectory.AWSInstanceType vm_type, AWSDirectory.AWSInstanceSize vm_size,
				AWSDirectory.AWSInstanceSpeed vm_speed, double vcpus, double ram, int num_instances, DiskDirectory.DiskType disk_type,
				DiskDirectory.DiskSpeed disk_speed, int disk_count) {
		this.vm_type = vm_type;
		this.vm_size = vm_size;
		this.vm_speed = vm_speed;
		this.vcpus = vcpus;
		this.ram = ram;
		this.num_instances = num_instances;
		this.disk_type = disk_type;
		this.disk_speed = disk_speed;
		this.disk_count = disk_count;
	}

	/* implement interface methods */
	@Override
	public int numAttributes() {
		return 8;
	}

	@Override
	public Object at(int i) {
		switch (i) {
	        case 0:
	           return vm_type;
	        case 1:
	           return vm_speed;
	        case 2:
	           return vcpus;
	        case 3:
	           return ram;
	        case 4:
	           return num_instances;
	        case 5:
	           return disk_type;
	        case 6:
	       	 	return disk_speed;
	        case 7:
	        	return disk_count;
	        default:
	        	throw new RuntimeException("Attribute " + i + " not defined for " + this.getClass());
		}
	}

	@Override
	public Configuration clone() {
		return new CherrypickConfig (this.vm_type, this.vm_size, this.vm_speed, this.vcpus, this.ram, this.num_instances, this.disk_type, this.disk_speed, this.disk_count);
	}
	
	/* getters */
	public AWSDirectory.AWSInstanceType getVm_type() {
		return vm_type;
	}

	public AWSDirectory.AWSInstanceSize getVm_size() {
		return vm_size;
	}

	public AWSDirectory.AWSInstanceSpeed getVm_speed() {
		return vm_speed;
	}

	public double getVcpus() {
		return vcpus;
	}

	public double getRam() {
		return ram;
	}

	public int getNum_instances() {
		return num_instances;
	}

	public DiskDirectory.DiskType getDisk_type() {
		return disk_type;
	}

	public DiskDirectory.DiskSpeed getDisk_speed() {
		return disk_speed;
	}

	public int getDisk_count() {
		return disk_count;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + disk_count;
		result = prime * result + ((disk_speed == null) ? 0 : disk_speed.hashCode());
		result = prime * result + ((disk_type == null) ? 0 : disk_type.hashCode());
		long temp;
		temp = Double.doubleToLongBits(num_instances);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(ram);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(vcpus);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + ((vm_size == null) ? 0 : vm_size.hashCode());
		result = prime * result + ((vm_speed == null) ? 0 : vm_speed.hashCode());
		result = prime * result + ((vm_type == null) ? 0 : vm_type.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
//		if (getClass() != obj.getClass())
//			return false;
		CherrypickConfig other = (CherrypickConfig) obj;
		if (disk_count != other.disk_count)
			return false;
		if (disk_speed != other.disk_speed)
			return false;
		if (disk_type != other.disk_type)
			return false;
		if (Double.doubleToLongBits(num_instances) != Double.doubleToLongBits(other.num_instances))
			return false;
		if (Double.doubleToLongBits(ram) != Double.doubleToLongBits(other.ram))
			return false;
		if (Double.doubleToLongBits(vcpus) != Double.doubleToLongBits(other.vcpus))
			return false;
		if (vm_size != other.vm_size)
			return false;
		if (vm_speed != other.vm_speed)
			return false;
		if (vm_type != other.vm_type)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "CherrypickConfig [vm_type=" + vm_type + ", vm_size=" + vm_size + ", vm_speed=" + vm_speed + ", vcpus="
				+ vcpus + ", ram=" + ram + ", num_instances=" + num_instances + ", disk_type=" + disk_type
				+ ", disk_speed=" + disk_speed + ", disk_count=" + disk_count + "]";
	}
	
	public static CherrypickConfig parse(CSVRecord csvRecord) {
				
        String vm_name = csvRecord.get(14);
		int disk_count = Integer.parseInt(csvRecord.get(9));
        int numInstances = Integer.parseInt(csvRecord.get(3));
        
		AWSDirectory.AWSInstanceSize vmSize;
        AWSDirectory.AWSInstanceType vmType;
                        
        if (vm_name.contains("2xlarge")) {
        	vmSize = AWSDirectory.AWSInstanceSize.x2large;
        } else if (vm_name.contains("xlarge")) {
        	vmSize = AWSDirectory.AWSInstanceSize.xlarge;
        } else {
        	vmSize = AWSDirectory.AWSInstanceSize.large;
        }
        
        if (vm_name.contains("c4")) {
        	vmType = AWSDirectory.AWSInstanceType.C4;
        } else if (vm_name.contains("m4")) {
        	vmType = AWSDirectory.AWSInstanceType.M4;
        } else if (vm_name.contains("r3")) {
        	vmType = AWSDirectory.AWSInstanceType.R3;
        } else {
        	vmType = AWSDirectory.AWSInstanceType.I2;
        }

        AWSDirectory.AWSInstanceSpeed speed = AWSDirectory.speedForType(vmType);
        double ram = ramFor(vmSize, vmType);
        double cpus = cpusFor(vmSize, vmType);
        DiskDirectory.DiskType disk_type = DiskDirectory.diskTypeForVMType(vmType);
    	DiskDirectory.DiskSpeed disk_speed = DiskDirectory.diskSpeedForDiskType(disk_type);
    	

        return new CherrypickConfig(vmType, vmSize, speed, cpus, ram, numInstances, disk_type, disk_speed, disk_count);
  }

	
}
