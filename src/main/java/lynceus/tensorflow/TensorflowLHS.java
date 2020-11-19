package lynceus.tensorflow;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import lynceus.Configuration;
import lynceus.LHS;
import lynceus.Main;
import weka.tensorflow.WekaTensorflowConfig;

public class TensorflowLHS<C extends Configuration> extends LHS{
	
	/* class attributes */
	private ArrayList<double[]> dims;
	
	/* class constructor */
	public TensorflowLHS(int samples) {
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
		
		double[] nr_ps = {1};
		double[] nr_cores = {8, 16, 32, 48, 64, 80, 96, 112};
//		double[] nr_workersSmall = {8, 16, 32, 48, 64, 80, 96, 112};
//		double[] nr_workersMedium = {4, 8, 16, 24, 32, 40, 48, 56};
//		double[] nr_workersXL = {2, 4, 8, 12, 16, 20, 24, 28};
//		double[] nr_workers2XL = {1, 2, 4, 6, 8, 10, 12, 14};
		double[] vm_flavor = {0, 1, 2, 3};
		double[] batch_size = {16, 256};
		double[] learning_rate = {0.001, 0.0001, 0.00001};
		double[] synchronism = {0, 1};
		
		this.dims.add(nr_cores);
		this.dims.add(vm_flavor);
//		this.dims.add(nr_workersSmall);
//		this.dims.add(nr_workersMedium);
//		this.dims.add(nr_workersXL);
//		this.dims.add(nr_workers2XL);
		this.dims.add(learning_rate);
		this.dims.add(batch_size);
		this.dims.add(synchronism);
		this.dims.add(nr_ps);

		//System.out.println("dims: " + dims);
		return this.dims;
	}

	/**
	 * Retrieve a new initial sample with values corresponding to index 'index'
	 * of all dimension arrays
	 */
	@Override
	protected Configuration newSample(int index) {
		
		double[] dim = new double[8];
		int auxIndex = -1;	// auxiliar to maintain the initial value of index when 
							// we need to do round-robin
		
		int 	nr_cores = 0;
		int		nr_ps = 1;	// 1
		int 	nr_workers = 0;
		double 	learning_rate = 0.1;
		int 	batch_size = 0;	 // 16; 256
		int		synchronism = 0; // 0 = async ; 1 = sync
		int 	vm_flavor = 0;	 // 0 = t2.small ; 1 = t2.medium ; 2 = t2.xlarge ; 3 = t2.2xlarge

		
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
			
			if (i == 0) {	// selecting nr_cores
				nr_cores = (int) dim[auxIndex];
			} else if (i == 1) { // selecting vm_flavor
				vm_flavor = (int) dim[auxIndex];
				if (vm_flavor == 0) { // if vm_flavor = 0 ==> t2.small
					nr_workers = nr_cores/1;
				} else if (vm_flavor == 1) { // if vm_flavor = 1 ==> t2.medium
					nr_workers = nr_cores/2;
				} else if (vm_flavor == 2) { // if vm_flavor = 2 ==> t2.xlarge
					nr_workers = nr_cores/4;
				} else { // if vm_flavor = 3 ==> t2.2xlarge
					nr_workers = nr_cores/8;
				}
			} else if (i == 2) {	// selecting learning_rate
				learning_rate = dim[auxIndex];
			} else if (i == 3) {	// selecting batch_size
				batch_size = (int) dim[auxIndex];
			} else if (i == 4) {	// selecting synchronism
				synchronism = (int) dim[auxIndex];
			} else {	// selecting nr_ps
				nr_ps = (int) dim[auxIndex];
			}
			//System.out.println("dim: " + Arrays.toString(dim) + " ; auxIndex = " + auxIndex + " ; lenght = " + this.dims.get(i).length + " ; i = " + i);
			
			i++;
		}
		
		TensorflowConfig newConfig = new TensorflowConfig(nr_ps, nr_workers, learning_rate, batch_size, synchronism, vm_flavor);
		//System.out.println("newConfig = " + newConfig);
		
