package lynceus;

import lynceus.cherrypick.CherrypickConfigCostGenerator;
import lynceus.cherrypick.CherrypickLHS;
import lynceus.results.ExpParam;
import lynceus.results.OptResult;
import lynceus.scout.ScoutVMCostGenerator;
import lynceus.tensorflow.TensorflowConfigCostGenerator;

import weka.cherrypick.WekaCherrypickConfigFactory;
import weka.cherrypick.WekaCherrypickConfigLynceus;
import weka.scout.WekaScoutLynceus;
import weka.scout.WekaScoutVMConfigFactory;
import weka.tensorflow.WekaTensorflowConfigFactory;
import weka.tensorflow.WekaTensorflowConfigLynceus;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

/**
 * @author Diego Didona
 * @email diego.didona@epfl.ch
 * @since 15.03.18
 */
public class Main {

   static String file;
   private static String datasetFile;
   static double budget;
   static Lynceus.optimizer optimizer;
   static Lynceus.earlyStopOptions earlyStop;
   static int numSeeds;
   static long horizon;  // 2 = default value in Remi's paper
   private static int initialTrainingSamples;
   private static boolean allBudgets;
   private static double acc;
   static double gamma = 0.9;
   private static long gh = 5;
   private static double max_time = 0;      // in mins
   static double max_time_perc = 0;
   private static int WKLD_ID = 0;
   static String bootstrap;

   static int earlyStoppingCounter = 0;				// number of runs that ended due to the early stopping condition
   static int noMoreFeasibleSolutionsCounter = 0;	// number of runs that ended because, according to the model, there were no more configurations that costed less than the available budget

   private enum test {
      SCOUT, TENSORFLOW, CHERRYPICK
   }

   private static test target;

   public enum timeout {
      FALSE, IDEAL, LIN, TRUNCATED_GAUSSIAN, MAX_COST_PENALTY, NO_INFO
   }

   private static timeout timeoutType;

   static String timeoutToStr(timeout timeout) {
      switch (timeout) {
         case FALSE:
            return "false";
         case IDEAL:
            return "ideal";
         case LIN:
            return "linear";
         case TRUNCATED_GAUSSIAN:
        	 return "truncatedGaussian";
         case MAX_COST_PENALTY:
         	 return "maxCostPenalty";
         case NO_INFO:
        	 return "noInfo";
         default:
            return "unrecognized timeout type";
      }
   }

   private static timeout strToTimeout(String input) {
      if (input.equalsIgnoreCase("n") || input.equalsIgnoreCase("no") || input.equalsIgnoreCase("false")) {
         System.out.println("Running WITHOUT timeout");
         return timeout.FALSE;
      } else if (input.equalsIgnoreCase("ideal")) {
         System.out.println("Running with ideal timeout");
         return timeout.IDEAL;
      } else if (input.equalsIgnoreCase("linear")) {
         System.out.println("Running with linear timeout");
         return timeout.LIN;
      } else if (input.equalsIgnoreCase("truncatedGaussian")) {
    	  System.out.println("Running with truncated gaussian timeout");
          return timeout.TRUNCATED_GAUSSIAN;
      } else if (input.equalsIgnoreCase("maxCostPenalty")) {
    	  System.out.println("Running with max cost penalty timeout");
          return timeout.MAX_COST_PENALTY;
      } else if (input.equalsIgnoreCase("noInfo")) {
    	  System.out.println("Running with noInfo timeout");
          return timeout.NO_INFO;
      } else {
         System.out.println("unrecognized timeout type " + input + ". Setting to false.");
         return timeout.FALSE;
      }
   }

   private static Lynceus buildLynceus(long horizon, double budget, double epsilon, double gamma, Lynceus.optimizer opt, long wkld, Lynceus.earlyStopOptions es) {
      switch (target) {
         case SCOUT:
            return new WekaScoutLynceus(horizon, budget, epsilon, gamma, opt, wkld, es);
         case TENSORFLOW:
            return new WekaTensorflowConfigLynceus(horizon, budget, epsilon, gamma, opt, wkld, es);
         case CHERRYPICK:
            return new WekaCherrypickConfigLynceus(horizon, budget, epsilon, gamma, opt, wkld, es);
         default:
            throw new UnsupportedOperationException(target + " is not a supported test");
      }
   }

