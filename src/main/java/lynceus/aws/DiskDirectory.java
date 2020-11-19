package lynceus.aws;

import lynceus.aws.AWSDirectory.AWSInstanceType;

public class DiskDirectory {
	
	public enum DiskType {
		ebs(0), local(1);
		private int id;

		DiskType(int i) {
			id = i;
		}
	}
	
	public enum DiskSpeed {
		slow(0), fast(1);
		private int id;
		
		DiskSpeed(int i) {
			id = i;
		}
	}
	
	public static DiskType diskTypeForVMType (AWSDirectory.AWSInstanceType vmType) {
		if (vmType == AWSInstanceType.I2) {
			return DiskType.local;
		} else {
			return DiskType.ebs;
		}
	}
	
	public static DiskType diskTypeForDiskSpeed (DiskSpeed diskSpeed) {
		switch (diskSpeed) {
			case slow:
				return DiskType.ebs;
			case fast:
				return DiskType.local;
			default:
				 throw new RuntimeException("Cannot determine disk type for " + diskSpeed + " disk speed");
		}
	}
	
	public static DiskSpeed diskSpeedForDiskType (DiskType diskType) {
		switch (diskType) {
			case ebs:
				return DiskSpeed.slow;
			case local:
				return DiskSpeed.fast;
			default:
				throw new RuntimeException("Cannot determine disk speed for " + diskType + " disk type");
		}
	}
	
	public static double diskCostForDiskType (DiskType diskType) {
		switch (diskType) {
		case ebs:
			return 0.12;
		case local:
			return 0.0;
		default:
			throw new RuntimeException("Cannot determine disk cost for " + diskType + " disk type");
	}
}
	
}
