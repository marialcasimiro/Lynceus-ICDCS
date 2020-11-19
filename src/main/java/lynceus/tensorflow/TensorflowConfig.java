package lynceus.tensorflow;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import lynceus.Configuration;
import lynceus.ModelSample;
import lynceus.Pair;
import lynceus.TestSet;

public class TensorflowConfig implements Configuration{
	
	/* class attributes */
	protected int		nr_ps;	// 1
	protected int 		nr_workers;
	protected double 	learning_rate;
	protected int 		batch_size;	 // 16; 256
	protected int		synchronism; // 0 = async ; 1 = sync
	protected int 		vm_flavor;	 // 0 = t2.small ; 1 = t2.medium ; 2 = t2.xlarge ; 3 = t2.2xlarge
	
	
	/* class constructor */
	protected TensorflowConfig(){
		
	}
	
	public TensorflowConfig(int nr_ps, int nr_workers, double lr, int bs, int synchronism, int flavor){
		this.nr_ps = nr_ps;
		this.nr_workers = nr_workers;
		learning_rate = lr;
		batch_size = bs;
		this.synchronism = synchronism;
		vm_flavor = flavor;
	}
	
	
	/* getters */
	public int getNr_ps(){
		return nr_ps;
	}
	   
	public int getNr_workers() {
	   return nr_workers;
	}
		
	public double getLearning_rate() {
	   return learning_rate;
	}
		
	public int getBatch_size() {
	   return batch_size;
	}
		
	public int getSynchronism(){
	   return synchronism;
	}
	   
	public int getVm_flavor() {
	   return vm_flavor;
	}
   
	/* implement interface methods */
	@Override
   public int numAttributes() {
      return 6;
   }
	
   @Override
   public Object at(int i) {
      switch (i) {
      	 case 0:
      		 return nr_ps;
         case 1:
            return nr_workers;
         case 2:
            return learning_rate;
         case 3:
            return batch_size;
         case 4:
        	 return synchronism;
         case 5:
            return vm_flavor;
         default:
            throw new RuntimeException("[TensorflowConfig] Requested attribute " + i + " but only " + numAttributes() + " available");
      }
   }
   
   @Override
   public Configuration clone() {
      return new TensorflowConfig(nr_ps, nr_workers, learning_rate, batch_size, synchronism, vm_flavor);
   }


   
   /* other methods */
   @Override
   public String toString() {
	   return "Config [nr_ps=" + nr_ps + ", nr_workers=" + nr_workers + ", learning_rate=" + learning_rate + ", batch_size=" + batch_size
			   + ", synchronism=" + synchronism + ", vm_flavor=" + vm_flavor + "]";
   }

   	@Override
	public int hashCode() {
//		final int prime = 31;
//		int result = 1;
//		result = prime * result + batch_size;
//		long temp;
//		temp = Double.doubleToLongBits(learning_rate);
//		result = prime * result + (int) (temp ^ (temp >>> 32));
//		result = prime * result + nr_ps;
//		result = prime * result + nr_workers;
//		result = prime * result + synchronism;
//		result = prime * result + vm_flavor;
//		return result;
   		return Objects.hash(nr_ps, nr_workers, learning_rate, batch_size, synchronism, vm_flavor);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
//		if (getClass() != obj.getClass())
//			return false;
		TensorflowConfig other = (TensorflowConfig) obj;
		if (batch_size != other.batch_size)
			return false;
		if (Double.doubleToLongBits(learning_rate) != Double.doubleToLongBits(other.learning_rate))
			return false;
		if (nr_ps != other.nr_ps)
			return false;
		if (nr_workers != other.nr_workers)
			return false;
		if (synchronism != other.synchronism)
			return false;
		if (vm_flavor != other.vm_flavor)
			return false;
		return true;
	}

}