   public static void main(String[] args) {

      System.out.println(Arrays.toString(args));
      parseArgs(args);

      /* class attributes to compute duration of an experiment */
      double totalDuration = 0.0;
      double[] duration;
      ArrayList<Double> timer = new ArrayList<>();   // stores how long each seed took
      ArrayList<ArrayList<Double>> seedDurations = new ArrayList<>();
      double[] avgSeedDuration;
      long start;
      long startTimer;
      long endTimer;
      long elapsedTime;
      int counter;
      int budgetIndex;
      double time_sum;

      double __gamma;

      // By taking the avg cost we decide how much of the param space we want to let explore
      // The number of configs is roughly 150 so 150 * budget allows almost
      // complete exploration.  15 gives one tenth

      final Random shuffleRandom = new Random(30031987);

      // #########################
      // #		DEBUG MODE		 #
      // #########################
//      Lynceus.setDebugLogs();
      

      // ######################################
      // #		ESTIMATION ERRORS LOGS		  #
      // # only timeout has estimation errors #
      // ######################################
      Lynceus.setEstimationErrorsLogs();


      List<Integer> wklds = new ArrayList<>();
      int numWklds, initWklds;
      numWklds = 1;
      initWklds = 1;

      for (int i = initWklds; i <= numWklds; i++) {
         wklds.add(i);
      }
      Collections.shuffle(wklds, shuffleRandom);

      System.out.println("System;Wkld;Budget;Epsilon;A-DFO;A-NEX;A-CEX;50-DF0;50-NEX;50-CEX;90-DFO;90-NEX;90-CEX;99-DFO;99-NEX;99-CEX;WITHIN_CONSTRAINT;TOTAL_RUNS");
      for (int id : wklds) {
         counter = 0;
         budgetIndex = 0;
         time_sum = 0.0;

         double avgBudget;
         switch (target) {
            case SCOUT:
               ScoutVMCostGenerator costGenerator = new ScoutVMCostGenerator();
               avgBudget = costGenerator.getAvgCost();
               double maxTime = costGenerator.getMaxTime();
               double minTime = costGenerator.getMinTime();
               max_time = costGenerator.getTimeForSpecificConstraint(max_time_perc);
               System.out.println("MinTime " + minTime + " maxTime " + maxTime + " Setting T_max " + max_time);
               Lynceus.setT_max(max_time);
               break;
            case TENSORFLOW:
               avgBudget = 0.15;
               TensorflowConfigCostGenerator TFcostGenerator;
               try {
                  TFcostGenerator = new TensorflowConfigCostGenerator(datasetFile);
                  avgBudget = TFcostGenerator.getAvgCost();
                  System.out.println("Avg cost is " + avgBudget);
                  double maxTimeTF = TFcostGenerator.getMaxTime();
                  double minTimeTF = TFcostGenerator.getMinTime();
                  double tmaxTF = minTimeTF * (1.0D + max_time_perc);
                  max_time = TFcostGenerator.getTimeForSpecificConstraint(max_time_perc);
                  System.out.println("MinTime " + minTimeTF + " maxTime " + maxTimeTF + " Setting T_max " + max_time + " tmaxTF " + tmaxTF + " max_time_perc " + max_time_perc);
                  Lynceus.setT_max(max_time);
               } catch (IOException e1) {
                  System.out.println("[Main] Error setting up avgBudget");
                  e1.printStackTrace();
               }
               break;
            case CHERRYPICK:
               CherrypickConfigCostGenerator cherrypickCostGenerator;
               cherrypickCostGenerator = new CherrypickConfigCostGenerator();
               avgBudget = cherrypickCostGenerator.getAvgCost();
               System.out.println("Avg cost is " + avgBudget);
               double maxTimeCP = cherrypickCostGenerator.getMaxTime();
               double minTimeCP = cherrypickCostGenerator.getMinTime();
               max_time = cherrypickCostGenerator.getTimeForSpecificConstraint(max_time_perc);
               System.out.println("MinTime " + minTimeCP + " maxTime " + maxTimeCP + " Setting T_max " + max_time);
               Lynceus.setT_max(max_time);
               break;
            default:
               throw new UnsupportedOperationException(target + " is not a supported test");
         }

         if (max_time == 0) {
            throw new RuntimeException("Max time cannot be zero");
         }

         long[] seeds = new long[numSeeds];
         for (int s = 1; s <= numSeeds; s++) {
            seeds[s - 1] = s;
         }
         
         double[] budgets;

         if (allBudgets) {
            budgets = new double[]{1, 1.25, 1.5, 1.75, 2, 3, 4, 5};
            avgSeedDuration = new double[budgets.length];
            duration = new double[budgets.length];
         } else {
            budgets = new double[]{budget};
            avgSeedDuration = new double[1];
            duration = new double[1];
         }

         double[] epsilons = {0.0};

         HashMap<ExpParam, OptResult> hL = new HashMap<>();

         Lynceus.optimizer[] optimizers = {optimizer};

         /* Do experiments for workload*/
         double fixed_budget = 7.0;
         boolean is_fixed_budget = false;
         for (double b : budgets) {
            double actualBudget;
            if (!is_fixed_budget)
               actualBudget = b * avgBudget * initialTrainingSamples;
            else
               actualBudget = b * avgBudget * fixed_budget;
            System.out.println("D: Actual money " + actualBudget);
            budget = b;  //Set this for the name
            for (double e : epsilons) {
               start = System.currentTimeMillis();
               for (long s : seeds) {
                  counter = 0;
                  time_sum = 0;
                  startTimer = System.currentTimeMillis();
                  for (Lynceus.optimizer o : optimizers) {
                     __gamma = gamma; //0.9 is the default value in Remi's paper
                     
                     // #########################
                     // #		RUN LOGS		#
                     // #########################
                     Lynceus.setRunLogs(s);
                     
                     Lynceus.setDiscreteBudget(budget);
                     Lynceus.setTotalBudget(actualBudget);
                     Lynceus.setMaxTimePerc(max_time_perc);
                     Lynceus lyn = buildLynceus(horizon, actualBudget, e, __gamma, o, id, earlyStop);
                     ExpParam expParam = new ExpParam(b, e, s, id, o);
                     hL.put(expParam, lyn.doYourJob(s));
                     System.out.println("adding to hashmap: dist = " + hL.get(expParam).getDistFromOpt());
                  }
                  endTimer = System.currentTimeMillis() - startTimer;
                  timer.add(endTimer / 1000.0);
               }
               elapsedTime = System.currentTimeMillis() - start;
               duration[budgetIndex] = elapsedTime / 1000.0;
               totalDuration += duration[budgetIndex];
               while (counter < timer.size()) {
                  time_sum += timer.get(counter);
                  counter++;
               }
               avgSeedDuration[budgetIndex] = time_sum / timer.size();
               seedDurations.add((ArrayList<Double>) timer.clone());
               timer.clear();
               budgetIndex++;
            }
         }

         /* set-up results file */
         PrintWriter writer = null;
         String resultsFile = "files/results/";
         resultsFile = setFileName(id, resultsFile);
         try {
            File f = new File(resultsFile);
            if (f.exists()) {
               f.renameTo(new File(f.getAbsolutePath().concat(".").concat(String.valueOf(System.currentTimeMillis())).concat(".txt")));
            }
            writer = new PrintWriter(resultsFile, "UTF-8");
         } catch (FileNotFoundException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
         } catch (UnsupportedEncodingException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
         }

         /* set-up log file */
         PrintWriter logWriter = null;
         String logFile = "files/logs/";
         logFile = setFileName(id, logFile);
         try {
            File f = new File(logFile);
            if (f.exists()) {
               f.renameTo(new File(f.getAbsolutePath().concat(".").concat(String.valueOf(System.currentTimeMillis())).concat(".txt")));
            }
            logWriter = new PrintWriter(logFile, "UTF-8");
         } catch (FileNotFoundException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
         } catch (UnsupportedEncodingException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
         }



         /* Print results for workload and copy them to file */
         budgetIndex = 0;
         for (double b : budgets) {
            double actualBudget = 0;
            if (!is_fixed_budget)
               actualBudget = b * avgBudget * initialTrainingSamples;
            else
               actualBudget = b * avgBudget * fixed_budget;
            for (double e : epsilons) {
               for (Lynceus.optimizer o : optimizers) {
                  printResults(hL, o, b, e, id, seeds, writer, budgetIndex, duration, avgSeedDuration, actualBudget, logWriter, seedDurations);
               }
            }
            budgetIndex++;
         }
         writer.println("Overall duration = " + totalDuration + "\n");
         writer.print("Number of runs that stopped early = " + earlyStoppingCounter + " ; Number of runs with no more feasible solutions = " + noMoreFeasibleSolutionsCounter);
         writer.close();
         logWriter.close();
      }
   }

