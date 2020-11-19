package lynceus.tensorflow;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import lynceus.CostGenerator;
import lynceus.Main.timeout;
import lynceus.State;


public class TensorflowConfigCostGenerator implements CostGenerator<TensorflowConfig>{

	private final int timeOut = 600;		// secs
	private ArrayList<TensorflowDatapoint> dataset;
	private double avgCost;
	private double maxTime;		// mins
	private double minTime;		// mins	
	private static double accThreshold;	// this threshold is used to define which configurations we want to include in the
										// computation of the average cost of a configuration. Only the configurations that
										// have accuracy >= accThreshold are taken into account
	private TensorflowDatapoint currentDatapoint;
	
	
	
	/* class constructor */
	public TensorflowConfigCostGenerator(String dataset) throws IOException{
		this.dataset = parseCSV(dataset);
		initAvgCost();
		initTimes();
	}
	
	
	public static void setAccThreshold(double accThreshold) {
		TensorflowConfigCostGenerator.accThreshold = accThreshold;
	}



	/* implement interface methods */
	@Override
	public double setupCost(State state, TensorflowConfig tensorflow) {
		// TODO Auto-generated method stub
		return 0.0D;
	}

	@Override
	/** Return the deployment cost of a config. The deployment cost is the
	 * 	expected cost to be paid to get the minimum desired accuracy
	 * 	@input: State state: current state of the algorithm
	 * 			TensorflowConfig tensorflow: config whose cost we want
	 * 	@return: double deploymentCost
	 */
	public double deploymentCost(State state, TensorflowConfig tensorflow) {
		
		/* search for the datapoint corresponding to the config */
		currentDatapoint = TensorflowDatapoint.findDatapoint(tensorflow, dataset);
		//System.out.println(tensorflow + " ; " + currentDatapoint); 
		
		/* if there is no such point, assume worst case scenario cost */
		if(currentDatapoint == null){
//			System.out.println("[ConfigCostGenerator] Could not find " + tensorflow + " in the dataset --- assuming worst case scenario deployment cost");
			return (costPerConfigPerMinute(tensorflow) / 60.0) * timeOut; // worst case scenario, the configuration cannot reach the accuracy threshold before the timeout
		}
		/* otherwise return real observed deployment cost */
		return currentDatapoint.getPrice();
	}

	
	@Override
	/* cost per hour of a particular vm_flavor */
	public double costPerConfigPerMinute(TensorflowConfig tensorflow) {
		int num_instances = tensorflow.getNr_ps() + tensorflow.getNr_workers() + 1;
		
		/* costPerMin = costPerHour/60 */
		switch (tensorflow.getVm_flavor()) {
	        case 0: 	// "t2.small":
	           return num_instances * 0.023/60.0;
	        case 1: 	// "t2.medium":
	           return num_instances * 0.0464/60.0;
	        case 2: 	// "t2.xlarge":
	           return num_instances * 0.1856/60.0;
	        case 3: 	// "t2.2xlarge":
	           return num_instances * 0.3712/60.0;
	        default:
	           throw new RuntimeException("[ConfigCostGenerator] Inexistent vm flavor " + tensorflow.getVm_flavor());
		}
	}
	
	/* getters */
	public double getAvgCost(){
		return avgCost;
	}
	
	public double getMinTime() {
	    return this.minTime;
	}
	
	public double getMaxTime() {
	    return this.maxTime;
	}
	
	public ArrayList<TensorflowDatapoint> getDataset() {
		return dataset;
	}



