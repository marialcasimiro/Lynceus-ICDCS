package weka.tensorflow;

import java.util.ArrayList;
import java.util.Arrays;

import lynceus.Configuration;
import lynceus.ModelSample;
import lynceus.Pair;
import lynceus.TestSet;
import lynceus.WekaConfiguration;
import lynceus.tensorflow.TensorflowConfig;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

public class WekaTensorflowConfig extends TensorflowConfig implements WekaConfiguration{

	/* class attributes */
	private Instances dataset;
	
	/* config parameters */
	final static Integer[] flavor	 	   = {0, 1, 2, 3};
	final static Double [] lr			   = {0.001, 0.0001, 0.00001};
	final static Integer[] bs			   = {16, 256};
	final static Integer[] sync			   = {0, 1};
	final static Integer[] nr_worker_cores = {8, 16, 32, 48, 64, 80, 96, 112};
	final static Integer[] ps			   = {1};
	
	/* class constructors */
	public WekaTensorflowConfig(int nr_ps, int nr_instances, double lr, int bs, int synchronism, int flavor, Instances d) {
		super(nr_ps, nr_instances, lr, bs, synchronism, flavor);
		dataset = d;
		if (this.dataset == null)
	    	throw new RuntimeException("[WekaTensorflowConfig] Cannot be null");
	}
	
	public WekaTensorflowConfig(Instance i){
		super((int) i.value(0), (int) i.value(1), (double) i.value(2), (int) i.value(3), (int)i.value(4), (int)i.value(5));
		this.dataset = i.dataset();
	    if (this.dataset == null)
	    	throw new RuntimeException("[WekaTensorflowConfig] Cannot be null");
	}

	public WekaTensorflowConfig(TensorflowConfig wekaTensorflow, Instances dataset) {
		super(wekaTensorflow.getNr_ps(), wekaTensorflow.getNr_workers(), wekaTensorflow.getLearning_rate(), wekaTensorflow.getBatch_size(), wekaTensorflow.getSynchronism(), wekaTensorflow.getVm_flavor());
		this.dataset = dataset;
		if (this.dataset == null)
	    	throw new RuntimeException("[WekaTensorflowConfig] Cannot be null");
	}

	/* interface methods */
	@Override
	public Instance toInstance() {
//	   double ret[] = new double[dataset.numAttributes()];   //leave room for the target attribute as well
//	   ret[0] = this.getNr_ps();
//	   ret[1] = this.getNr_workers();
//	   ret[2] = this.getLearning_rate();
//	   ret[3] = this.getBatch_size();
//	   ret[4] = (double) this.getSynchronism();
//	   ret[5] = (double) this.getVm_flavor();
//	   Instance rr = new DenseInstance(1.0, ret);
//	   rr.setDataset(this.dataset);
//	   return rr;
		
		Instance rr = new DenseInstance(numAttributes() + 1);  // + 1 for the target attribute
        rr.setDataset(this.dataset); //first set dataset, otherwise following statements fail with "no dataset associated" exception
        rr.setValue(0, this.getNr_ps());
        rr.setValue(1, this.getNr_workers());
        rr.setValue(2, this.getLearning_rate());
        rr.setValue(3, this.getBatch_size());
        rr.setValue(4, (double) this.getSynchronism());
        rr.setValue(5, (double) this.getVm_flavor());
        return rr;
		
	}

	@Override
	public WekaTensorflowConfig clone() {
	   return  new WekaTensorflowConfig(this,this.dataset);   //NB: this is *not* copying the instances
	}
	