   private static String setFileName(int id, String resultsFile) {
      String timeout = timeoutToStr(timeoutType);

      resultsFile = resultsFile + optimizer + "_timeout" + timeout + "_earlyStop" + earlyStop + "_initSamples" + initialTrainingSamples + "_budget" + budget + "_lookahead" + horizon + "_gamma" + gamma + "_gh" + gh + "_maxTime" + max_time_perc + "_bootstrapMethod_" + bootstrap + "_nUses_" + Lynceus.nUses;

      if (target.equals(test.TENSORFLOW)) {   // tensorflow has the acc parameter that the others don't have
         resultsFile = resultsFile + "_accThreshold" + acc + "_wkld" + file;
      } else {
         resultsFile = resultsFile + "_wkld" + file;
      }

      return resultsFile + ".txt";
   }

   /*
   Optimizer timeout budget workload [wkld_param (acc or dataset)] threads horizon numInitSamples gamma max_time_perc gh numSeeds bootstrapMethod numUses earlyStopOption
    */

   private static Lynceus.optimizer fromString(String text) {
      for (Lynceus.optimizer b : Lynceus.optimizer.values()) {
         if (b.toString().equalsIgnoreCase(text)) {
            return b;
         }
      }
      throw new RuntimeException("Optimizer not recognized "+text);
   }

