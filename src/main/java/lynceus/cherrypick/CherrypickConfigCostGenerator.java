package lynceus.cherrypick;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import lynceus.CostGenerator;
import lynceus.Main.timeout;
import lynceus.aws.AWSDirectory;
import lynceus.State;

public class CherrypickConfigCostGenerator implements CostGenerator<CherrypickConfig>{

	/* class attributes */
	private final static String data = "data/cherrypick/";
	private static String targetWkld = "CP_kmeans.csv";
	private Map<CherrypickConfig, Double> costMap = null;
	private Map<CherrypickConfig, Double> timeMap = null;
	
	private double avgCost;
	private double maxTime;
	private double minTime;
	
	/* class constructor */
	public CherrypickConfigCostGenerator() {
		updateCostOnWorkload(targetWkld);
	}
	
	private void updateCostOnWorkload(String currWorkload) {
		FileReader csvData = null;
		int counter = 0;
		double t_max = 0;
        double t_min = Double.MAX_VALUE;
        //setup a map config-> cost taken from the file
        targetWkld = currWorkload;
        costMap = new HashMap<>();
        timeMap = new HashMap<>();
        avgCost = 0;
		
		try{
			csvData = new FileReader(new File(data + currWorkload));
		}catch(Exception e) {
			System.out.print("[CherrypickConfig Cost Generator] ");
			e.printStackTrace();
		}
		
		CSVParser parser;
		try {
			parser = CSVParser.parse(csvData, CSVFormat.RFC4180);
			for (CSVRecord csvRecord : parser) {
	        	if(counter >= 1){
		           CherrypickConfig config = CherrypickConfig.parse(csvRecord);
		           double time = Double.parseDouble(csvRecord.get(4));
		           double cost = (time / 60) * costPerConfigPerMinute(config);
		           costMap.put(config, cost);
		           timeMap.put(config, time);
		           // System.out.println(currWorkload + " " + config + " " + " " + time + " " + cost);
		           avgCost += cost;
		           if (time < t_min) {
		              t_min = time;
		           }
		           if (time > t_max) {
		        	   t_max = time;
		           }
	        	}
	        	counter ++;
	        }
	        
	        avgCost /= costMap.size();
	        minTime = t_min;
	        maxTime = t_max;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public double getAvgCost() {
		return avgCost;
	}

	public double getMinTime() {
		return this.minTime;
	}
	
	public double getMaxTime() {
		return maxTime;
	}
	
	public static void setTargetWkld(int targetWkld) {
		
		//wkld_id = targetWkld;
		try {
			File f = new File(data);
			if (!f.exists()) {
				throw new RuntimeException("File  " + data + " does not exist.");
			}
			
			int i = 0;
			ArrayList<File> files = new ArrayList<>();
			for (File file : f.listFiles()) {
				if (file.getName().contains(".DS"))
		            continue;
				else {
					files.add(i, file);
					i++;
				}
			}
			assert files != null;
			Collections.sort(files);
			CherrypickConfigCostGenerator.targetWkld = files.get(targetWkld - 1).getName();
			System.out.println("WKLD set " + targetWkld + " --- " + files.get(targetWkld - 1).getName());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	/** 
	 * Method for finding the max_time that corresponds to only having a given percentage
	 * of the search space feasible
	 * @input feasible percentage of the search space
	 * @return max_time constraint (in min)
	 */
	public double getTimeForSpecificConstraint(double perc) {
		double[] times = new double[timeMap.size()];
		double maxTime = Double.POSITIVE_INFINITY;
		
		int numberOfFeasibleConfigs = (int) Math.round(timeMap.size()*perc);
		System.out.println("NbFeasibleConfigs = " + numberOfFeasibleConfigs);
		
		int i = 0;
		for (Map.Entry<CherrypickConfig, Double> config : timeMap.entrySet()) {
			times[i] = config.getValue();
			i ++;
		}
		Arrays.sort(times);
		
		if (numberOfFeasibleConfigs == timeMap.size()) {
			maxTime = times[numberOfFeasibleConfigs-1]; // in min
		} else {
			maxTime = times[numberOfFeasibleConfigs]; // in min
		}
		
		System.out.println("NbFeasibleConfigs = " + numberOfFeasibleConfigs + " ; maxTime = " + maxTime);
		
		return maxTime;
	}

	
	@Override
	public double setupCost(State state, CherrypickConfig config) {
		return 0.0D;
	}

	@Override
	public double deploymentCost(State state, CherrypickConfig config) {
		final Double cost = costMap.get(config);

		if (cost == null)
			throw new RuntimeException(config + " with hashcode " + config.hashCode() + " not in cost map for wkld " + targetWkld + " map size " + costMap.size());
		return cost;
	}

	@Override
	public double costPerConfigPerMinute(CherrypickConfig config) {
		return AWSDirectory.cherrypickCostPerConfigPerMinute(config.getVm_type(), config.getVm_size(), config.getNum_instances());

	}

	@Override
	public double[] getIntermediateValues(CherrypickConfig config, int amount, timeout timeoutType) {
		
		double[] intermediateValues = null;
		double totalTime = this.deploymentCost(null,config) / this.costPerConfigPerMinute(config);
//		System.out.println("[CostGenerator class] totalTime="+totalTime);

		intermediateValues = new double[amount + 1];
		intermediateValues[0] = 0;
		
		for (int j = 1; j < intermediateValues.length; j++) {
			intermediateValues[j] = (0.5 * j) / totalTime;
//			System.out.println("[CostGenerator class] intermediateValues["+j+"]=" + intermediateValues[j] + " time=" + 0.5*j + " totalTime=" + totalTime);
		}
		
		return intermediateValues;
	}

	@Override
	public double getAccForSpecificTime(double time, CherrypickConfig config) {
		double totalTime = this.deploymentCost(null,config) / this.costPerConfigPerMinute(config);
		
		double acc = time / totalTime;
//		System.out.println("[CostGenerator class] totalTime="+totalTime + " time=" + time + " acc=" + acc);
		return acc;
	}

}