	/* other methods */
	/** method for parsing a csv file. Expects a file with the attributes in the order:
	 * _id/$oid,n_workers,timestamp,learning_rate,batch_size,synchronism,training_time,n_ps,acc,vm_flavor,total_iterations
	 * @input name of the csv file
	 * @return list of the datapoints contained in the csv file */
	public ArrayList<TensorflowDatapoint> parseCSV(String file_path) throws IOException{
		FileReader csvData = null;
		ArrayList<TensorflowDatapoint> datapoints = new ArrayList<TensorflowDatapoint>();
		TensorflowDatapoint aux;
		int counter = 0;
		try{
			csvData = new FileReader(new File(file_path));
		}catch(Exception e) {
			System.out.print("[TensorflowConfig Cost Generator] ");
			 e.printStackTrace();
		}
		
		CSVParser parser = CSVParser.parse(csvData, CSVFormat.RFC4180);
		for (CSVRecord csvRecord : parser) {
			if(counter >= 1){
				int nr_workers		 = Integer.parseInt(csvRecord.get(1));
			    double learning_rate = Double.parseDouble(csvRecord.get(3));
			    int batch_size		 = Integer.parseInt(csvRecord.get(4));
			    int synchronism	 	 = (csvRecord.get(5).equals("async") ? 0:1);
			    double performance	 = Double.parseDouble(csvRecord.get(6));	// = training time
			    int nr_ps			 = Integer.parseInt(csvRecord.get(7));
			    double accuracy		 = Double.parseDouble(csvRecord.get(8));
			    String flavor	 	 = csvRecord.get(9);
			    
			    ArrayList<Double> intermediateValues = new ArrayList<Double>();
			    
			    if (csvRecord.size()>=12){
			    	ArrayList<String> intermediateValuesStr = new ArrayList<String>(Arrays.asList(csvRecord.get(11).split(" ")));
			    
				    int i = 0;
				    while(i < intermediateValuesStr.size()){
				    	intermediateValues.add(Double.parseDouble(intermediateValuesStr.get(i)));
				    	i ++;
				    }
			    }
			    
			    int vm_flavor = 0;
			    
			    switch(flavor){
			    	case "t2.small":
			    		vm_flavor = 0;
			    		break;
			    	case "t2.medium":
			    		vm_flavor = 1;
			    		break;
			    	case "t2.xlarge":
			    		vm_flavor = 2;
			    		break;
			    	case "t2.2xlarge":
			    		vm_flavor = 3;
			    		break;
			    	default:
				        throw new RuntimeException("[TensorflowConfigCostGenerator] Unknown flavor " + flavor);
			    }
			    
			    aux = new TensorflowDatapoint(nr_ps, nr_workers, learning_rate, batch_size, synchronism, performance, accuracy, vm_flavor, intermediateValues);
			    datapoints.add(aux);
			 }
			counter += 1;
		}
		
		return datapoints;
	}
	
	
	private void initAvgCost(){
		
		int counter = 0;
		for(TensorflowDatapoint d : dataset){
			if(d.getAccuracy() >= accThreshold){
				avgCost += d.getPrice();
				counter ++;
			}
		}
		avgCost /= counter;
	}
	
	private void initTimes(){
		double max = 0;
		double min = Double.MAX_VALUE;
		
		for (TensorflowDatapoint d: dataset){
			if (d.getPerformance() > max){
				max = d.getPerformance();
			} else if(d.getPerformance() < min){
				min = d.getPerformance();
			}
		}
		
		maxTime = max/60.0;	// in min
		minTime = min/60.0;	// in min
	}

	@Override
	public double getAccForSpecificTime(double time, TensorflowConfig config) {
		
		/* search for the datapoint corresponding to the config */
		currentDatapoint = TensorflowDatapoint.findDatapoint(config, dataset);
		
		if (currentDatapoint == null || currentDatapoint.getIntermediateValues().isEmpty()){
			return 0.0;
		} else {
			return currentDatapoint.getAccForSpecificTime(time);
		}
	}
	
	
	/** 
	 * Method for finding the max_time that corresponds to only having a given percentage
	 * of the search space feasible
	 * @input feasible percentage of the search space
	 * @return max_time constraint (in min)
	 */
	public double getTimeForSpecificConstraint(double perc) {
		double[] times = new double[dataset.size()];
		double maxTime = Double.POSITIVE_INFINITY;
		
		int numberOfFeasibleConfigs = (int) Math.round(dataset.size()*perc);
		
		for (int i = 0; i < times.length; i++) {
			times[i] = dataset.get(i).getPerformance();
		}
		Arrays.sort(times);
		
		if (numberOfFeasibleConfigs == dataset.size()) {
			maxTime = times[numberOfFeasibleConfigs-1]/60; // in min
		} else {
			maxTime = times[numberOfFeasibleConfigs]/60; // in min
		}
		System.out.println("NbFeasibleConfigs = " + numberOfFeasibleConfigs + " ; maxTime = " + maxTime);
		
		return maxTime;
	}


	@Override
	public double[] getIntermediateValues(TensorflowConfig config, int amount, timeout timeoutType) {
		double[]	intermediateValues = null;	// time
		ArrayList<Double>	values = new ArrayList<Double> ();
		int i = 0;
		int valueIndex = 0;
		
		values = TensorflowDatapoint.findDatapoint(config, this.getDataset()).getIntermediateValues();
		
		if (values.size() < amount) {
			amount = values.size();
		}

		intermediateValues = new double[amount + 1];
		intermediateValues[0] = 0;
		valueIndex = 0;
		i = 1;

		while (i < intermediateValues.length) {
			intermediateValues[i] = values.get(valueIndex);
			i ++;
			valueIndex ++;
		}
		
		return intermediateValues;
	}
	
}