   private static void parseArgs(String[] args) {
      String input;
      int i = 0;
      input = args[i++];
      int searchSpaceSize = 1;
      int percentage_initialTrainingSamples;

      optimizer = fromString(input);


      System.out.println("Main: Optimizer " + optimizer);

      input = args[i++];
      timeoutType = strToTimeout(input);
      Lynceus.setTIMEOUT(timeoutType);


      input = args[i++];
      double[] allowedBudgets = {1, 1.25, 1.5, 1.75, 2, 3, 4, 5, 6, 8, 10, 15, 20, 30, 40, 60, 100, 1000, 1000000};
      boolean budgetSet = false;
      int j = 0;

      if (input.equalsIgnoreCase("all")) {
         allBudgets = true;
      } else {
         allBudgets = false;
         budget = Double.parseDouble(input);
         while (!budgetSet && j < allowedBudgets.length) {
            if (budget == allowedBudgets[j]) {
               budget = allowedBudgets[j];
               budgetSet = true;
            }
            j++;
         }
         if (!budgetSet) {
            budget = 10;
         }
      }

      System.out.println("Budget  = " + (allBudgets ? "all" : budget));

      input = args[i++];
      int numOfDimensionsOfTheConfig;
      
      if (input.equalsIgnoreCase("scout")) {
         target = test.SCOUT;
         input = args[i++];
         WKLD_ID = Integer.parseInt(input);
         file = "SCOUT" + input;
         ScoutVMCostGenerator.setTargetWkld(Integer.parseInt(input));
         WekaScoutVMConfigFactory.setTargetWkld(Integer.parseInt(input));
         searchSpaceSize = 69;
         numOfDimensionsOfTheConfig = 6;
      } else if (input.equalsIgnoreCase("cherrypick")) {
         target = test.CHERRYPICK;
         input = args[i++];
         WKLD_ID = Integer.parseInt(input);
         file = "CP_" + input;
         CherrypickConfigCostGenerator.setTargetWkld(Integer.parseInt(input));
         WekaCherrypickConfigFactory.setTargetWkld(Integer.parseInt(input));
         CherrypickLHS.setTargetWkld(Integer.parseInt(input));
         numOfDimensionsOfTheConfig = 9;
         
         int dataset = Integer.parseInt(input);
         if (dataset == 1) {   // kmeans wkld
            searchSpaceSize = 47;
         } else if (dataset == 2) {   // spark wkld
            searchSpaceSize = 59;
         } else if (dataset == 3) {   // tersort wkld
            searchSpaceSize = 72;
         } else if (dataset == 4) {   // tpcds wkld
            searchSpaceSize = 61;
         } else {	// tpch wkld
            searchSpaceSize = 52;
         }
         
       } else if (input.equalsIgnoreCase("tensorflow")) {
         target = test.TENSORFLOW;
         input = args[i++];
         searchSpaceSize = 384;
         if (Integer.parseInt(input) == 1) {   // t2_multilayer
            file = "t2_multilayer_intermediate_values";
         } else if (Integer.parseInt(input) == 2) {   // t2_rnn
            file = "t2_rnn";
         } else {   // t2_cnn
            file = "t2_cnn_intermediate_values";
         }
         numOfDimensionsOfTheConfig = 6;

         datasetFile = "files/" + file + ".csv";
         System.out.println(datasetFile);
         WekaTensorflowConfigLynceus.setDatasetFile(datasetFile);

         acc = 0.0;
         System.out.println("Acc " + acc);

         TensorflowConfigCostGenerator.setAccThreshold(acc);


      } else {
         System.out.println("\nChoosing default workload: Tensorflow");
         target = test.TENSORFLOW;
         numOfDimensionsOfTheConfig = 6;
      }

      input = args[i++];

      int threads = Integer.parseInt(input);
      if (threads <= 0) {
         //These are already hardware cores.
         threads = (int) (((double) Runtime.getRuntime().availableProcessors()) * 1.5D);

      }
      
      System.out.println("Threads " + threads);

      Lynceus.setTHREADS(threads);

      input = args[i++];

      horizon = Integer.parseInt(input);
      if (horizon < 0) {      //We allow zero-horizon to fall back to a cherrypick-like  optimizer
         horizon = 2;
      }

      System.out.println("Horizon " + horizon);

      input = args[i++];


      percentage_initialTrainingSamples = Integer.parseInt(input);
      if (searchSpaceSize != 1) {
    	 int threePercentSearchSpace = (int) Math.round(percentage_initialTrainingSamples / 100.0 * searchSpaceSize);
         initialTrainingSamples = Math.max(threePercentSearchSpace, numOfDimensionsOfTheConfig);
      } else {
         initialTrainingSamples = percentage_initialTrainingSamples;
      }
      if (initialTrainingSamples <= 0) {
         initialTrainingSamples = 5;
      }
      Lynceus.setInitTrainingSamples(initialTrainingSamples);
      System.out.println("TrainSample " + initialTrainingSamples);


      input = args[i++];
      gamma = Double.parseDouble(input);
      System.out.println("Gamma " + gamma);

      input = args[i++];
      max_time_perc = Double.parseDouble(input) / 100D;
      System.out.println("Max Perc " + max_time_perc);

      input = args[i++];
      gh = Long.parseLong(input);
      Lynceus.setGaussHermitePoints((int) gh);
      System.out.println("gh " + gh);

      input = args[i++];
      numSeeds = Integer.parseInt(input);
      System.out.println("numSeeds " + numSeeds);

      bootstrap = args[i++];
      System.out.println("bootstrap method " + bootstrap);
   }