	public ArrayList<WekaConfiguration> neighbourhood(){
		   
		   /* method variables */
			WekaConfiguration 			neighbour;														// next neighbour to be inserted in the neighbourhood				
		   int							index					= 0;									//  
		   ArrayList<WekaConfiguration> neighbourhood 			= new ArrayList<WekaConfiguration>(); 	// neighbourhood of configuration config

		   /* choose neighbours */
		   // nr_ps
		   index = Arrays.asList(ps).indexOf(this.nr_ps);
		   if (index >= 0 && index < ps.length-1){	// generate next
			   neighbour = new WekaTensorflowConfig(ps[index + 1], this.nr_workers, this.learning_rate, this.batch_size, this.synchronism, this.vm_flavor, this.dataset);
			   neighbourhood.add(neighbour);
		   }
		   if (index <= ps.length-1 && index > 0){	// generate previous
			   neighbour = new WekaTensorflowConfig(ps[index - 1], this.nr_workers, this.learning_rate, this.batch_size, this.synchronism, this.vm_flavor, this.dataset);
			   neighbourhood.add(neighbour);
		   }
			
		   // nr_workers
		   // nr_worker_cores = nr_workers * nr_cores
		   index = Arrays.asList(nr_worker_cores).indexOf(this.nr_workers * nrCores(this.vm_flavor));
		   if (index >= 0 && index < nr_worker_cores.length - 1){	// generate next
			   neighbour = new WekaTensorflowConfig(this.nr_ps, nr_worker_cores[index + 1]/nrCores(this.vm_flavor), this.learning_rate, this.batch_size, this.synchronism, this.vm_flavor, this.dataset);
			   neighbourhood.add(neighbour);
		   }
		   if (index <= nr_worker_cores.length - 1 && index > 0){	// generate previous
			   neighbour = new WekaTensorflowConfig(this.nr_ps, nr_worker_cores[index - 1]/nrCores(this.vm_flavor), this.learning_rate, this.batch_size, this.synchronism, this.vm_flavor, this.dataset);
			   neighbourhood.add(neighbour);
		   }
			   		
		   // learning_rate
		   index = Arrays.asList(lr).indexOf(this.learning_rate);
		   if (index >= 0 && index < lr.length - 1){	// generate neighbour forward
			   neighbour = new WekaTensorflowConfig(this.nr_ps, this.nr_workers, lr[index + 1], this.batch_size, this.synchronism, this.vm_flavor, this.dataset);
			   neighbourhood.add(neighbour);
		   }
		   if (index <= lr.length - 1 && index > 0){	// generate backward
			   neighbour = new WekaTensorflowConfig(this.nr_ps, this.nr_workers, lr[index - 1], this.batch_size, this.synchronism, this.vm_flavor, this.dataset);
			   neighbourhood.add(neighbour);
		   }

		   // batch_size
		   index = Arrays.asList(bs).indexOf(this.batch_size);
		   if (index >= 0 && index < bs.length - 1){	// generate neighbour forward
			   neighbour = new WekaTensorflowConfig(this.nr_ps, this.nr_workers, this.learning_rate, bs[index + 1], this.synchronism, this.vm_flavor, this.dataset);
			   neighbourhood.add(neighbour);
		   } 
		   if (index <= bs.length - 1 && index > 0){	// generate backward
			   neighbour = new WekaTensorflowConfig(this.nr_ps, this.nr_workers, this.learning_rate, bs[index - 1], this.synchronism, this.vm_flavor, this.dataset);
			   neighbourhood.add(neighbour);
		   }
			
		   // synchronism
		   index = Arrays.asList(sync).indexOf(this.synchronism);
		   if (index >= 0 && index < sync.length - 1){	// generate neighbour forward
			   neighbour = new WekaTensorflowConfig(this.nr_ps, this.nr_workers, this.learning_rate, this.batch_size, sync[index + 1], this.vm_flavor, this.dataset);
			   neighbourhood.add(neighbour);
		   } 
		   if (index <= sync.length - 1 && index > 0){	// generate backward
			   neighbour = new WekaTensorflowConfig(this.nr_ps, this.nr_workers, this.learning_rate, this.batch_size, sync[index - 1], this.vm_flavor, this.dataset);
			   neighbourhood.add(neighbour);
		   }
			
		   // flavor
		   index = Arrays.asList(flavor).indexOf(this.vm_flavor);
		   if (index >= 0 && index < flavor.length - 1){	// generate neighbour forward
			   neighbour = new WekaTensorflowConfig(this.nr_ps, (this.nr_workers*nrCores(this.vm_flavor))/nrCores(flavor[index + 1]), this.learning_rate, this.batch_size, this.synchronism, flavor[index + 1], this.dataset);
			   neighbourhood.add(neighbour);
		   } 
		   if (index <= flavor.length - 1 && index > 0){	// generate backward
			   neighbour = new WekaTensorflowConfig(this.nr_ps, (this.nr_workers*nrCores(this.vm_flavor))/nrCores(flavor[index - 1]), this.learning_rate, this.batch_size, this.synchronism, flavor[index - 1], this.dataset);
			   neighbourhood.add(neighbour);
		   }
			  
		   return neighbourhood;
	   }
	   
	public boolean findPair(ArrayList<Pair> searchSpace, Pair pair){
		for(Pair p : searchSpace){
			if(p.equals(pair))
				return true;
		}
		return false;
	}

	/** determine the number of cores of a specific VM flavor
	 * @param flavor
	 * @return number of cores of the specified VM flavor **/
	private int nrCores(int flavor){
		switch(flavor){
			case 0:
				return 1;
			case 1:
				return 2;
			case 2:
				return 4;
			case 3:
				return 8;
			default:
				throw new RuntimeException("[Config] unknown flavor " + flavor);	
		}
	}
	
}