		return new WekaTensorflowConfig(newConfig, super.dataset);
	}
	
	@Override
	protected void checkSamples(List samples, String type, int seed) {
		C currSample;
		double[] buckets = new double[19];
		int num_cores = 0;
		String file = "files/LHS/teste_" + type + "_" + seed + ".txt";
		PrintWriter writer = null;
		File f = new File(file);
		if (f.exists()) {
			f.renameTo(new File(f.getAbsolutePath().concat(".").concat(String.valueOf(System.currentTimeMillis())).concat(".txt")));
		}
		try {
			writer = new PrintWriter(file, "UTF-8");
		} catch (FileNotFoundException | UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		writer.println("type;seed;bucket;count;numSamples;percentage");
		writer.flush();
		
		for (int i = 0; i < samples.size(); i++) {
			currSample = (C) samples.get(i);
			
			switch((int)currSample.at(5)) { // vm_flavor
				case 0:
					num_cores = (int)currSample.at(1)*1;
					buckets[8] += 1;
					break;
				case 1:
					num_cores = (int)currSample.at(1)*2;
					buckets[9] += 1;
					break;
				case 2:
					num_cores = (int)currSample.at(1)*4;
					buckets[10] += 1;
					break;
				case 3:
					num_cores = (int)currSample.at(1)*8;
					buckets[11] += 1;
					break;
			}
			
			switch(num_cores) {
				case 8:
					buckets[0] += 1;
					break;
				case 16:
					buckets[1] += 1;
					break;
				case 32:
					buckets[2] += 1;
					break;
				case 48:
					buckets[3] += 1;
					break;
				case 64:
					buckets[4] += 1;
					break;
				case 80:
					buckets[5] += 1;
					break;
				case 96:
					buckets[6] += 1;
					break;
				case 112:
					buckets[7] += 1;
					break;
			}
			
			if ((double)currSample.at(2) == 0.001) {	// learning_rate
				buckets[12] += 1;
			} else if ((double)currSample.at(2) == 0.0001){
				buckets[13] += 1;
			} else {
				buckets[14] += 1;
			}
			
			
			if ((int)currSample.at(3) == 16) {// batch_size
				buckets[15] += 1;
			} else {
				buckets[16] += 1;
			}
			
			if ((int)currSample.at(4) == 0) {	// synchronism
				buckets[17] += 1;
			} else {
				buckets[18] += 1;
			}
			
		}
		
		writer.println(type + ";" + seed + ";num_cores = 8;" + buckets[0] + ";" + samples.size() + ";" + buckets[0]/samples.size());
		writer.println(type + ";" + seed + ";num_cores = 16;" + buckets[1] + ";" + samples.size() + ";" + buckets[1]/samples.size());
		writer.println(type + ";" + seed + ";num_cores = 32;" + buckets[2] + ";" + samples.size() + ";" + buckets[2]/samples.size());
		writer.println(type + ";" + seed + ";num_cores = 48;" + buckets[3] + ";" + samples.size() + ";" + buckets[3]/samples.size());
		writer.println(type + ";" + seed + ";num_cores = 64;" + buckets[4] + ";" + samples.size() + ";" + buckets[4]/samples.size());
		writer.println(type + ";" + seed + ";num_cores = 80;" + buckets[5] + ";" + samples.size() + ";" + buckets[5]/samples.size());
		writer.println(type + ";" + seed + ";num_cores = 96;" + buckets[6] + ";" + samples.size() + ";" + buckets[6]/samples.size());
		writer.println(type + ";" + seed + ";num_cores = 112;" + buckets[7] + ";" + samples.size() + ";" + buckets[7]/samples.size());
		writer.println(type + ";" + seed + ";vm_flavor = 0;" + buckets[8] + ";" + samples.size() + ";" + buckets[8]/samples.size());
		writer.println(type + ";" + seed + ";vm_flavor = 1;" + buckets[9] + ";" + samples.size() + ";" + buckets[9]/samples.size());
		writer.println(type + ";" + seed + ";vm_flavor = 2;" + buckets[10] + ";" + samples.size() + ";" + buckets[10]/samples.size());
		writer.println(type + ";" + seed + ";vm_flavor = 3;" + buckets[11] + ";" + samples.size() + ";" + buckets[11]/samples.size());
		writer.println(type + ";" + seed + ";learning_rate = 0.001;" + buckets[12] + ";" + samples.size() + ";" + buckets[12]/samples.size());
		writer.println(type + ";" + seed + ";learning_rate = 0.0001;" + buckets[13] + ";" + samples.size() + ";" + buckets[13]/samples.size());
		writer.println(type + ";" + seed + ";learning_rate = 0.00001;" + buckets[14] + ";" + samples.size() + ";" + buckets[14]/samples.size());
		writer.println(type + ";" + seed + ";batch_size = 16;" + buckets[15] + ";" + samples.size() + ";" + buckets[15]/samples.size());
		writer.println(type + ";" + seed + ";batch_size = 256;" + buckets[16] + ";" + samples.size() + ";" + buckets[16]/samples.size());
		writer.println(type + ";" + seed + ";synchronism = 0;" + buckets[17] + ";" + samples.size() + ";" + buckets[17]/samples.size());
		writer.println(type + ";" + seed + ";synchronism = 1;" + buckets[18] + ";" + samples.size() + ";" + buckets[18]/samples.size());
		writer.flush();
		writer.close();
	}

	protected ArrayList<Double> dimensions(Configuration config) {
		ArrayList<Double> dimensionsArray = new ArrayList<Double>();
		int nr_cores;
		
		if ((int)config.at(5) == 0) {	// t2.small
			dimensionsArray.add(1.0);
			nr_cores = (int)config.at(1);
		} else if ((int)config.at(5) == 1) {	// t2.medium
			dimensionsArray.add(2.0);
			nr_cores = (int)config.at(1)*2;
		} else if ((int)config.at(5) == 2) {	// t2.xlarge
			dimensionsArray.add(3.0);
			nr_cores = (int)config.at(1)*4;
		} else {	// t2.2xlarge
			dimensionsArray.add(4.0);
			nr_cores = (int)config.at(1)*8;
		}
		
		switch(nr_cores) {
			case 8:
				dimensionsArray.add(1.0);
				break;
			case 16:
				dimensionsArray.add(2.0);
				break;
			case 32:
				dimensionsArray.add(3.0);
				break;
			case 48:
				dimensionsArray.add(4.0);
				break;
			case 64:
				dimensionsArray.add(5.0);
				break;
			case 80:
				dimensionsArray.add(6.0);
				break;
			case 96:
				dimensionsArray.add(7.0);
				break;
			case 112:
				dimensionsArray.add(8.0);
				break;
		}
		
		if ((double)config.at(2) == 0.001) {	// learning_rate
			dimensionsArray.add(1.0);
		} else if ((double)config.at(2) == 0.0001) {
			dimensionsArray.add(2.0);
		} else {
			dimensionsArray.add(3.0);
		}
		
		if ((int)config.at(3) == 16) {	// batch_size
			dimensionsArray.add(1.0);
		} else {
			dimensionsArray.add(2.0);
		}
		
		dimensionsArray.add((int)config.at(4) + 1.0); // synchronism
		dimensionsArray.add(1.0); // nr_ps
		
		return dimensionsArray;

	}
	
}