   private static void printResults(HashMap<ExpParam, OptResult> hL, Lynceus.optimizer opt, double b, double e,
                                    long id, long[] seeds, PrintWriter writer, int budgetIndex, double[] duration, double[] avgSeedDuration, double actualBudget, PrintWriter logWriter, ArrayList<ArrayList<Double>> seedDurations) {
      
      double avgDist = 0, avgExpl = 0, avgCost = 0, avgInitDist = 0, avgError = 0, avgStdv = 0, avgExplToOpt = 0, avgtimeExp = 0, avgOptCost = 0, avgTotalCost = 0;
      double p99Dist = -1, p99Expl = -1, p99Cost = -1, p99InitDist = -1, p99Error = -1, p99ExplToOpt = -1, p99timeExp = -1, p99optCost = -1, p99TotalCost = -1;
      double p90Dist = -1, p90Expl = -1, p90Cost = -1, p90InitDist = -1, p90Error = -1, p90ExplToOpt = -1, p90timeExp = -1, p90optCost = -1, p90TotalCost = -1;
      double p50Dist = -1, p50Expl = -1, p50Cost = -1, p50InitDist = -1, p50Error = -1, p50ExplToOpt = -1, p50timeExp = -1, p50optCost = -1, p50TotalCost = -1;
     
      int i = 0;

      double withinTMax = 0;

      /* check how many results comply with the constraint */
      for (long s : seeds) {
         ExpParam expParam = new ExpParam(b, e, s, id, opt);
         OptResult o = hL.get(expParam);
         if (o == null) {
            continue;
         }
         if (o.isWithinTmax()) {
            withinTMax++;
         }
      }

      double[] dists = new double[(int) withinTMax];
      double[] expls = new double[(int) withinTMax];
      double[] costs = new double[(int) withinTMax];
      double[] init_dists = new double[(int) withinTMax];
      double[] estimationErrors = new double[(int) withinTMax];
      double[] numTimeouts = new double[(int) withinTMax];
      double[] stdvs = new double[(int) withinTMax];
      double[] explsToOpt = new double[(int) withinTMax];
      double[] timeExp = new double[(int) withinTMax];
      double[] optCost = new double[(int) withinTMax];
      double[] totalCosts = new double[(int) withinTMax];
      
      ArrayList<Double> exec_times = new ArrayList<Double>();
      exec_times = seedDurations.get(budgetIndex);
      
      double totalOverallCost; 
      
      logWriter.println("system;wkld;budget;bootstrapMethod;initSamples;nUses;lookahead;gamma;gh;timeout;earlyStop;numSeeds;seed;maxTimePerc;optCostFound;dfo;nex;totalExplorationCost;totalOverallCost;estimationError;numTimeouts;exec_time;init_DFO;nexToOpt;ExpTime");
      String timeout = timeoutToStr(timeoutType);
            
      for (long s : seeds) {
         ExpParam expParam = new ExpParam(b, e, s, id, opt);
         OptResult o = hL.get(expParam);
         if (o == null) {
            continue;
         }
         if (o.isWithinTmax()) {
            dists[i] = o.getDistFromOpt();
            expls[i] = o.getExplNumExpls();
            costs[i] = o.getExplCost();
            init_dists[i] = o.getInitDistFromOpt();
           	estimationErrors[i] = o.getEstimationError();
            numTimeouts[i] = o.getNumTimeouts();
            explsToOpt[i] = o.getNexToOpt();
            timeExp[i] = o.getExperimentTime()/60.0; // timeExp[i] is in minutes
            optCost[i] = o.getOptCost();
            totalCosts[i] = costs[i] + optCost[i] * Lynceus.nUses;
            
            avgDist += o.getDistFromOpt();
            avgExpl += o.getExplNumExpls();
            avgCost += o.getExplCost();
            avgInitDist += o.getInitDistFromOpt();
            avgError += o.getEstimationError();
            avgExplToOpt += o.getNexToOpt();
            avgtimeExp += o.getExperimentTime()/60.0; // avgtimeExp is in minutes
            avgOptCost += o.getOptCost(); 
            avgTotalCost += costs[i] + optCost[i] * Lynceus.nUses;
            totalOverallCost = costs[i] + optCost[i] * Lynceus.nUses;
            logWriter.println(opt + ";" + file + ";" + b + ";" + bootstrap + ";" + initialTrainingSamples + ";" + Lynceus.nUses + ";" + horizon + ";" + gamma + ";" + gh + ";" + timeout + ";" + earlyStop + ";" + numSeeds + ";" + s + ";" + max_time_perc + ";" + optCost[i] + ";" + dists[i] + ";" + expls[i] + ";" + costs[i] + ";" + totalOverallCost + ";" + estimationErrors[i] + ";" + numTimeouts[i] + ";" + exec_times.get(i) + ";" + init_dists[i] + ";" + explsToOpt[i] + ";" +  timeExp[i]);
            i++;
         }
      }

      if (dists.length > 0) {
	      avgDist /= (int) withinTMax;
	      avgCost /= (int) withinTMax;
	      avgExpl /= (int) withinTMax;
	      avgInitDist /= (int) withinTMax;
	      avgError /= (int) withinTMax;
	      avgExplToOpt /= (int) withinTMax;
	      avgtimeExp /= (int) withinTMax;
	      avgOptCost /= (int) withinTMax;
	      avgTotalCost /= (int) withinTMax;;

	      Arrays.sort(dists);
	      Arrays.sort(expls);
	      Arrays.sort(costs);
	      Arrays.sort(init_dists);
	      Arrays.sort(estimationErrors);
	      Arrays.sort(explsToOpt);
	      Arrays.sort(timeExp);
	      Arrays.sort(optCost);
	      Arrays.sort(totalCosts);
	      
	      // this needs to be computed here instead of in the previous
	      // loop because the avgDist is necessary to compute the stdv
	      i = 0;
	      for (long s : seeds) {
	         ExpParam expParam = new ExpParam(b, e, s, id, opt);
	         OptResult o = hL.get(expParam);
	         if (o == null) {
	            continue;
	         }
	         if (o.isWithinTmax()) {
	            stdvs[i] = Math.pow(o.getDistFromOpt() - avgDist, 2);
	            avgStdv += stdvs[i];
	            i++;
	         }
	      }
	
	      avgStdv /= (int) withinTMax;
	      avgStdv = Math.sqrt(avgStdv);
	
	      int index_99 = (int) Math.floor((99.0 / 100) * ((double) withinTMax));
	      if (index_99 == (int) withinTMax)
	         index_99 = (int) withinTMax - 2; //not the max
	      p99Cost = costs[index_99];
	      p99Expl = expls[index_99];
	      p99Dist = dists[index_99];
	      p99InitDist = init_dists[index_99];
	      p99Error = estimationErrors[index_99];
	      p99ExplToOpt = explsToOpt[index_99];
	      p99timeExp = timeExp[index_99];
	      p99optCost = optCost[index_99];
	      p99TotalCost = totalCosts[index_99];
	
	      int index_90 = (int) Math.floor((90.0 / 100) * ((double) withinTMax));
	      if (index_90 == (int) withinTMax)
	         index_90 = (int) withinTMax - 2; //not the max
	      p90Cost = costs[index_90];
	      p90Expl = expls[index_90];
	      p90Dist = dists[index_90];
	      p90InitDist = init_dists[index_90];
	      p90Error = estimationErrors[index_90];
	      p90ExplToOpt = explsToOpt[index_90];
	      p90timeExp = timeExp[index_90];
	      p90optCost = optCost[index_90];
	      p90TotalCost = totalCosts[index_90];
	
	      int index_50 = (int) Math.floor((50.0 / 100) * ((double) withinTMax));
	      if (index_50 == (int) withinTMax)
	         index_50 = (int) withinTMax - 2; //not the max	
	      p50Cost = costs[index_50];
	      p50Expl = expls[index_50];
	      p50Dist = dists[index_50];
	      p50InitDist = init_dists[index_50];
	      p50Error = estimationErrors[index_50];
	      p50ExplToOpt = explsToOpt[index_50];
	      p50timeExp = timeExp[index_50];
	      p50optCost = optCost[index_50];
	      p50TotalCost = totalCosts[index_50];
      }

      String dfos = dfos(hL, opt, b, e, id, seeds);


      writeResults(writer, opt, id, b, actualBudget, e, avgDist, avgtimeExp, avgExpl, avgCost, p50Dist, p50timeExp, p50Expl, p50Cost, p90Dist, p90timeExp, p90Expl, p90Cost, p99Dist, p99timeExp, p99Expl, p99Cost, withinTMax, seeds.length, budgetIndex, duration, avgSeedDuration, file, acc, target, horizon, gamma, gh, dfos, avgInitDist, p50InitDist, p90InitDist, p99InitDist, avgError, p50Error, p90Error, p99Error, avgStdv, p50ExplToOpt, p90ExplToOpt, p99ExplToOpt, avgExplToOpt, avgOptCost, p50optCost, p90optCost, p99optCost, avgTotalCost, p50TotalCost, p90TotalCost, p99TotalCost);
      
      System.out.println("\n" + opt + ";" + id + ";" + b + ";" + actualBudget + ";" + e + ";" + avgDist + ";" + avgExpl + ";" + avgCost + ";" + avgError + ";" + p50Dist + ";" + p50Expl + ";" + p50Cost + ";" + p50Error + ";" + p90Dist + ";" + p90Expl + ";" + p90Cost + ";" + p90Error + ";" + p99Dist + ";" + p99Expl + ";" + p99Cost + ";" + p99Error + "; " + withinTMax + "; " + seeds.length);
   }

