package lynceus.tensorflow;

import java.util.ArrayList;
import java.util.Objects;

import lynceus.Configuration;

public class TensorflowDatapoint extends TensorflowConfig implements Configuration{

	/* class attributes */
	private final int 			timeOut = 600;		// in seconds
	private double 				performance;		// in seconds
	private double 				accuracy;
	private double 				price;
	private ArrayList<Double>	intermediateValues;
		
	/* class constructors */
	protected TensorflowDatapoint(){
		
	}
	
	public TensorflowDatapoint(int nr_ps, int nr_workers, double learning_rate, int batch_size, int synchronism, double performance, double accuracy, int vm_flavor, ArrayList<Double> intermediateValues){
		super(nr_ps, nr_workers, learning_rate, batch_size, synchronism, vm_flavor);
		this.performance = performance;
		this.accuracy = accuracy;
		if(this.performance > timeOut && this.accuracy < 0.85){
			this.performance = (this.performance * 0.85) / this.accuracy;
		}
		this.price = this.performance * vm_price_per_sec(vm_flavor) * (nr_ps + nr_workers + 1);
		this.intermediateValues = intermediateValues;
	}
	
	public TensorflowDatapoint(TensorflowConfig c, double performance, double accuracy){
		super(c.getNr_ps(), c.getNr_workers(), c.getLearning_rate(), c.getBatch_size(), c.getSynchronism(), c.getVm_flavor());
		this.performance = performance;
		this.accuracy = accuracy;
		if(this.performance > timeOut && this.accuracy < 0.85){
			this.performance = (this.performance * 0.85) / this.accuracy;
		}
		this.price = this.performance * vm_price_per_sec(c.getVm_flavor()) * (c.getNr_workers() + c.getNr_ps() + 1);
	}
	
	
	/* implement interface methods */
	@Override
	public int numAttributes() {
		return 10;
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
	      case 6:
	    	  return performance;
	      case 7:
	    	  return accuracy;
	      case 8:
	    	  return price;
	      case 9:
	    	  return intermediateValues;
	      default:
	         throw new RuntimeException("[Datapoint] Requested attribute " + i + " but only " + numAttributes() + " available");
	   }
	}
	   
	@Override
	public Configuration clone() {
	   return new TensorflowDatapoint(nr_ps, nr_workers, learning_rate, batch_size, synchronism, performance, accuracy, vm_flavor, intermediateValues);
	}


	/* getters */
	public double getPerformance() {
		return performance;
	}

	public double getAccuracy() {
		return accuracy;
	}

	public double getPrice() {
		return price;
	}
	
	public ArrayList<Double> getIntermediateValues() {
		return intermediateValues;
	}

	/* other methods */
	/** return a config of a datapoint *
	 * @input: the datapoint
	 * @return: a config extracted from the datapoint **/
	public TensorflowConfig toTensorflow(){
		return new TensorflowConfig(this.getNr_ps(), this.getNr_workers(), this.getLearning_rate(), this.getBatch_size(), this.getSynchronism(), this.getVm_flavor());
	}
	
//	/** return a WekaConfig of a datapoint *
//	 * @input: the datapoint
//	 * @return: a WekaConfig extracted from the datapoint **/
//	public WekaConfig toWekaConfig(){
//		return new WekaConfig(this.getNr_workers(), this.getLearning_rate(), this.getBatch_size(), this.getSynchronism(), this.getVm_flavor());
//	}
	
	/** search for the datapoint with the configuration config and return it 
	 * @input: the configuration which we want to find, the dataset of all datapoints
	 * @return: the datapoint with the configuration **/
	public static TensorflowDatapoint findDatapoint(TensorflowConfig tensorflow, ArrayList<TensorflowDatapoint> dataset){
		TensorflowConfig c;
		for(TensorflowDatapoint d : dataset){
			c = d.toTensorflow();
			if(c.equals(tensorflow)){
				return d;
			}
		}
		return null;
	}
	
	@Override
	public String toString() {
		return "Datapoint [nr_ps=" + nr_ps + ", nr_workers=" + nr_workers + ", learning_rate=" + learning_rate + ", batch_size=" + batch_size
				+ ", synchronism=" + synchronism + ", performance=" + performance + ", accuracy=" + accuracy
				+ ", vm_flavor=" + vm_flavor + ", price=" + price + ", intermediateValues=" + intermediateValues +"]";
	}

	
	@Override
	public int hashCode() {
//		final int prime = 31;
//		int result = super.hashCode();
//		long temp;
//		temp = Double.doubleToLongBits(accuracy);
//		result = prime * result + (int) (temp ^ (temp >>> 32));
//		result = prime * result + ((intermediateValues == null) ? 0 : intermediateValues.hashCode());
//		temp = Double.doubleToLongBits(performance);
//		result = prime * result + (int) (temp ^ (temp >>> 32));
//		temp = Double.doubleToLongBits(price);
//		result = prime * result + (int) (temp ^ (temp >>> 32));
//		result = prime * result + timeOut;
//		return result;
		return Objects.hash(nr_ps, nr_workers, learning_rate, batch_size, synchronism, vm_flavor, accuracy, performance, price, intermediateValues);
	}	
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		TensorflowDatapoint other = (TensorflowDatapoint) obj;
		if (Double.doubleToLongBits(accuracy) != Double.doubleToLongBits(other.accuracy))
			return false;
		if (intermediateValues == null) {
			if (other.intermediateValues != null)
				return false;
		} else if (!intermediateValues.equals(other.intermediateValues))
			return false;
		if (Double.doubleToLongBits(performance) != Double.doubleToLongBits(other.performance))
			return false;
		if (Double.doubleToLongBits(price) != Double.doubleToLongBits(other.price))
			return false;
		if (timeOut != other.timeOut)
			return false;
		return true;
	}


	/** method to get the price per second for a specific instance flavor
	 ** price_per_hour/seconds_in_an_hour for Ohio availability zone
	 ** @input: vm flavor whose price we want to know **/
	private double vm_price_per_sec(int vm_flavor){
		switch (vm_flavor) {
	        case 0:		// "t2.small"
	           return 0.023/3600.0;
	        case 1: 	// "t2.medium":
	           return 0.0464/3600.0;
	        case 2: 	// "t2.xlarge":
	           return 0.1856/3600.0;
	        case 3: 	// "t2.2xlarge":
	           return 0.3712/3600.0;
	        default:
	           throw new RuntimeException("[TensorflowDatapoint] Inexistent vm flavor " + vm_flavor);
        }
	}
	
	public double getAccForSpecificTime(double time){
		int position = -1;
		
		position = (int) Math.round((time*60) / 30.0) - 1;
		if (position >= intermediateValues.size()){
			position = intermediateValues.size() - 1;
		}
		//System.out.println("time = " + time + " position = " + position + " intermediateValues = " + intermediateValues);
		if (position < 0){
			return 0.0;
		} else {
			return intermediateValues.get(position);
		}

	}
	
}
