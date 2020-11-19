package lynceus.scout;

import lynceus.CostGenerator;
import lynceus.Main.timeout;
import lynceus.State;
import lynceus.aws.AWSDirectory;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static lynceus.scout.ScoutVMConfig.parseName;


/**
 * @author Diego Didona
 * @email diego.didona@epfl.ch
 * @since 29.03.18
 */
public class ScoutVMCostGenerator implements CostGenerator<ScoutVMConfig> {

   /* class attributes */
   private final static String data = "data/scout/osr_multiple_nodes_original/";
   private static String targetWkld = "join_bigdata_spark";
   private Map<ScoutVMConfig, Double> costMap = null;
   private Map<ScoutVMConfig, Double> timeMap = null;

   /* to write a file with the dataset info */
   private boolean write_file = false;
   private static int wkld_id;

   private double avgCost;
   private double maxTime;	// min
   private double minTime;	// min

   /* class constructor */
   public ScoutVMCostGenerator() {
      /* to write a file with the dataset info */
		   if (write_file) {
			   PrintWriter writer = null;
		       String resultsFile = "data/scout/osr_multiple_nodes_original/wkld"+ wkld_id + "_" + targetWkld + ".txt";
			   
		       try {
		    	   File f = new File(resultsFile);
		    	   writer = new PrintWriter(f, "UTF-8");
		       } catch (FileNotFoundException | UnsupportedEncodingException e) {
		    	   // TODO Auto-generated catch block
		    	   e.printStackTrace();
		       }
			   
			   writer.println("type,size,vcpus,ecus,frequency,ram,ebs_band,net_band,num_instances,time,cost,maxTime,$$/h");
			   updateCostOnWorkload(targetWkld, writer);
			   writer.close();
			   write_file = false;
		   } else {
			   updateCostOnWorkload(targetWkld);
		   }
//      updateCostOnWorkload(targetWkld);
   }

   /* getters */
   public double getAvgCost() {
      return avgCost;
   }

   public double getMinTime() {
      return this.minTime;
   }

   public double getMaxTime() {
      return maxTime;
   }

   /* interface methods to implement */
   @Override
   public double setupCost(State state, ScoutVMConfig config) {
      return 0.0D;
   }

   @Override
   public double deploymentCost(State state, ScoutVMConfig config) {
      final Double cost = costMap.get(config);

      if (cost == null)
         throw new RuntimeException(config + " with hashcode " + config.hashCode() + " not in cost map for wkld " + targetWkld + " map size " + costMap.size());
      return cost;
   }

   @Override
   public double costPerConfigPerMinute(ScoutVMConfig config) {
      return AWSDirectory.costPerConfigPerMinute(config.getType(), config.getSize(), config.getNum_instances());

   }

   @Override
   public double getAccForSpecificTime(double time, ScoutVMConfig config) {
	   double totalTime = this.deploymentCost(null,config) / this.costPerConfigPerMinute(config);
		
		double acc = time / totalTime;
//		System.out.println("[CostGenerator class] totalTime="+totalTime + " time=" + time + " acc=" + acc);
		return acc;
   }