   /* print results to the output result's file */
   private static void writeResults(PrintWriter writer, Lynceus.optimizer opt, long id, double budget, double actualBudget, double e, double avgDist, double avgtimeExp, double avgExpl, double avgCost, double p50Dist, double p50timeExp, double p50Expl, double p50Cost, double p90Dist, double p90timeExp, double p90Expl, double p90Cost, double p99Dist, double p99timeExp, double p99Expl, double p99Cost, double withinTMax, int numSeeds, int budgetIndex, double[] duration, double[] avgSeedDuration, String file, double acc, test job, double horizon, double gamma, long gh, String dfos, double avgInitDist, double p50InitDist, double p90InitDist, double p99InitDist, double avgError, double p50Error, double p90Error, double p99Error, double avgStdv, double p50ExplToOpt, double p90ExplToOpt, double p99ExplToOpt, double avgExplToOpt, double avgOptCost, double p50optCost, double p90optCost, double p99optCost, double avgTotalCost, double p50TotalCost, double p90TotalCost, double p99TotalCost) {

      if (target == test.SCOUT)
         id = (long) WKLD_ID;

      String timeout = timeoutToStr(timeoutType);

      String line = opt + ";wkldID=" + id + ";budget=" + budget + ";money=" + actualBudget + ";e=" + e + ";A-DFO=" + avgDist + ";A-ExpTime=" + avgtimeExp +";A-NEX=" + avgExpl + ";A-CEX=" + avgCost + ";A-STDV=" + avgStdv + ";50-DFO=" + p50Dist + ";50-ExpTime=" + p50timeExp +";50-NEX=" + p50Expl + ";50-CEX=" + p50Cost + ";90-DFO=" + p90Dist + ";90-ExpTime=" + p90timeExp +";90-NEX=" + p90Expl + ";90-CEX=" + p90Cost + ";99-DFO=" + p99Dist + ";99-ExpTime=" + p99timeExp +";99-NEX= " + p99Expl + ";99-CEX=" + p99Cost + ";constraint=" + withinTMax + ";numSeeds=" + numSeeds + ";duration=" + duration[budgetIndex] + ";avgSeedDuration=" + avgSeedDuration[budgetIndex] + ";wkld=" + file + ";timeout=" + timeout + ";acc=" + acc + ";job=" + job + ";horizon=" + horizon + ";gamma=" + gamma + ";gh=" + gh + ";max_time=" + max_time + ";max_time_perc=" + max_time_perc + ";num_init_samples=" + initialTrainingSamples + ";" + dfos + ";A-EERR=" + avgError + ";50-EERR=" + p50Error + ";90-EERR=" + p90Error + ";99-EERR=" + p99Error + ";A-initDFO=" + avgInitDist + ";50-initDFO=" + p50InitDist + ";90-initDFO=" + p90InitDist + ";99-initDFO=" + p99InitDist + ";bootstrapMethod=" + bootstrap + ";AVG-NEX-TO-OPT=" + avgExplToOpt + ";50-NEX-TO-OPT=" + p50ExplToOpt + ";90-NEX-TO-OPT=" + p90ExplToOpt + ";99-NEX-TO-OPT=" + p99ExplToOpt + ";AVG-OptCostFound=" + avgOptCost + ";50-OptCostFound=" + p50optCost + ";90-OptCostFound=" + p90optCost + ";99-OptCostFound=" + p99optCost + ";nUses=" + Lynceus.nUses + ";earlyStop=" + earlyStop + ";AVG-TotalCost=" + avgTotalCost + ";50-TotalCost=" + p50TotalCost + ";90-TotalCost=" + p90TotalCost + ";99-TotalCost=" + p99TotalCost + "\n ";
      writer.println(line);
   }

