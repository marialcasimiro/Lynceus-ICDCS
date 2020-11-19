package weka.tensorflow;

import lynceus.tensorflow.TensorflowConfig;

public class WekaTensorflowConfigFactory {


	public static TensorflowConfigWekaTestSet buildInitTestSet(String arff){
		
		final TensorflowConfigWekaTestSet testSet = new TensorflowConfigWekaTestSet(arff);
		
		int nr_workers;
		int[] 	 synchronism		= new int[]{0, 1};
		int[] 	 batch_size_list 	= new int[]{16, 256};
		double[] learning_rate_list = new double[]{0.001, 0.0001, 0.00001};
		int[] 	 vm_flavors			= new int[]{0, 1, 2, 3};
		int[]	 total_worker_cores	= new int[]{8, 16, 32, 48, 64, 80, 96, 112};//, 128};

		for (int flavor : vm_flavors){
			for(int cores : total_worker_cores){
				for (int bs : batch_size_list){				
					for(double lr : learning_rate_list){
						for(int sync : synchronism){
							switch(flavor){
								case 0: 	// t2.small
									nr_workers = cores;
									TensorflowConfig a = new TensorflowConfig(1, cores, lr, bs, sync, flavor);
									testSet.addTestSample(a);
									break;
								case 1:		// t2.medium
									nr_workers = cores/2;
									TensorflowConfig b = new TensorflowConfig(1, nr_workers, lr, bs, sync, flavor); 
									testSet.addTestSample(b);
									break;
								case 2:		// t2.xlarge
									nr_workers = cores/4;
									TensorflowConfig c = new TensorflowConfig(1, nr_workers, lr, bs, sync, flavor); 
									testSet.addTestSample(c);
									break;
								case 3:		// t2.2xlarge
									nr_workers = cores/8;
									TensorflowConfig d = new TensorflowConfig(1, nr_workers, lr, bs, sync, flavor); 
									testSet.addTestSample(d);
									break;
								default:
									System.out.println("[WekaConfigFactory] Inexistent vm flavor " + flavor);
									break;
							}
						}	
					}
				}
			}
		}
		return testSet;
	}
	
	
}