   @Override
   public double[] getIntermediateValues(ScoutVMConfig config, int amount, timeout timeoutType) {
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


   /* other methods */
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
         ScoutVMCostGenerator.targetWkld = files.get(targetWkld - 1).getName();
         System.out.println("WKLD set " + targetWkld + " --- " + files.get(targetWkld - 1).getName());
      } catch (Exception e) {
         throw new RuntimeException(e);
      }
   }


   //TODO: note that the cost in time here might differ from the cost in time
   //	   obtained doing "cost / cost_per_unit", because of a different rounding.
   private static double minutesForExperiment(String name) throws Exception {
      File folder = new File(name);
      if (!folder.isDirectory()) {
         System.out.println(folder.getAbsolutePath() + " is not a dir");
         System.exit(-1);
      }
      File[] listOfFiles = folder.listFiles();
      assert listOfFiles != null;
      for (File f : listOfFiles) {
         if (f.getName().contains("json")) {
            JSONParser parser = new JSONParser();
            JSONObject bo = (JSONObject) parser.parse(new FileReader(f.getAbsolutePath()));
            double time = Double.parseDouble(bo.get("elapsed_time").toString());
            time /= 60.0; //seconds to minutes
            return Math.ceil(time);
         }
      }
      throw new RuntimeException("Time taken for exp " + name + " not found");
   }


   //TODO: note that the cost in time here might differ from the cost in time
   //	   obtained doing "cost / cost_per_unit", because of a different rounding.
   private static double adjustedMinutesForExperiment(String name, double maxTime) throws Exception {

      File folder = new File(name);
      if (!folder.isDirectory()) {
         System.out.println(folder.getAbsolutePath() + " is not a dir");
         System.exit(-1);
      }
      File[] listOfFiles = folder.listFiles();
      assert listOfFiles != null;
      for (File f : listOfFiles) {
         if (f.getName().contains("json")) {
            JSONParser parser = new JSONParser();
            JSONObject bo = (JSONObject) parser.parse(new FileReader(f.getAbsolutePath()));
            Double time = Double.parseDouble(bo.get("elapsed_time").toString());
            String completed = bo.get("completed").toString();
            if (completed.compareTo("true") == 0) {
               time /= 60.0; //seconds to minutes
               return Math.ceil(time);
            } else {
               time = maxTime;
               //time /= 60.0; //seconds to minutes
               return Math.ceil(time);
            }
         }
      }
      throw new RuntimeException("Time taken for exp " + name + " not found");
   }

   private static double maxMinutesForExperiment(File[] files) throws Exception {
      double max = 0, t;
      for (File f : files) {
         if (f.getName().contains(".DS"))
            continue;
         t = minutesForExperiment(f.getAbsolutePath());
         if (t > max) {
            max = t;
         }
      }
      return max;
   }


   private void updateCostOnWorkload(String currWorkload) {

      try {
         double t_max = 0;
         double t_min = Double.MAX_VALUE;
         //setup a map config-> cost taken from the file
         targetWkld = currWorkload;
         costMap = new HashMap<>();
         timeMap = new HashMap<>();
         avgCost = 0;
         File folder = new File(data + targetWkld); //folder with
         System.out.println("WKLD: " + targetWkld);
         File[] listOfFiles = folder.listFiles();
         assert listOfFiles != null;
         maxTime = maxMinutesForExperiment(listOfFiles);
         for (File f : listOfFiles) {
            if (f.getName().contains("DS"))
               continue;
            String f_name = f.getName();
            ScoutVMConfig config = parseName(f_name);
            double time = adjustedMinutesForExperiment(f.getAbsolutePath(), maxTime);
            double cost = time * costPerConfigPerMinute(config);
            costMap.put(config, cost);
            timeMap.put(config, time);
            // System.out.println(currWorkload + " " + config + " " + " " + time + " " + cost);
            avgCost += cost;
            if (time < t_min) {
               t_min = time;
            }
         }
         avgCost /= costMap.size();

         minTime = t_min;
      } catch (Exception e) {
         e.printStackTrace();
         throw new RuntimeException(e);
      }
   }

   private void updateCostOnWorkload(String currWorkload, PrintWriter writer) {
	   try {
		   double t_min = Double.MAX_VALUE;
		   //setup a map config-> cost taken from the file
		   targetWkld = currWorkload;
		   costMap = new HashMap<>();
		   timeMap = new HashMap<>();
		   avgCost = 0;
		   		   
		   File folder = new File(data + targetWkld); //folder with
		   System.out.println("WKLD: " + targetWkld);
		   File[] listOfFiles = folder.listFiles();
		   assert listOfFiles != null;
		   maxTime = maxMinutesForExperiment(listOfFiles);
		   for (File f : listOfFiles) {
			   if (f.getName().contains(".DS"))
				   continue;
			   String f_name = f.getName();	
			   ScoutVMConfig config = parseName(f_name);
			   
			   double time = adjustedMinutesForExperiment(f.getAbsolutePath(), maxTime);
			   double cost = time * costPerConfigPerMinute(config);
			   writer.println(config.getType().toString() + "," + config.getSize().toString() + "," + config.getVcpus() + "," + config.getEcus() + "," + config.getFrequency() + "," + config.getRam() + "," + config.getEbs_band() + "," + config.getNet_band() + "," + config.getNum_instances() + "," + time + "," + cost + "," + maxTime + "," + costPerConfigPerMinute(config));
			   costMap.put(config, cost);
			   timeMap.put(config, time);
			   // System.out.println(currWorkload + " " + config + " " + " " + time + " " + cost);
			   avgCost += cost;
			   if (time < t_min) {
				   t_min = time;
			   }
		   }
		   avgCost /= costMap.size();
		   minTime = t_min;
	   } catch (Exception e) {
		   e.printStackTrace();
		   throw new RuntimeException(e);
	   }
   }


   /**
    * Method for finding the max_time that corresponds to only having a given percentage of the search space feasible
    *
    * @return max_time constraint (in min)
    * @input feasible percentage of the search space
    */
   public double getTimeForSpecificConstraint(double perc) {
      double[] times = new double[timeMap.size()];
      double maxTime = Double.POSITIVE_INFINITY;

      int numberOfFeasibleConfigs = (int) Math.round(((double) timeMap.size()) * perc);
      System.out.println("Perc "+ perc+" AllConfigs "+timeMap.size()+" NbFeasibleConfigs = " + numberOfFeasibleConfigs);

      int i = 0;
      for (Map.Entry<ScoutVMConfig, Double> config : timeMap.entrySet()) {
         times[i] = config.getValue();
         i++;
      }
      Arrays.sort(times);

      if (numberOfFeasibleConfigs == timeMap.size()) {
         maxTime = times[numberOfFeasibleConfigs - 1]; // in min
      } else {
         maxTime = times[numberOfFeasibleConfigs]; // in min
      }
      System.out.println("NbFeasibleConfigs = " + numberOfFeasibleConfigs + " ; maxTime = " + maxTime);

      return maxTime;
   }

}