   /* return a string with the stats considering all seeds, regardless of the constraint */
   private static String dfos(HashMap<ExpParam, OptResult> hL, Lynceus.optimizer opt, double b, double e, long id, long[] seeds) {
	  double avgDist = 0, avgExpl = 0, avgCost = 0, avgInitDist = 0, avgError = 0, avgStdv = 0, avgExplToOpt = 0, avgtimeExp = 0, avgOptCost = 0, avgTotalCost = 0;
      double p99Dist = -1, p99Expl = -1, p99Cost = -1, p99InitDist = -1, p99Error = -1, p99ExplToOpt = -1, p99timeExp = -1, p99optCost = -1, p99TotalCost = -1;
      double p90Dist = -1, p90Expl = -1, p90Cost = -1, p90InitDist = -1, p90Error = -1, p90ExplToOpt = -1, p90timeExp = -1, p90optCost = -1, p90TotalCost = -1;
      double p50Dist = -1, p50Expl = -1, p50Cost = -1, p50InitDist = -1, p50Error = -1, p50ExplToOpt = -1, p50timeExp = -1, p50optCost = -1, p50TotalCost = -1;
      int i = 0;

      double[] dists = new double[seeds.length];
      double[] expls = new double[seeds.length];
      double[] costs = new double[seeds.length];
      double[] init_dists = new double[seeds.length];
      double[] estimationError = new double[seeds.length];
      double[] stdvs = new double[seeds.length];
      double[] explsToOpt = new double[seeds.length];
      double[] timeExp = new double[seeds.length];
      double[] optCosts = new double[seeds.length];
      double[] totalCosts = new double[seeds.length];


      for (long s : seeds) {
         ExpParam expParam = new ExpParam(b, e, s, id, opt);
         OptResult o = hL.get(expParam);
         if (o == null) {
            continue;
         }
         dists[i] = o.getDistFromOpt();
         expls[i] = o.getExplNumExpls();
         costs[i] = o.getExplCost();
         init_dists[i] = o.getInitDistFromOpt();
         estimationError[i] = o.getEstimationError();
         explsToOpt[i] = o.getNexToOpt();
         timeExp[i] = o.getExperimentTime()/60.0; // timeExp[i] is in minutes
         optCosts[i] = o.getOptCost();
         totalCosts[i] = costs[i] + optCosts[i] * Lynceus.nUses;
         
         avgDist += o.getDistFromOpt();
         avgExpl += o.getExplNumExpls();
         avgCost += o.getExplCost();
         avgInitDist += o.getInitDistFromOpt();
         avgError += o.getEstimationError();
         avgExplToOpt += o.getNexToOpt();
         avgtimeExp += o.getExperimentTime()/60.0; // avgtimeExp is in minutes
         avgOptCost += o.getOptCost();
         avgTotalCost += costs[i] + optCosts[i] * Lynceus.nUses;

         i++;
      }
      
      if (dists.length == 0)
         return "ALL_A-DFO=" + avgDist + ";ALL_A-NEX=" + avgExpl + ";ALL_A-CEX=" + avgCost + ";ALL_50-DFO=" + p50Dist + ";ALL_50-NEX=" + p50Expl + ";ALL_50-CEX=" + p50Cost + ";ALL_90-DFO=" + p90Dist + ";ALL_90-NEX=" + p90Expl + ";ALL_90-CEX=" + p90Cost + ";ALL_99-DFO=" + p99Dist + ";ALL_99-NEX= " + p99Expl + ";ALL_99-CEX=" + p99Cost + ";ALL_A-EERR=" + avgError + ";ALL_50-EERR=" + p50Error + ";ALL_90-EERR=" + p90Error + ";ALL_99-EERR=" + p99Error + ";ALL_A-INIT-DFO=" + avgInitDist + ";ALL_50-INIT-DFO=" + p50InitDist + ";ALL_90-INIT-DFO=" + p90InitDist + ";ALL_99-INIT-DFO=" + p99InitDist + ";ALL_AVG-NEX-TO-OPT=" + avgExplToOpt + ";ALL_50-NEX-TO-OPT=" + p50ExplToOpt + ";ALL_90-NEX-TO-OPT=" + p90ExplToOpt + ";ALL_99-NEX-TO-OPT=" + p99ExplToOpt + ";ALL_A-ExpTime=" + avgtimeExp + ";ALL_50-ExpTime=" + p50timeExp+ ";ALL_90-ExpTime=" + p90timeExp + ";ALL_99-ExpTime=" + p99timeExp + ";ALL_A-OptCostFound=" + avgOptCost + ";ALL_50-OptCostFound=" + p50optCost + ";ALL_90-OptCostFound=" + p90optCost + ";ALL_99-OptCostFound=" + p99optCost + ";ALL_A-TotalCost=" + avgTotalCost + ";ALL_50-TotalCost=" + p50TotalCost + ";ALL_90-TotalCost=" + p90TotalCost + ";ALL_99-TotalCost=" + p99TotalCost;
      
      avgDist /= seeds.length;
      avgCost /= seeds.length;
      avgExpl /= seeds.length;
      avgInitDist /= seeds.length;
      avgError /= seeds.length;
      avgExplToOpt /= seeds.length;
      avgtimeExp /= seeds.length;
      avgOptCost /= seeds.length;
      avgTotalCost /= seeds.length;
      
      Arrays.sort(dists);
      Arrays.sort(expls);
      Arrays.sort(costs);
      Arrays.sort(init_dists);
      Arrays.sort(estimationError);
      Arrays.sort(explsToOpt);
      Arrays.sort(timeExp);
      Arrays.sort(optCosts);
      Arrays.sort(totalCosts);

      i = 0;
      for (long s : seeds) {
         ExpParam expParam = new ExpParam(b, e, s, id, opt);
         OptResult o = hL.get(expParam);
         if (o == null) {
            continue;
         }
         stdvs[i] = Math.pow(o.getDistFromOpt() - avgDist, 2);
         avgStdv += stdvs[i];
         i++;
      }
      avgStdv /= (int) seeds.length;
      avgStdv = Math.sqrt(avgStdv);

      int index_99 = (int) Math.floor((99.0 / 100) * ((double) seeds.length));
      if (index_99 == seeds.length)
         index_99 = seeds.length - 2; //not the max

      p99Cost = costs[index_99];
      p99Expl = expls[index_99];
      p99Dist = dists[index_99];
      p99InitDist = init_dists[index_99];
      p99Error = estimationError[index_99];
      p99ExplToOpt = explsToOpt[index_99];
      p99timeExp = timeExp[index_99];
      p99optCost = optCosts[index_99];
      p99TotalCost = totalCosts[index_99];

      int index_90 = (int) Math.floor((90.0 / 100) * ((double) seeds.length));
      if (index_90 == seeds.length)
         index_90 = seeds.length - 2; //not the max

      p90Cost = costs[index_90];
      p90Expl = expls[index_90];
      p90Dist = dists[index_90];
      p90InitDist = init_dists[index_90];
      p90Error = estimationError[index_90];
      p90ExplToOpt = explsToOpt[index_90];
      p90timeExp = timeExp[index_90];
      p90optCost = optCosts[index_90];
      p90TotalCost = totalCosts[index_90];

      int index_50 = (int) Math.floor((50.0 / 100) * ((double) seeds.length));
      if (index_50 == seeds.length)
         index_50 = seeds.length - 2; //not the max

      p50Cost = costs[index_50];
      p50Expl = expls[index_50];
      p50Dist = dists[index_50];
      p50InitDist = init_dists[index_50];
      p50Error = estimationError[index_50];
      p50ExplToOpt = explsToOpt[index_50];
      p50timeExp = timeExp[index_50];
      p50optCost = optCosts[index_50];
      p50TotalCost = totalCosts[index_50];


      return "ALL_A-DFO=" + avgDist + ";ALL_A_STDV=" + avgStdv + ";ALL_A-NEX=" + avgExpl + ";ALL_A-CEX=" + avgCost + ";ALL_50-DFO=" + p50Dist + ";ALL_50-NEX = " + p50Expl + ";ALL_50-CEX=" + p50Cost + ";ALL_90-DFO=" + p90Dist + ";ALL_90-NEX=" + p90Expl + ";ALL_90-CEX=" + p90Cost + ";ALL_99-DFO=" + p99Dist + ";ALL_99-NEX= " + p99Expl + ";ALL_99-CEX=" + p99Cost + ";ALL_A-EERR=" + avgError + ";ALL_50-EERR=" + p50Error + ";ALL_90-EERR=" + p90Error + ";ALL_99-EERR=" + p99Error + ";ALL_A-INIT-DFO=" + avgInitDist + ";ALL_50-INIT-DFO=" + p50InitDist + ";ALL_90-INIT-DFO=" + p90InitDist + ";ALL_99-INIT-DFO=" + p99InitDist + ";ALL_AVG-NEX-TO-OPT=" + avgExplToOpt + ";ALL_50-NEX-TO-OPT=" + p50ExplToOpt + ";ALL_90-NEX-TO-OPT=" + p90ExplToOpt + ";ALL_99-NEX-TO-OPT=" + p99ExplToOpt + ";ALL_A-ExpTime=" + avgtimeExp + ";ALL_50-ExpTime=" + p50timeExp + ";ALL_90-ExpTime=" + p90timeExp + ";ALL_99-ExpTime=" + p99timeExp + ";ALL_A-OptCostFound=" + avgOptCost + ";ALL_50-OptCostFound=" + p50optCost + ";ALL_90-OptCostFound=" + p90optCost + ";ALL_99-OptCostFound=" + p99optCost + ";ALL_A-TotalCost=" + avgTotalCost + ";ALL_50-TotalCost=" + p50TotalCost + ";ALL_90-TotalCost=" + p90TotalCost + ";ALL_99-TotalCost=" + p99TotalCost;
   }

}
