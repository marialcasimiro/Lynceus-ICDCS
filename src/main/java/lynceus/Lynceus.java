package lynceus;

import gh.GaussHermiteParams;
import gh.NormalGaussHermiteQuadrature;
import lynceus.Main.timeout;
import lynceus.results.OptResult;
import org.apache.commons.math3.distribution.NormalDistribution;
import regression.LinearRegression;
import weka.AbstractConfigWekaTestSet;
import weka.WekaGaussianProcess;
import weka.core.Instance;
import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.NominalToBinary;
import weka.gauss.CustomGP;
import weka.tuning.ModelParams;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;


/**
 * @author Diego Didona
 * @email diego.didona@epfl.ch
 * @since 11.03.18
 */

public abstract class Lynceus<C extends Configuration, M extends ModelSample> {

   private static boolean log_transform = false;

   static {
      if (log_transform) {
         throw new RuntimeException("No log transform for now");
      }
      System.out.println("log_transform is " + log_transform);
   }

   protected CostGenerator<C> costGenerator;
   public static int initTrainSamples = 10;
   private static int gaussHermitePoints = 5;
   private static double discreteBudget;
   private static double maxTimePerc;
   private final double z_99 = 2.326348;   // x = mu + z s  for the percentile of a normal distribution
   private static double T_max = 500;      // Maximum acceptable runtime for the target job
   protected TestSet<C, M> testSet;
   private long horizon;               // In the lookahead
   private double budget;               // Monetary budget to run the exploration
   private double cumulativeCost;         // Cumulative monetary cost of the exploration phase
   private long cumulativeExplorations;     // Number of explorations done during the exploration phase
   private double epsilon_utility;         // In percentage w.r.t. last one

   private double gamma; // Discount factor for future rewards: 0 = only consider immediate reward; 1 = long-term reward has the same value as short term reward
   private double delta; // Parameter for the UCB BO. It is set to be equal to gamma (which is not used with UCB)

   private double maxCost = Double.NEGATIVE_INFINITY;   // Max cost to run the job until completion seen so far

   private final static boolean _trace_eval_ = false;

   private static int THREADS;

   private int consecutiveAcceptableUtilities = 0;
   private final static int minAcceptableUtilities = 2;

   private final optimizer opt_type;

   private final long wkldid;

   private long searchSpaceCardinality = -1;

   private LHS<C> lhs;

   private final static boolean lyn_print = false;

   private static PrintWriter debugWriter = null;
   private static boolean debug_logs;

   private int stdv_counter;
   private int stdv_total_counter;

   private static timeout timeoutType;

   private static PrintWriter estimationErrorsWriter = null;
   private static boolean estimationErrors_logs;
   private long timeoutCounter;   // number of times an exploration is timed out. This should be equal to the length of the estimationErrors array
//   ArrayList<double> estimationErrors = new ArrayList<double>();   // estimation errors of the linear model (for the timeout policy) that
   
   /* estimation errors of the linear model (for the timeout policy) that
   /* predicts the costs of running a job until completion
    * The Pair<> contains the estimation error and the corresponding exploration, respectively
    * The Triple<> contains the estimated, real and execution costs, respectively
    */
   private ArrayList<Pair<Pair<Double, Long>, Triple<Double, Double, Double>>> estimationErrors = new ArrayList<>();
   private ArrayList<C> estimationErrorsConfigs = new ArrayList<C>();
   private int nexToOpt;	// number of explorations performed until the config that is returned was found 
   private double currDFO;	// keep track of the current and next DFOs to see if a better 
   private double nextDFO;	// configuration has been found
   
   private static boolean runLogs;
   private static PrintWriter runLogsWriter = null;
   
   private double timeExp = 0.0; // in seconds --- How long a full run of the algorithm takes (including cloud execution times)
   
   /* these variables are for the runLogs, for each exploration */
   private double avgMu = 0;
   private double mu50 = -1;
   private double mu90 = -1;
   private double avgSigma = 0;
   private double sigma50 = -1;
   private double sigma90 = -1;
   private double avgEI = 0;
   private double ei50 = -1;
   private double ei90 = -1;

   /* Early stopping */
   static int nUses;		// expected number of times the model will be used
   private static double bTotal;	// total budget the user is willing to spend to search for the best config and to use it nUses times
   private final earlyStopOptions es_type;


   static void setGaussHermitePoints(int gaussHermitePoints) {
      Lynceus.gaussHermitePoints = gaussHermitePoints;
   }

   static void setT_max(double t) {
      T_max = t;
      if (t == 0) {
         throw new RuntimeException("Max time cannot be zero");
      }
   }

   public enum optimizer {
      LYNCEUS, RAND
   }

   public enum earlyStopOptions {
	   EI_LESS_THAN_10_PERCENT, EI_LESS_THAN_1_PERCENT, PROTEUS_TM, MIXED, IMPR_LESS_THAN_10_PERCENT, OUR_ES, OUR_ES_3, NO_ES
   }
   
   public Lynceus(long h, double b, double epsilon, double gamma, optimizer opt, long wkldid, earlyStopOptions es) {
      this.horizon = h;
      this.budget = b;
      this.cumulativeCost = 0;
      this.cumulativeExplorations = 0;
      this.costGenerator = buildCostGenerator(h);
      this.lhs = instantiateLHS(initTrainSamples);
      this.epsilon_utility = epsilon;
      this.gamma = gamma;
      this.delta = gamma;
      this.opt_type = opt;
      this.wkldid = wkldid;
      this.timeoutCounter = 0;
      this.es_type = es;

      this.stdv_counter = 0;
      this.stdv_total_counter = 0;

      this.estimationErrors.clear();

      this.nexToOpt = -1;
      this.currDFO = Double.POSITIVE_INFINITY;	// keep track of the current and next DFOs to see if a better 
      this.nextDFO = Double.POSITIVE_INFINITY;
      
      this.timeExp = 0.0;
   }

   
   public static void setDebugLogs() {
      debug_logs = true;
      String timeout = Main.timeoutToStr(timeoutType);

      String file = "files/debug/" + Main.optimizer + "_timeout" + timeout + "_earlyStop" + Main.earlyStop + "_bootstrapMethod_" + Main.bootstrap + "_initSamples" + initTrainSamples + "_budget" + Main.budget + "_lookahead" + Main.horizon + "_gamma" + Main.gamma + "_gh" + gaussHermitePoints + "_maxTime" + Main.max_time_perc + "_" + "nUses" + nUses + "_" + Main.file + ".txt";
      File f = new File(file);
      if (f.exists()) {
         f.renameTo(new File(f.getAbsolutePath().concat(".").concat(String.valueOf(System.currentTimeMillis())).concat(".txt")));
      }
      try {
         debugWriter = new PrintWriter(file, "UTF-8");
      } catch (FileNotFoundException | UnsupportedEncodingException e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }
   }

   
   static void setEstimationErrorsLogs() {
      estimationErrors_logs = true;
      String timeout = Main.timeoutToStr(timeoutType);

      String file = "files/estimationErrors/" + Main.optimizer + "_timeout" + timeout + "_earlyStop" + Main.earlyStop + "_bootstrapMethod_" + Main.bootstrap + "_initSamples" + initTrainSamples + "_budget" + Main.budget + "_lookahead" + Main.horizon + "_gamma" + Main.gamma + "_gh" + gaussHermitePoints + "_maxTime" + Main.max_time_perc + "_" + "nUses" + nUses + "_" + Main.file + ".txt";
      File f = new File(file);
      if (f.exists()) {
         f.renameTo(new File(f.getAbsolutePath().concat(".").concat(String.valueOf(System.currentTimeMillis())).concat(".txt")));
      }
      try {
         estimationErrorsWriter = new PrintWriter(file, "UTF-8");
      } catch (FileNotFoundException | UnsupportedEncodingException e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }
      estimationErrorsWriter.println("wkld;optimizer;budget;bootstrapMethod;initSamples;nUses;timeout;earlyStop;numSeeds;seed;lookahead;gamma;gh;maxTimePerc;estimationError;exploration;modelCost;realCost;executionCost;costPerTimeUnit");
      estimationErrorsWriter.flush();
   }

   
   static void setRunLogs(long seed) {
	   runLogs = true;
      String timeout = Main.timeoutToStr(timeoutType);

      String file = "files/runLogs/" + "runID_" + seed + "_numSeeds_" + Main.numSeeds + "_optimizer_" + Main.optimizer + "_timeout" + timeout + "_earlyStop" + Main.earlyStop + "_bootstrapMethod_" + Main.bootstrap + "_initSamples" + initTrainSamples + "_budget" + Main.budget + "_lookahead" + Main.horizon + "_gamma" + Main.gamma + "_gh" + gaussHermitePoints + "_maxTime" + Main.max_time_perc + "_" + "nUses" + nUses + "_" + Main.file + ".txt";
      File f = new File(file);
      if (f.exists()) {
         f.renameTo(new File(f.getAbsolutePath().concat(".").concat(String.valueOf(System.currentTimeMillis())).concat(".txt")));
      }
      try {
         runLogsWriter = new PrintWriter(file, "UTF-8");
      } catch (FileNotFoundException | UnsupportedEncodingException e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }
      runLogsWriter.println("runID;wkld;optimizer;budget;bootstrapMethod;initSamples;nUses;timeout;earlyStop;lookahead;gamma;gh;maxTimePerc;explorationNumber;config;DFO(config);currBestDFO;EIc;MU;SIGMA;LowestMu;EIcOfLowestMu;configOfLowestMu;avgMu;avgSigma;avgEI;mu50;sigma50;ei50;mu90;sigma90;ei90");
      runLogsWriter.flush();
   }


   static void setTIMEOUT(timeout type) {
      timeoutType = type;
   }


   static void setTHREADS(int tHREADS) {
      THREADS = tHREADS;
   }


   static void setInitTrainingSamples(int initSamples) {
      initTrainSamples = initSamples;
   }


   static void setDiscreteBudget(double budget) {
      discreteBudget = budget;
   }


   static void setMaxTimePerc(double timePerc) {
      maxTimePerc = timePerc;
   }

   

   /**
    * Set total budget for finding the optimal config and for
    * using the model nUses times
    * @param totalBudget
    */
   static void setTotalBudget(double totalBudget) {
	   bTotal = totalBudget;
   }
   
   @Override
   public String toString() {
      return "{" +
            "initTrainSamples=" + initTrainSamples +
            ", gaussHermitePoints=" + gaussHermitePoints +
            ", T_max=" + T_max +
            ", horizon=" + horizon +
            ", budget=" + budget +
            ", epsilon_utility=" + epsilon_utility +
            ", gamma=" + gamma +
            ", consecutiveAcceptableUtilities=" + consecutiveAcceptableUtilities +
            '}';
   }


   protected abstract CostGenerator<C> buildCostGenerator(long seed);

   protected abstract TestSet<C, M> initialTestSet();

   protected abstract PredictiveModel<C, M> buildPredictiveModel(TrainingSet<C, M> trainingSet, ModelParams params);

   protected abstract PredictiveModel<C, M> buildPredictiveModelForApprox(TrainingSet<C, M> trainingSet);

   protected abstract TrainingSet<C, M> emptyTrainingSet();

   protected abstract TrainingSet<C, M> emptyTrainingSetForApprox();

   protected abstract TestSet<C, M> fullTestSet();

   protected abstract LHS<C> instantiateLHS(int initTrainSamples);

   private C bestConfigGroundTruth(TestSet<C, M> fullTestSet, double t_max) {
      Pair<C, Double> opt = null;
      for (int i = 0; i < fullTestSet.size(); i++) {
         C cc = fullTestSet.getConfig(i);
         double y = costGenerator.deploymentCost(null, cc);
         double time = y / costGenerator.costPerConfigPerMinute(cc);   // time in min
         if (time > t_max)
            continue;
         if (opt == null || opt.getSnd() > y) {
            opt = new Pair<>(cc, y);
         }
      }
      if (opt == null) {
         throw new RuntimeException("No optimal config found. This should not happen, since we" +
                                          "are using t_max always at least as high as the minimum");
      }
      return opt.getFst();
   }

   private double distanceFromOpt(TestSet<C, M> fullTestSet, C config, double tmax) {
      Pair<C, Double> opt = bestInTest(fullTestSet, tmax);
      final double chosenCost = costGenerator.deploymentCost(null, config);
      final double optCost = opt.getSnd();

      if (optCost == 0) {
         throw new RuntimeException("WKLD " + wkldid + ": cost for opt " + opt.getFst() + " is " + optCost);
      }

      return (chosenCost - optCost) / optCost;
   }

   private void evalOnTest(State<C, M> evalState, int iter) {
      PredictiveModel<C, M> m = buildPredictiveModel(evalState.getTrainingSet(), evalState.getParams());
      m.train();
      double mape = 0;

      for (int ti = 0; ti < evalState.getTestSet().size(); ti++) {
         C cc = evalState.getTestSet().getConfig(ti);
         double groundTruthCost = costGenerator.deploymentCost(null, cc);
         double predCost = m.evaluate(cc);
         double err = Math.abs(groundTruthCost - predCost) / groundTruthCost;
         System.out.println(cc + " real " + groundTruthCost + " pred " + predCost + " err " + err);
         mape += err;
      }
      mape /= evalState.getTestSet().size();
      System.out.println("[" + (iter) + "] MAPE on ground truth " + mape);
   }


   OptResult doYourJob(long seed) {
      System.out.println("================ " + this.opt_type + "============ " + seed + " " + this);
      System.out.flush();
      if (debug_logs) {
         debugWriter.println("================ " + this.opt_type + "============ " + seed + " " + this);
         debugWriter.flush();
      }

      WekaGaussianProcess.ensemble = true;
      System.out.println("USING RANDOM FOREST");

      switch (opt_type) {
         case LYNCEUS:
            return lynceus(seed);
         case RAND:
            return rand(seed);
         default:
            throw new RuntimeException(opt_type + " not valid");
      }
   }

   private OptResult rand(long seed) {
	  
	  /* timer to compute how long a real execution would take
	   * (taking into account each experiment's time) */
	  long startTime = System.currentTimeMillis();

      /* keep track of the errors of the linear model for estimating cost */
      double finalError = 0.0;   // average of the errors of all explorations of the same seed

      this.testSet = initialTestSet();
      this.searchSpaceCardinality = this.testSet.size();

      final TestSet<C, M> allTestConfigsNoTarget = this.fullTestSet();

      State<C, M> currState;
      
      String timeout = Main.timeoutToStr(timeoutType);

      if (Main.bootstrap.equalsIgnoreCase("lhs")) {
         currState = initLHS(testSet, budget, seed, allTestConfigsNoTarget);
      } else {
         currState = init(testSet, budget, seed, allTestConfigsNoTarget);
      }

      if (lyn_print) {
         System.out.println("Seed: " + seed + " ; State after init" + currState);
      }
      if (debug_logs) {
         debugWriter.println("Seed: " + seed);
         debugWriter.println("State after init" + currState);
         debugWriter.flush();
      }

      /* this predictive model SHOULD NOT be here
       * Random does not use this model to guide the search
       * I put it just to assure that the init-dfo is the same
       * for all optimizers and that the bestInTrain method
       * outputs the most expensive config with a cost that is
       * cost = cost + 3*variance when there are no feasible configs */

      PredictiveModel<C, M> model = buildPredictiveModel(currState.getTrainingSet(), currState.getParams());
      model.train();

      C initBest = bestInTrain(currState.getTrainingSet(), T_max, model).getFst();
      double initDist = distanceFromOpt(allTestConfigsNoTarget, initBest, T_max);
      
      long setupCost;
      SamplingResult<C> samplingResult;
      int iter = 0;

      Random random = new Random(seed);

      /* while there are unexplored configs and we have budget ==> explore */
      while (currState.getTestSet().size() > 0 && currState.getBudget() > 0) {

         if (_trace_eval_) {
            evalOnTest(currState, iter);
         }

         C nextConfig = null;

         /* select random config from the set of unexplored configs */
         nextConfig = currState.getTestSet().getConfig(random.nextInt(currState.getTestSet().size()));

         /* explore the chosen config and update your state */
         setupCost = setupConfig(nextConfig, currState, this.costGenerator);
         samplingResult = sample(nextConfig, costGenerator, currState, model);
         updateState(currState, setupCost, samplingResult);
         this.timeExp += ((costGenerator.deploymentCost(null, nextConfig) / costGenerator.costPerConfigPerMinute(nextConfig))*60.0);
         this.nextDFO = distanceFromOpt(allTestConfigsNoTarget, nextConfig, T_max);
         
         writeRunLogs(seed, timeout, new ConfigUtility(nextConfig, -1, samplingResult.getExecutionCost()), -1.0, -1.0, -1.0, -1.0, null);
         
         // better config was found. Let us save the NEX so far
         if (this.nextDFO < this.currDFO) {
        	 this.currDFO = this.nextDFO;
        	 this.nexToOpt = (int) cumulativeExplorations;
         }

         if (lyn_print) {
            System.out.println("Updated state " + currState + " after sampling " + nextConfig + " with running cost ($) " + samplingResult.getExecutionRealCost());
         }
         if (debug_logs) {
            debugWriter.println("Updated state " + currState + " after sampling " + nextConfig + " with running cost ($) " + samplingResult.getExecutionRealCost());
            debugWriter.println("#########################################################################");
            debugWriter.flush();
         }
         iter++;
      }

      C ret = bestInTrain(currState.getTrainingSet(), T_max).getFst();
      double dist = distanceFromOpt(allTestConfigsNoTarget, ret, T_max);
      double costPerUnitOfTime = costGenerator.costPerConfigPerMinute(ret);
      double time = costGenerator.deploymentCost(null, ret) / costPerUnitOfTime;
      C groundBest = bestConfigGroundTruth(allTestConfigsNoTarget, T_max);
      double costPerTimePerBest = costGenerator.costPerConfigPerMinute(groundBest);
      double timeBest = costGenerator.deploymentCost(null, groundBest) / costPerTimePerBest;
      boolean withinTmax = time <= T_max;
      if (lyn_print) {
         System.out.println("Complying with time constraint: " + withinTmax + " ; time = " + time + " ; Tmax = " + T_max);
         System.out.println("WKLD " + wkldid + " " + this.opt_type + " Cumulative <cost, #expl> = <" + cumulativeCost + ", " + cumulativeExplorations + "> Returning best " + ret + " with time " + time + " and cost " + (time * costPerUnitOfTime) + " ground best" + groundBest + " with time " + timeBest + " and cost " + (timeBest * costPerTimePerBest) + " Dist_from_opt " + dist + " budget " + budget + " epsilon " + epsilon_utility);
      }
      if (debug_logs) {
         debugWriter.println("Complying with time constraint: " + withinTmax + " ; time = " + time + " ; Tmax = " + T_max);
         debugWriter.println("WKLD " + wkldid + " " + this.opt_type + " Cumulative <cost, #expl> = <" + cumulativeCost + ", " + cumulativeExplorations + "> Returning best " + ret + " with time " + time + " and cost " + (time * costPerUnitOfTime) + " ground best" + groundBest + " with time " + timeBest + " and cost " + (timeBest * costPerTimePerBest) + " Dist_from_opt " + dist + " budget " + budget + " epsilon " + epsilon_utility);
         debugWriter.flush();
      }
     
      finalError = writeEstimationErrorLogs(seed, timeout);
      
      double intervalTime = (System.currentTimeMillis()-startTime)/1000.0 + this.timeExp;

      System.out.println("stdv_counter = " + stdv_counter + " ; stdv_total_counter = " + stdv_total_counter + " ; %fails = " + (float) stdv_counter / (float) stdv_total_counter);
      return new OptResult(this.opt_type, this.cumulativeCost, (long) time, 0, cumulativeExplorations, initDist, dist, ret, seed, initTrainSamples, horizon, gaussHermitePoints, this.epsilon_utility, this.budget, withinTmax, finalError, timeoutCounter, nexToOpt, intervalTime, costGenerator.deploymentCost(null, ret));

   }


   private Queue<ConfigUtility<C>> sortCU(Queue<ConfigUtility<C>> queue) {
      ConfigUtility<C>[] set = new ConfigUtility[queue.size()];

      Queue<ConfigUtility<C>> results = new ConcurrentLinkedQueue<ConfigUtility<C>>();

      int i = 0;
      for (ConfigUtility<C> o : queue) {
         set[i] = o;
         i++;
      }

      /* do quickSort */
      quickSortCU(0, set.length - 1, set);

      i = 0;
      while (i < set.length) {
         results.add(set[i]);
         i++;
      }

      return results;
   }

 
   private void quickSortCU(int lowerIndex, int higherIndex, ConfigUtility<C> array[]) {
      int i = lowerIndex;
      int j = higherIndex;
      // calculate pivot number, I am taking pivot as middle index number
      int pivot = array[lowerIndex + (higherIndex - lowerIndex) / 2].getConfiguration().hashCode();

      if (i < j) {
         // Divide into two arrays
         while (i <= j) {
            /**
             * In each iteration, we will identify a number from left side which
             * is greater then the pivot value, and also we will identify a number
             * from right side which is less then the pivot value. Once the search
             * is done, then we exchange both numbers.
             */
            while (array[i].getConfiguration().hashCode() < pivot) {
               i++;
            }
            while (array[j].getConfiguration().hashCode() > pivot) {
               j--;
            }
            if (i <= j) {
               ConfigUtility<C> temp = array[i];
               array[i] = array[j];
               array[j] = temp;
               //move index to next position on both sides
               i++;
               j--;
            }
         }
         // call quickSort() method recursively
         if (lowerIndex < j)
            quickSortCU(lowerIndex, j, array);
         if (i < higherIndex)
            quickSortCU(i, higherIndex, array);
      } else
         return;
   }


   private int computePercentileIndex(int size, double percentile) {
	  int index = (int) Math.floor((percentile / 100) * size);
	  if (index == size) {
         index = size - 2; //not the max
      }
	  if (size <= 2) {
         index = (int) Math.floor(size/2);
      }
	  return index;
  }

   public static final boolean retrain_in_depth = false;

   private OptResult lynceus(long seed) {
	   
	  /* timer to compute how long a real execution would take
	   * (taking into account each experiment's time) */
	  long startTime = System.currentTimeMillis();

      System.out.println("Retraining in depth = " + retrain_in_depth);
      /* keep track of the errors of the linear model for estimating cost */
      double finalError;   // average of the errors of all explorations of the same seed
      
      final TestSet<C, M> allTestConfigsNoTarget = this.fullTestSet();
      
      this.testSet = initialTestSet();

      this.searchSpaceCardinality = this.testSet.size();

      State<C, M> currState;
      
      String timeout = Main.timeoutToStr(timeoutType);

      if (Main.bootstrap.equalsIgnoreCase("lhs")) {
         System.out.println("Bootstrapping LHS");
         currState = initLHS(testSet, budget, seed, allTestConfigsNoTarget);
      } else {
         System.out.println("Bootstrapping RND");
         currState = init(testSet, budget, seed, allTestConfigsNoTarget);
      }
      System.out.println("Bootstrapping finished");

      if (lyn_print) {
         System.out.println("Seed: " + seed + " ; State after init" + currState);
      }
      if (debug_logs) {
         debugWriter.println("Seed: " + seed);
         debugWriter.println("State after init" + currState);
         debugWriter.flush();
      }

      /* measure time */
      ArrayList<Double> timer = new ArrayList<>();
      long start;
      long elapsedTime;


      ConfigUtility<C> configUtility;
      long setupCost;
      SamplingResult<C> samplingResult;
      int iteration = 0;

      double previousUtility = -1;
      Pair<C, Double> previousBestInTrain = null;

      PredictiveModel<C, M> model = buildPredictiveModel(currState.getTrainingSet(), currState.getParams());
      model.train();
      
      C initBest = bestInTrain(currState.getTrainingSet(), T_max, model).getFst();
      double initDist = distanceFromOpt(allTestConfigsNoTarget, initBest, T_max);

      /* while there are unexplored configs and we have budget ==> explore */
      while (currState.getTestSet().size() > 0 && currState.getBudget() > 0) {

    	 currState.resetParams(); //This is done at the beginning of each iteration

         model = buildPredictiveModel(currState.getTrainingSet(), currState.getParams());
         model.train();

         WekaGaussianProcess wekaGaussianProcess = (WekaGaussianProcess) model;

         if (wekaGaussianProcess.peekClassifier() instanceof CustomGP && !retrain_in_depth) {
            CustomGP gp = (CustomGP) wekaGaussianProcess.peekClassifier();
            currState.setParams(gp.getModelParams());
         }

         if (_trace_eval_) {
            evalOnTest(currState, iteration);
         }

         try {
            NominalToBinary f = new NominalToBinary();
            f.setInputFormat(currState.getTrainingSet().instances());
            Instances train = Filter.useFilter(currState.getTrainingSet().instances(), f);
            train.setRelationName("job_cost");
            train.setClassIndex(train.numAttributes() - 1);

            Instances test = ((AbstractConfigWekaTestSet) currState.getTestSet()).getInstancesCopy();
            for (Instance i : test) {
               Configuration cc = ((AbstractConfigWekaTestSet) currState.getTestSet()).buildFromInstance(i);
               double y = costGenerator.deploymentCost(null, (C) cc);
               i.setClassValue(y);
            }

         } catch (Exception e) {
            throw new RuntimeException(e);
         }

         start = System.currentTimeMillis();
         configUtility = nextConfig(currState, this.horizon);
         elapsedTime = System.currentTimeMillis() - start;
         timer.add(elapsedTime / 1000.0);

         if (configUtility == null || configUtility.getConfiguration() == null) {
            break;
         }

         final double currentUtility = configUtility.getUtility();	// current best EI

         final Pair<C, Double> bestInTrain = bestInTrain(currState.getTrainingSet(), T_max, model);

         final double improvement = (configUtility.getUtility()) / bestInTrain.getSnd();
         
         
         if (lyn_print) {
            System.out.println("Consecutive decreases " + this.consecutiveAcceptableUtilities + " Predicted U is " + currentUtility + " prev was " + previousUtility + " curr best is " + bestInTrain.getSnd() + " improvement is " + improvement + ". NOT Stopping");
         }
         if (debug_logs) {
            debugWriter.println("#########################################################################");
            debugWriter.println("Consecutive decreases " + this.consecutiveAcceptableUtilities + " Predicted U is " + currentUtility + " prev was " + previousUtility + " curr best is " + bestInTrain.getSnd() + " improvement is " + improvement + ". NOT Stopping");
            debugWriter.flush();
         }
         previousUtility = currentUtility;
         previousBestInTrain = bestInTrain;
         
	      /*
	       * Compute avgMu, avgSimga, mu50, sigma50, mu90 and sigma90 for the current exploration
	       * to add to the run logs
	       * The avgEI, ei50 and ei90 must be computed in the method bestConfigUtility
	       */
	   	  double[] mu = new double[currState.getTestSet().size()];
	   	  double[] sigma = new double[currState.getTestSet().size()];
	   	  double currMu = 0, currSigma = 0, lowestMu=Double.POSITIVE_INFINITY;
	   	  C	currConfig, lowestMuConfig = null;
	      model = buildPredictiveModel(currState.getTrainingSet(), currState.getParams());
	      model.train();

	      this.avgMu = 0;
	      this.avgSigma = 0;
	      for (int i = 0; i < currState.getTestSet().size(); i++) {
	    	  currConfig = currState.getTestSet().getConfig(i);
	    	  currMu = model.evaluate(currConfig);
	    	  currSigma = model.stdv(currConfig);
	    	  mu[i] = currMu;
	    	  sigma[i] = currSigma;
	    	  this.avgMu += currMu;
	    	  this.avgSigma += currSigma;
	    	  if (currMu < lowestMu) {
	    		  lowestMu = currMu;
	    		  lowestMuConfig = currConfig;
	    	  }
	      }
	      
	      this.avgMu /= currState.getTestSet().size();
	      this.avgSigma /= currState.getTestSet().size();
	      
	      Arrays.sort(mu);
	      Arrays.sort(sigma);
	      
	      int index_50 = computePercentileIndex(currState.getTestSet().size(), 50.0); 
	      this.mu50 = mu[index_50];
	      this.sigma50 = sigma[index_50];

	      int index_90 = computePercentileIndex(currState.getTestSet().size(), 90.0);
	      this.mu90 = mu[index_90];
	      this.sigma90 = sigma[index_90];
	      
	      
	      final C nextConfig = configUtility.getConfiguration();
	      
         if (checkEarlyStopCondition(improvement, currentUtility, previousUtility, previousBestInTrain, bestInTrain, configUtility.getCost())) {
        	 writeRunLogs(seed, timeout, configUtility, model.evaluate(nextConfig), model.stdv(nextConfig), lowestMu, constrainedExpectedImprovement(lowestMuConfig, model, bestInTrain, false).fst, lowestMuConfig);
        	 break;
         }

         
         setupCost = setupConfig(nextConfig, currState, this.costGenerator);
         samplingResult = sample(nextConfig, costGenerator, currState, model);
         updateState(currState, setupCost, samplingResult);
         this.timeExp += (( costGenerator.deploymentCost(null, nextConfig) / costGenerator.costPerConfigPerMinute(nextConfig) )*60.0);
         this.nextDFO = distanceFromOpt(allTestConfigsNoTarget, nextConfig, T_max);
                
    	 writeRunLogs(seed, timeout, configUtility, model.evaluate(nextConfig), model.stdv(nextConfig), lowestMu, constrainedExpectedImprovement(lowestMuConfig, model, bestInTrain, false).fst, lowestMuConfig);
       
         // better config was found. Let us save the NEX so far
         if (this.nextDFO < this.currDFO) {
        	 this.currDFO = this.nextDFO;
        	 this.nexToOpt = (int) cumulativeExplorations;
         }

         if (lyn_print) {
            System.out.println("Updated state " + currState + " after sampling " + nextConfig + " with running cost ($) " + samplingResult.getExecutionRealCost());
         }
         if (debug_logs) {
            debugWriter.println("Updated state " + currState + " after sampling " + nextConfig + " with running cost ($) " + samplingResult.getExecutionRealCost());
            debugWriter.println("#########################################################################");
            debugWriter.flush();
         }
         
         iteration++;

      }

      C ret = bestInTrain(currState.getTrainingSet(), T_max, model).getFst();
      double dist = distanceFromOpt(allTestConfigsNoTarget, ret, T_max);
      double costPerUnitOfTime = costGenerator.costPerConfigPerMinute(ret);
      double time = costGenerator.deploymentCost(null, ret) / costPerUnitOfTime;
      C groundBest = bestConfigGroundTruth(allTestConfigsNoTarget, T_max);
      double costPerTimePerBest = costGenerator.costPerConfigPerMinute(groundBest);
      double timeBest = costGenerator.deploymentCost(null, groundBest) / costPerTimePerBest;
      boolean withinTmax = time <= T_max;
      if (lyn_print) {
         System.out.println("Complying with time constraint: " + withinTmax + " ; time = " + time + " ; Tmax = " + T_max);
         System.out.println("WKLD " + wkldid + " " + this.opt_type + " Cumulative <cost, #expl> = <" + cumulativeCost + ", " + cumulativeExplorations + "> Returning best " + ret + " with time " + time + " and cost " + (time * costPerUnitOfTime) + " ground best" + groundBest + " with time " + timeBest + " and cost " + (timeBest * costPerTimePerBest) + " Dist_from_opt " + dist + " budget " + budget + " epsilon " + epsilon_utility);
      }
      if (debug_logs) {
         debugWriter.println("Complying with time constraint: " + withinTmax + " ; time = " + time + " ; Tmax = " + T_max);
         debugWriter.println("WKLD " + wkldid + " " + this.opt_type + " Cumulative <cost, #expl> = <" + cumulativeCost + ", " + cumulativeExplorations + "> Returning best " + ret + " with time " + time + " and cost " + (time * costPerUnitOfTime) + " ground best" + groundBest + " with time " + timeBest + " and cost " + (timeBest * costPerTimePerBest) + " Dist_from_opt " + dist + " budget " + budget + " epsilon " + epsilon_utility);
         debugWriter.flush();
      }
      /* compute avg time to get next config */
      int time_samples = timer.size();
      int counter = 0;
      double time_sum = 0.0;
      while (counter < time_samples) {
         time_sum += timer.get(counter);
         counter++;
      }
      double avg_time = time_sum / time_samples;
      if (lyn_print) {
         System.out.println("Average time to get next config = " + avg_time + " secs");
      }
      if (debug_logs) {
         debugWriter.println("Average time to get next config = " + avg_time + " secs");
         debugWriter.flush();
      }

      finalError = writeEstimationErrorLogs(seed, timeout);
      
	  double intervalTime = (System.currentTimeMillis()-startTime)/1000.0 + this.timeExp;
      
	  System.out.println("stdv_counter = " + stdv_counter + " ; stdv_total_counter = " + stdv_total_counter + " ; %fails = " + (float) stdv_counter / (float) stdv_total_counter + " ; final_budget = " + currState.getBudget());
      return new OptResult(this.opt_type, this.cumulativeCost, (long) time, 0, cumulativeExplorations, initDist, dist, ret, seed, initTrainSamples, horizon, this.gaussHermitePoints, this.epsilon_utility, this.budget, withinTmax, finalError, timeoutCounter, nexToOpt, intervalTime, costGenerator.deploymentCost(null, ret));

   }

   
   /* update training and test sets, as well as, overall cost of exploring and number of explorations */
   private void updateState(State<C, M> currState, double setUpCost, SamplingResult<C> samplingResult) {

      double executionCost = samplingResult.getExecutionCost();
      double transformedExecutionCost = executionCost;

      if (log_transform) {
         transformedExecutionCost = Math.log(executionCost);
      }
      
      if (timeoutType == timeout.NO_INFO) {
    	  currState.setBudget((currState.getBudget() - setUpCost - samplingResult.getExecutionRealCost()));
    	  this.cumulativeCost += setUpCost + samplingResult.getExecutionRealCost();
	      this.cumulativeExplorations++;
    	  
	      if (executionCost == -1.0) {
    		  currState.removeTestSample(samplingResult.getConfig());
    	  } else {
	         currState.addTrainingSample(samplingResult.getConfig(), transformedExecutionCost);
	         currState.removeTestSample(samplingResult.getConfig());
	         currState.setCurrentConfiguration(samplingResult.getConfig());
    	  }
      } else if (timeoutType == timeout.FALSE) {
         currState.setBudget((currState.getBudget() - setUpCost - executionCost));
         currState.addTrainingSample(samplingResult.getConfig(), transformedExecutionCost);
         currState.removeTestSample(samplingResult.getConfig());
         currState.setCurrentConfiguration(samplingResult.getConfig());

         this.cumulativeCost += setUpCost + executionCost;
         this.cumulativeExplorations++;
      } else {
         currState.setBudget((currState.getBudget() - setUpCost - samplingResult.getExecutionRealCost()));      // update budget ==> subtract setup cost and runTime cost
         currState.addTrainingSample(samplingResult.getConfig(), transformedExecutionCost);
         currState.removeTestSample(samplingResult.getConfig());
         currState.setCurrentConfiguration(samplingResult.getConfig());

         this.cumulativeCost += setUpCost + samplingResult.getExecutionRealCost();
         this.cumulativeExplorations++;
      }
   }

   /**
    * try new configuration
    *
    * @return: Sampling result = {predicted cost of the sample, predicted time to train, config, real cost of the
    * sample}
    **/
   private final SamplingResult<C> sample(C config, CostGenerator<C> costGenerator, State<C, M> state, PredictiveModel<C, M> model) {

      if (timeoutType == timeout.FALSE || this.cumulativeExplorations < Lynceus.initTrainSamples) {	// what if I only timeout after the initial sampling? 
         final double runtimeCost = costGenerator.deploymentCost(state, config);
         final double costPerTimeUnit = costGenerator.costPerConfigPerMinute(config);   // $$/minute
         final double timeTaken = runtimeCost / costPerTimeUnit;                  		// minutes

         this.estimationErrors.add(new Pair(new Pair(-1.0, this.cumulativeExplorations + 1), new Triple(runtimeCost, runtimeCost, runtimeCost)));
         this.estimationErrorsConfigs.add(config);
         if (runtimeCost > maxCost) {
            maxCost = runtimeCost;
         }

         return new SamplingResult<>(runtimeCost, timeTaken, config, runtimeCost);
      }
      
      if (timeoutType == timeout.NO_INFO) {
          final double runtimeCost = costGenerator.deploymentCost(state, config);
          final double costPerTimeUnit = costGenerator.costPerConfigPerMinute(config);   // $$/minute
          final double timeTaken = runtimeCost / costPerTimeUnit;                  		// minutes
          
          Pair<C, Double> best = null;	// best pair(config, cost) found so far
          
          /* check if there is already a current best config 
           * the only chance that there might be a current best
           * config is if the training set is not empty */
          if (state.getTrainingSet().size() != 0) {
             best = bestInTrain(state.getTrainingSet(), T_max);
          }
 
          this.estimationErrors.add(new Pair(new Pair(-1.0, this.cumulativeExplorations + 1), new Triple(runtimeCost, runtimeCost, runtimeCost)));
          this.estimationErrorsConfigs.add(config);
 
          if (best != null) {
        	  // if the time taken by the current config is twice the time taken by the optimum found so far
        	  // discard the configuration
        	  if (runtimeCost >= best.getSnd() * 2)	{
        		  return new SamplingResult<>(-1.0, (best.getSnd() / costGenerator.costPerConfigPerMinute(best.getFst())) * 2, config, (best.getSnd() / costGenerator.costPerConfigPerMinute(best.getFst())) * 2 * costPerTimeUnit);
        	  }
          }
          return new SamplingResult<>(runtimeCost, timeTaken, config, runtimeCost);     
      }


      /* with timeout, there are two situations that must be considered:
       * 	- there is no current optimum yet
       * 	- there is already a best config so far
       *
       * to stop the exploration earlier, we will consider 2 regions:
       * 	- region 1: composed of the configs that are too slow (time > MAX_TIME and acc < 0.85)
       * 	- region 2: composed of the configs that are more expensive than the current optimum (cost(c) > cost(c*) and time < MAX_TIME)
       * all configs outside of these regions are possible optimums */

      double realCost;         		// how much is really paid for exploring the current config	
      double bestCostSoFar = Double.POSITIVE_INFINITY;			// cost of exploring the best config found so far
      C bestConfigSoFar;			// best config found so far
      Pair<C, Double> best = null;	// best pair(config, cost) found so far

      double costPerTimeUnit = costGenerator.costPerConfigPerMinute(config);	// $$/minute of the current config
      double costToAcc = costGenerator.deploymentCost(state, config);     		// how much it costs for current config to get acc = 0.85
      double timeToAcc = costToAcc / costPerTimeUnit;;     						// how long it takes for current config to get acc = 0.85 (minutes)
      
      double stoppingTime = 0.0;   // time when the exploration was stopped
      double discreteStoppingTime = 0.0;
      double modelCost = 0.0;      // cost with which the model will be updated

      double error = 0;	// error incurred by the model that predicts the expected time for a config to reach the target accuracy
      
      /* check if there is already a current best config 
       * the only chance that there might be a current best
       * config is if the training set is not empty */
      if (state.getTrainingSet().size() != 0) {
         best = bestInTrain(state.getTrainingSet(), T_max);
      }
      
      /* there is a current best config */
      if (timeoutType != timeout.FALSE && best != null) {
         bestConfigSoFar = (C) best.getFst();                           		// best config so far: c*
         bestCostSoFar = costGenerator.deploymentCost(state, bestConfigSoFar);	// cost of the best config so far: cost(c*)
         /* current config is a new best */
         if (timeToAcc <= T_max && costToAcc < bestCostSoFar) {
            realCost = costToAcc;
            modelCost = costToAcc;
            this.estimationErrors.add(new Pair(new Pair(-1.0, this.cumulativeExplorations + 1), new Triple(modelCost, modelCost, realCost)));
            this.estimationErrorsConfigs.add(config);

         } else { /* current config is not new best */
            if (timeToAcc > T_max && costToAcc < bestCostSoFar) {   // stop exploring when time = T_max
            	stoppingTime = T_max;
            	realCost = stoppingTime * costPerTimeUnit;
            } else if (timeToAcc <= T_max && costToAcc >= bestCostSoFar) {   // stop exploring when cost(config) = cost(c*)
               stoppingTime = bestCostSoFar / costPerTimeUnit;	// time instant when the current config reaches cost(c*)
               if (timeoutType == timeout.IDEAL) {
                  realCost = bestCostSoFar;
               } else {  // discrete version
            	   realCost = stoppingTime * costPerTimeUnit;
            	   if (stoppingTime * 60 < 30) {   // make sure we reach the 1st discrete position of the intermediateValues list
            		   discreteStoppingTime = 0.5;
            	   } else {
            		   discreteStoppingTime = (int) (Math.round((stoppingTime * 60) / 30) - 1) * 0.5 + 0.5;
            	   }
               }
            } else {   // time > T_max and cost(config) > cost(c*) ; decide whether to stop due to cost or to time
               double timeToBestCost = bestCostSoFar / costPerTimeUnit;	// time instant when the current config reaches cost(c*)
               if (timeToBestCost <= T_max) {   // stop due to cost
                  stoppingTime = timeToBestCost;	
                  if (timeoutType == timeout.IDEAL) {
                     realCost = bestCostSoFar;
                  } else {  // discrete version
                	  realCost = stoppingTime * costPerTimeUnit;
                	  if (stoppingTime * 60 < 30) {   // make sure we reach the 1st discrete position of the intermediateValues list
                		  discreteStoppingTime = 0.5;
                	  } else {
                		  discreteStoppingTime = (int) (Math.round((stoppingTime * 60) / 30) - 1) * 0.5 + 0.5;
                	  }
                  }
               } else {   // stop due to time
            	   stoppingTime = T_max;
            	   realCost = stoppingTime * costPerTimeUnit;
               }
            }

            if (timeoutType == timeout.IDEAL) {
               modelCost = costToAcc;
            } else {
               modelCost = estimateCost(costGenerator, config, stoppingTime, discreteStoppingTime, costPerTimeUnit, timeoutType, model, bestCostSoFar, state);
               if (modelCost == 0.0) {
                  modelCost = costToAcc;
               }
            }
                       
            /* add the error to the array and count number of timeouts */
            timeoutCounter++;
            error = Math.abs(costToAcc - modelCost) / costToAcc;
            this.estimationErrors.add(new Pair(new Pair(error, this.cumulativeExplorations + 1), new Triple(modelCost, costToAcc, realCost)));
            this.estimationErrorsConfigs.add(config);
         }

         if (lyn_print) {
            System.out.println("bestCost = " + bestCostSoFar + " ; costToAcc = " + costToAcc + " ; realcost = " + realCost + " ; timeToAcc = " + timeToAcc + " ; T_max = " + T_max);
            System.out.println("Sampled " + config + " with cost per time unit ($/min) " + costPerTimeUnit);
         }
         if (debug_logs) {
        	 debugWriter.println("bestConfigSoFar " + bestConfigSoFar + " ; bestCost = " + bestCostSoFar + " ; costToAcc = " + costToAcc + " ; realcost = " + realCost + " ; timeToAcc = " + timeToAcc + " ; T_max = " + T_max);
        	 debugWriter.println("Sampled " + config + " with cost per time unit ($/min) " + costPerTimeUnit);
        	 debugWriter.flush();
         }

      } else {  // there is no best config yet
         
         if (timeToAcc <= T_max) {	// current config meets the time constraint
            realCost = costToAcc;
            modelCost = costToAcc;
            this.estimationErrors.add(new Pair(new Pair(-1.0, this.cumulativeExplorations + 1), new Triple(modelCost, modelCost, realCost)));
            this.estimationErrorsConfigs.add(config);
         } else { // time constraint is not met; only run that config for MAX_TIME
         	 stoppingTime = T_max;
        	 realCost = stoppingTime * costPerTimeUnit;
        	 if (timeoutType != timeout.IDEAL) {
        		 modelCost = estimateCost(costGenerator, config, stoppingTime, discreteStoppingTime, costPerTimeUnit, timeoutType, model, bestCostSoFar, state);
        	 }
        	 if (modelCost == 0.0) {
        		 modelCost = costToAcc;
        	 }

            /* add the error to the array and count number of timeouts */
            timeoutCounter++;
            error = Math.abs(costToAcc - modelCost) / costToAcc;
            this.estimationErrors.add(new Pair(new Pair(error, this.cumulativeExplorations + 1), new Triple(modelCost, costToAcc, realCost)));
            this.estimationErrorsConfigs.add(config);
          }
         
         if (lyn_print) {
             System.out.println("Sampled " + config + " with running cost ($) " + realCost + " and model cost ($) " + modelCost + " corresponding to time " + timeToAcc + " min");
          }
          if (debug_logs) {
             debugWriter.println("Sampled " + config + " with running cost ($) " + realCost + " and model cost ($) " + modelCost + " corresponding to time " + timeToAcc + " min");
             debugWriter.flush();
          } 
      }
      
      /* compute time to get to target accuracy (Time = cost * cost/min) */
      if (timeoutType != timeout.IDEAL) {
         timeToAcc = modelCost / costPerTimeUnit;
      }
            
      /* check if cost to get to target accuracy is higher than the maximum cost seen so far */
      if (modelCost > maxCost) {
         maxCost = modelCost;
      }
      
      return new SamplingResult<>(modelCost, timeToAcc, config, realCost);
   }


   private double estimateCost(CostGenerator<C> costGenerator, C config, double stoppingTime, double discreteStoppingTime, double costPerTimeUnit, timeout timeoutType, PredictiveModel<C, M> model, double bestCostSoFar, State<C, M> state) {

      double estimatedCost;	// estimated cost to reach target acc
      double estimatedTime = 0.0;   // estimated time to reach target acc
      double targetAcc = 0.85;      // target accuracy to stop running the job
      double currentAcc;      // accuracy at stopping time;
      double[] xAxis;      	// intermediate acc values
      double[] yAxis;      	// time values
      int counter;
      double timeStep = 0.5;      // it is supposed to be 30 secs
      int numPoints;

      currentAcc = costGenerator.getAccForSpecificTime(discreteStoppingTime, config);
           
      if (currentAcc == 0) {   // this means that there are no intermediate values for the config being used
         return 0;
      } else if (currentAcc >= targetAcc) {      // if for the stoppingTime the accuracy is already good enough, then I will not estimate backwards and I will assume the job has finished
    	  estimatedTime = stoppingTime;
      } else {
         numPoints = (int) Math.round(discreteStoppingTime / timeStep);
         xAxis = costGenerator.getIntermediateValues(config, numPoints, timeoutType);
         yAxis = new double[xAxis.length];

         yAxis[0] = timeStep;

         counter = 1;
         while (counter < yAxis.length) {
            yAxis[counter] = yAxis[counter - 1] + timeStep;
            counter++;
         }

         switch (timeoutType) {   // I need to use the regressions to estimate when I will reach 85% acc based on the intermediate points I have so far
            // x Axis will have the accuracy and y axis will have the time
            case LIN:
            // TrendLine linear;
            	if (xAxis.length == 1) {
                  //System.out.println("1 point is not enough for a regression");
               } else {
	            	LinearRegression linear = new LinearRegression(xAxis, yAxis);
	            	estimatedTime = linear.predict(targetAcc);
               } 
            	break;
            case TRUNCATED_GAUSSIAN:
            	// estimated cost corresponds to the mean of the truncated gaussian at stoppingTime
            	double mu = model.evaluate(config);   // mean of the current gaussian
                double sigma = model.stdv(config);      // standard deviation of the model
                double truncationBound= stoppingTime * costPerTimeUnit;	// value corresponding to where the gaussian will be truncated
                if (sigma == 0) {
                  System.out.println("STDV is zero at exploration " + cumulativeExplorations);
                  //TODO: should we do something specific when std = 0?
                  sigma = 0.000000000000000000001;
                }
                if (bestCostSoFar == Double.POSITIVE_INFINITY) {
                	bestCostSoFar = stoppingTime * costPerTimeUnit;
                }
                if(Math.round(mu * 10000.0) / 10000.0 == Math.round(bestCostSoFar * 10000.0) / 10000.0) {
                	System.out.println("MU is the same as truncation limit at exploration " + cumulativeExplorations);
                	System.out.println("MU=" + mu + " ; lowerTruncationLimit=" + bestCostSoFar + " ; truncationBound=" + truncationBound);   	
                }
                
             	TruncatedNormalDistribution truncDist = new TruncatedNormalDistribution(mu, sigma, bestCostSoFar, Double.POSITIVE_INFINITY);
            	estimatedCost = truncDist.getMean();
            	if (cumulativeExplorations == 36)
            		System.out.println(config + " mu=" + mu + " estimatedCost=" + estimatedCost);
            	estimatedTime = estimatedCost / costPerTimeUnit;
            	break;
            case MAX_COST_PENALTY:
            	estimatedCost = maxCost;
            	estimatedTime = estimatedCost / costPerTimeUnit;
            	break;
            default:
               if (lyn_print) {
                  System.out.println("Timeout type " + Main.timeoutToStr(timeoutType) + " does not need estimation for time");
               }
               estimatedTime = stoppingTime;
               break;
         }
      }      
      if (estimatedTime < stoppingTime) {
    	  estimatedTime = stoppingTime;
      }
      estimatedCost = estimatedTime * costPerTimeUnit;

      return estimatedCost;
   }


   private final long setupConfig(C newConfig, State<C, M> currState, CostGenerator<C> costGenerator) {
      long setUpCost = (long) (costGenerator.setupCost(currState, newConfig));

      currState.setCurrentConfiguration(newConfig);

      this.cumulativeCost += (setUpCost);
      if (lyn_print) {
         System.out.println("Deployed " + currState + " with deployment cost " + setUpCost);
      }
      if (debug_logs) {
         debugWriter.println("Deployed " + currState + " with deployment cost " + setUpCost);
         debugWriter.flush();
      }
      return (setUpCost);
   }


   private Pair<C, Double> bestInTrain(TrainingSet<C, M> train, double t_max) {
      Pair<C, Double> best = null;
      Pair<C, Double> notComplyingBest = null;
      
      if (train.size() == 0) {
          return null;
       }

      for (int i = 0; i < train.size(); i++) {
         Pair<C, Double> c = train.getConfig(i);
         double time = c.getSnd() / costGenerator.costPerConfigPerMinute(c.getFst());

         if (time <= t_max) {	// check if config is feasible
            if (best == null || best.getSnd() > c.getSnd()) {
               best = c;
            }
         } else {
            if (notComplyingBest == null || notComplyingBest.getSnd() > c.getSnd()) {
               notComplyingBest = c;
            }
         }
      }

      return best;
   }

   
   private Pair<C, Double> bestInTrain(TrainingSet<C, M> train, double t_max, PredictiveModel<C, M> model) {
      Pair<C, Double> best = null;
      Pair<C, Double> notComplyingBest = null;
      Pair<C, Double> maxCostInTrain = new Pair(null, Double.NEGATIVE_INFINITY);

      if (train.size() == 0) {
         return null;
      }

      for (int i = 0; i < train.size(); i++) {
         Pair<C, Double> c = train.getConfig(i);
         double time = c.getSnd() / costGenerator.costPerConfigPerMinute(c.getFst());
       
         if (time <= t_max) {	// check if config is feasible
            if (best == null || best.getSnd() > c.getSnd()) {
               best = c;
            }
         } else {
            if (notComplyingBest == null || notComplyingBest.getSnd() > c.getSnd()) {
               notComplyingBest = c;
            }
         }

         if (c.getSnd() > maxCostInTrain.getSnd()) {
            maxCostInTrain = c;
         }
      }
      
      if (best == null) {
         return new Pair(maxCostInTrain.getFst(), maxCostInTrain.getSnd() + 3 * model.maxVariance(testSet));
      }

      return best;
   }

   
   private Pair<C, Double> bestInTest(TestSet<C, M> test, double t_max) {
      Pair<C, Double> best = null;
      Pair<C, Double> notComplyingBest = null;
      for (int i = 0; i < test.size(); i++) {
         C tc = test.getConfig(i);
         Pair<C, Double> c = new Pair<>(tc, costGenerator.deploymentCost(null, tc));
         double time = c.getSnd() / costGenerator.costPerConfigPerMinute(c.getFst());
         if (time <= t_max) {
            if (best == null || best.getSnd() > c.getSnd()) {
               best = c;
            }
         } else {
            if (notComplyingBest == null || notComplyingBest.getSnd() > c.getSnd()) {
               notComplyingBest = c;
            }
         }
      }
      
//      System.out.println("notComplyingBest " + notComplyingBest + " best " + best);
      
      if (best == null) {
         if (true)
            throw new RuntimeException("No optimal config within constraint found. This should not happen because we always set tmax at least as high as the minimum in the test set");
         return notComplyingBest;
      }
      return best;
   }


   private final static boolean skip_cost_check = false;

   static {
      if (skip_cost_check)
         System.out.println(">>>> SKIPPING COST CHECK IN POPULATE <<<<<<");
   }
   

   private void populateWithFeasibleByCost(Queue<C> feasibleSet, State<C, M> currState, PredictiveModel<C, M> model, double stdvRange) {

      for (int i = 0; i < currState.getTestSet().size(); i++) {
         C config = currState.getTestSet().getConfig(i);
         double cost = model.evaluate(config);   // mean of the model
         double sigma = model.stdv(config);      // standard deviation of the model
         double cost99 = cost + stdvRange * sigma;

         if (log_transform) {
            cost99 = Math.exp(cost99);
         }
         if (false) {
            System.out.println("Budget: " + currState.getBudget() + " Mean " + cost + " sigma " + sigma + " total (transformed if needed) " + cost99 + " untransformed cost " + Math.exp(cost99));
         }

    	 if (skip_cost_check || cost99 * (nUses + 1) + this.cumulativeCost < bTotal) {
             feasibleSet.add(config);
    	 }
         if (debug_logs) {
            debugWriter.println("[populateWithFeasibleByCost] C: " + config + " cost99 (transformed if needed) =" + cost99 + " cost = " + cost + " stdv = " + sigma);
            debugWriter.flush();
         }

      }
   }

   
   private ConfigUtility<C> nextConfig(State<C, M> currState, long horizon) {
      //Train the model
      PredictiveModel<C, M> model = buildPredictiveModel(currState.getTrainingSet(), currState.getParams());
      model.train();

      /* Identify the configurations that are within the budget with .99 probability */
      final Queue<C> feasibleSet = new ConcurrentLinkedQueue<>();
      populateWithFeasibleByCost(feasibleSet, currState, model, z_99);

      if (feasibleSet.size() == 0) {
         System.out.println("LYN: No more feasible solutions");
         Main.noMoreFeasibleSolutionsCounter ++;
         return null;
      }

      final Queue<ConfigUtility<C>> results = new ConcurrentLinkedQueue<>();
      final Queue<Double> times = new ConcurrentLinkedQueue<>();

      final Pair<C, Double> bestSoFar = bestInTrain(currState.getTrainingSet(), T_max, model);

      if (THREADS > 1) {
         final int num_t = Math.min(THREADS, feasibleSet.size());
         final Thread[] threads = new Thread[num_t];
         for (int t = 0; t < threads.length; t++) {
            threads[t] = new LynceusThread(feasibleSet, results, currState, horizon, bestSoFar, times);
            threads[t].start();
         }

         for (Thread thread : threads) {
            try {
               thread.join();
            } catch (InterruptedException e) {
               throw new RuntimeException(e);
            }
         }
      } else {
         C next;
         ConfigUtility<C> res;
         long start;
         long elapsedTime;

         while (feasibleSet.size() > 0) {
            next = feasibleSet.remove();
            start = System.currentTimeMillis();
            res = utility(currState, next, horizon, bestSoFar);
            if (debug_logs) {
               debugWriter.println("[NextConfig] path: " + res);
               debugWriter.flush();
            }
            elapsedTime = System.currentTimeMillis() - start;
            results.add(res);
            times.add(elapsedTime / 1000.0);
         }
      }

      /* order results */
      Queue<ConfigUtility<C>> orderedResults = sortCU(results);

      /* compute avg time to compute Utility */
      int time_samples = times.size();
      double time_sum = 0.0;
      while (times.size() > 0) {
         time_sum += times.poll();
      }
      double avg_time = time_sum / time_samples;
      if (lyn_print) {
         System.out.println("Average time to compute Utility = " + avg_time + " secs");
      }

      return bestConfigUtility(orderedResults);
   }

   
   /**
    * Given a set of results, take the one with the best utility. The definition of "best" utility depends on the
    * optimizer
    *
    * @param results
    * @return
    */
   private ConfigUtility<C> bestConfigUtility(Queue<ConfigUtility<C>> results) {
      //We return best utility divided by cost
      double opt = 0;
      ConfigUtility<C> best = null;      
      double[] ei = new double[results.size()];
      this.avgEI = 0;

      int i = 0;
      for (ConfigUtility<C> cu : results) {
         double norm = opt_type.equals(optimizer.LYNCEUS) ? cu.getCost() : 1.0D;
         double u = cu.getUtility() / norm;
         if (best == null || opt <= u) {    //higher is better
            best = cu;
            opt = u;
         }
         this.avgEI += u;
         ei[i] = u;
         i++;
      }
      
      this.avgEI /= results.size();
      Arrays.sort(ei);
      
      int index_50 = computePercentileIndex(results.size(), 50.0); 
      this.ei50 = ei[index_50];

      int index_90 = computePercentileIndex(results.size(), 90.0);
      this.ei90 = ei[index_90];

      return best;
   }


   /**
    * Compute the expected improvement of a configuration given an icumbent
    *
    * @param c
    * @param model
    * @param bestSoFar
    * @param print
    * @return
    */
   private Triple<Double, Double, Double> ExpectedImprovement(C c, PredictiveModel<C, M> model, Pair<C, Double> bestSoFar, boolean print) {
      final double y_min = bestSoFar.getSnd();
      final double mu_x = model.evaluate(c);
      double s_x = model.stdv(c);

      //This is never happening apparently: tr uses s_x at the denominator
      if (s_x == 0) {
         //System.out.println("STDV is zero.");
         stdv_counter++;
         if (debug_logs) {
            debugWriter.println("[constrainedExpectedImprovement] sigma is zero");
            debugWriter.flush();
         }
         //TODO: should we do something specific when std = 0?
         s_x = 0.000000000000000000001;
      }

      final double u = (y_min - mu_x) / s_x;
      final NormalDistribution standardN = new NormalDistribution(); // Create a normal distribution with mu = 0 and stdev = 1
      final double FI = standardN.cumulativeProbability(u);          // CDF of the standard normal distribution
      final double fi = standardN.density(u);                   // PDF of the standard normal distribution
      final double ei = s_x * (fi + u * FI);                   // ei = stdv * (u * CDF + PDF)

      if (print) {
         System.out.println("EI: u " + u + " fi " + fi + " FI " + FI + " best " + y_min + " mean " + mu_x + " stdv " + s_x + " ei " + ei + " " + c);
      }
      if (debug_logs) {
         debugWriter.println(" --- EI: u " + u + " fi " + fi + " FI " + FI + " best " + y_min + " mean " + mu_x + " stdv " + s_x + " ei " + ei + " " + c);
         debugWriter.flush();
      }
      return new Triple<>(ei, mu_x, s_x);
   }


   private Triple<Double, Double, Double> ExpectedImprovement(C c, double mu_x, double s_x, Pair<C, Double> bestSoFar, boolean print) {
      stdv_total_counter++;
      final double y_min = bestSoFar.getSnd();
      if (s_x == 0.0) {
         stdv_counter++;
         if (debug_logs) {
            debugWriter.println("[ExpectedImprovement] XPTO ---  sigma is zero --- ");
            debugWriter.flush();
         }
      }
      final double u = (y_min - mu_x) / s_x;
      final NormalDistribution standardN = new NormalDistribution(); // Create a normal distribution with mu = 0 and stdev = 1
      final double FI = standardN.cumulativeProbability(u);          // CDF of the standard normal distribution
      final double fi = standardN.density(u);                   // PDF of the standard normal distribution
      final double ei = s_x * (fi + u * FI);                   // ei = stdv * (u * CDF + PDF)

      if (print) {
         System.out.println("EI: u " + u + " fi " + fi + " FI " + FI + " best " + y_min + " mean " + mu_x + " stdv " + s_x + " ei " + ei);
      }
      if (debug_logs) {
         debugWriter.println("C: " + c + " EI: u " + u + " fi " + fi + " FI " + FI + " best " + y_min + " mean " + mu_x + " stdv " + s_x + " ei " + ei);
         debugWriter.flush();
      }
      return new Triple<>(ei, mu_x, s_x);
   }

   
   /**
    * Compute the GP-UCB improvement over the incumbent
    *
    * @param c
    * @param model
    * @param bestSoFar
    * @param print
    * @return
    */
   private Triple<Double, Double, Double> UCBImprovement(C c, PredictiveModel<C, M> model, Pair<C, Double> bestSoFar, boolean print) {
      //We compute beta given delta following Theorem  1 in     https://arxiv.org/pdf/0912.3995.pdf
      final double t = cumulativeExplorations + 1; //
      assert searchSpaceCardinality > 0;
      final double card_D = searchSpaceCardinality;
      final double beta_t = 2 * Math.log(card_D * t * t * Math.PI * Math.PI / (6 * delta));
      final double mu_x = model.evaluate(c);
      final double s_x = model.stdv(c);
      return new Triple<>(mu_x - beta_t * s_x, mu_x, s_x);
   }

   /**
    * Compute the constrained expected improvement. The expected improvement can be plain EI or its "UCB version"
    *
    * @param c
    * @param model
    * @param bestSoFar
    * @param print
    * @return
    */
   private Triple<Double, Double, Double> constrainedExpectedImprovement(C c, PredictiveModel<C, M> model, Pair<C, Double> bestSoFar, boolean print) {

      stdv_total_counter++;

      Triple<Double, Double, Double> tr;
      tr = ExpectedImprovement(c, model, bestSoFar, print);

      final double ei = tr.fst;
      final double mu_x = tr.snd;   // mean of the model
      double s_x = tr.trd;         // stdv of the model

      final NormalDistribution distribution = new NormalDistribution(mu_x, s_x);   // we assume that the predictions of the model follow a normal distribution
      double cost_per_unit = costGenerator.costPerConfigPerMinute(c);
      double max_cost = T_max * cost_per_unit;
      if (log_transform) {
         max_cost = Math.log(max_cost);
      }
      double p_feasible = distribution.cumulativeProbability(max_cost);  // p_feasible = P(cost <= max_cost)
      // all the configs whose cost is lower than the max cost should satisfy the time constraint

      double improvement;
      improvement = ei;

      return new Triple<>(improvement * p_feasible, improvement, p_feasible);
   }
   
   
   /**
    * Computes the long-term utility corresponding to starting a sampling path with initial config "config"
    *
    * @param state
    * @param config
    * @param horizon
    * @param bestSoFar
    * @return
    */
   private ConfigUtility<C> utility(State<C, M> state, C config, long horizon, Pair<C, Double> bestSoFar) {

      final PredictiveModel<C, M> model = buildPredictiveModel(state.getTrainingSet(), state.getParams());
      model.train();

      final double avgCost = model.evaluate(config);
      final double stdCost = model.stdv(config);
      double U = 0, C = 0;
      //We add the EIc right away as expected value computed in closed form.
      final double eic = constrainedExpectedImprovement(config, model, bestSoFar, false).fst;

      if (lyn_print) {
         System.out.println("[utility] " + config + " ; eic = " + eic + " ; avgCost = " + avgCost + " ; stdCost = " + stdCost);
      }
      if (debug_logs) {
         debugWriter.println("[utility] " + config + " ; eic = " + eic + " ; avgCost = " + avgCost + " ; stdCost = " + stdCost);
         debugWriter.flush();
      }

      //Add the deployment cost (which only depends on the current state)
      //The runtime cost is added from time to time depending on the GH decomposition
      final double setup_cost = costGenerator.setupCost(state, config);

      if (!log_transform) {
         C += (setup_cost + avgCost);
      } else {
         C += Math.exp(avgCost);
      }
      U += eic;


      if (horizon == 0) {
         // with the cost updated as in the earlier version, C returned is C = avgCost + deploymentCost
         return new ConfigUtility<C>(config, U, C);
      }

      /* Go in depth. Assume the current config has a given cost and see the utility of the path
      starting from such current config
       */

      final GaussHermiteParams ghp = NormalGaussHermiteQuadrature.decompose(this.gaussHermitePoints, avgCost, stdCost * stdCost); //N takes mu, variance

      /* check approximation error */
      ghp.checkIntegral(avgCost);

      for (int g = 0; g < ghp.cardinality(); g++) {      /* ghp.cardinality = gaussHermitePoints */

         final double target_y = ghp.getValue(g);      // ghp value that corresponds to weight g
         final double gh_weight = ghp.getWeight(g);

         if (target_y <= 0 && !log_transform) continue;

         // After deploying this,  there's no more budget, so there's a null utility
         if (!log_transform) {
            if (state.getBudget() - target_y <= 0)
               continue;
         } else {
            if (state.getBudget() - Math.exp(target_y) <= 0)
               continue;
         }

         /* Clone state as input for the in-depth simulation */
         State<C, M> clone_curr = state.clone();
         clone_curr.setCurrentConfiguration(config);
         clone_curr.addTrainingSample(config, target_y);
         if (!log_transform) {
            clone_curr.setBudget((clone_curr.getBudget() - target_y - setup_cost));
         } else {
            clone_curr.setBudget((clone_curr.getBudget() - Math.exp(target_y)));
         }

         clone_curr.removeTestSample(config);
         C nextC = optimized_policy(clone_curr, (horizon - 1));
         if (nextC == null || nextC.equals(config)) {
            continue;
         }
         /* Select next point and add the corresponding cost/utility*/
         ConfigUtility<C> nextUtil = utility(clone_curr, nextC, (horizon - 1), bestInTrain(clone_curr.getTrainingSet(), T_max, model));
         U += gamma * gh_weight * nextUtil.getUtility();
         // The cost is the exploration cost of the current config (target_y) + the cost of the path

         //Note that the gh weight is a constant, so it's not affected by the log
         //Cost is already denormalized in the utility
         C += gamma * gh_weight * (nextUtil.getCost());
      }

      return new ConfigUtility<>(config, U, C);
   }


   /**
    * An optimized version to get results more quickly
    *
    * @param state
    * @param horizon
    * @return
    */
   private C optimized_policy(State<C, M> state, long horizon) {

      final PredictiveModel<C, M> model = buildPredictiveModel(state.getTrainingSet(), state.getParams());
      model.train();

      C config;
      C opt = null;
      double cost, sigma, cost99, ei;
      double curr_max_ei = -1;

      final Pair<C, Double> bestSoFar = bestInTrain(state.getTrainingSet(), T_max, model);
      for (int i = 0; i < state.getTestSet().size(); i++) {
         config = state.getTestSet().getConfig(i);
         cost = model.evaluate(config);
         sigma = model.stdv(config);
         cost99 = cost + z_99 * sigma;
    	 if (cost99 * (nUses + 1) + this.cumulativeCost < bTotal) {
    	    ei = constrainedExpectedImprovement(config, model, bestSoFar, false).fst;
            if (opt == null || ei > curr_max_ei) {
               opt = config;
               curr_max_ei = ei;
            }
         }

      }
      return opt;
   }


   private State<C, M> init(TestSet<C, M> allConfigs, double budget, long seed, TestSet<C, M> allTestConfigsNoTarget) {
      if (lyn_print) {
         System.out.println("Initializing");
      }
      if (debug_logs) {
         debugWriter.println("Initializing");
         debugWriter.flush();
      }
      
      String timeout = Main.timeoutToStr(timeoutType);
      
      final State<C, M> currState = new State<>();
      final TrainingSet<C, M> trainingSet = emptyTrainingSet();
      currState.setBudget(budget);
      currState.setTestSet(allConfigs);
      currState.setTrainingSet(trainingSet);
      int total = initTrainSamples;
      Random r = new Random(seed);


      PredictiveModel<C, M> model = buildPredictiveModel(currState.getTrainingSet(), currState.getParams());
      model.train();
      
      while (total > 0) {
         C nextConfig = allConfigs.getConfig(r.nextInt(allConfigs.size()));
         
         this.nextDFO = distanceFromOpt(allTestConfigsNoTarget, nextConfig, T_max);

         // better config was found. Let us save the NEX so far
         if (this.nextDFO < this.currDFO) {
        	 this.currDFO = this.nextDFO;
        	 this.nexToOpt = (int) (cumulativeExplorations - 1);
         }
         
         long setupCost = setupConfig(nextConfig, currState, this.costGenerator);
         SamplingResult<C> samplingResult = sample(nextConfig, this.costGenerator, currState, model);
         updateState(currState, setupCost, samplingResult);
         total--;
         model = buildPredictiveModel(currState.getTrainingSet(), currState.getParams());
         model.train();
         writeRunLogs(seed, timeout, new ConfigUtility(nextConfig, -1, samplingResult.getExecutionCost()), -1.0, -1.0, -1.0, -1.0, null);
      }
      if (lyn_print) {
         System.out.println("Initialized.");
      }
      if (debug_logs) {
         debugWriter.println("Initialized.");
         debugWriter.flush();
      }
      return currState;
   }

  
   private State<C, M> initLHS(TestSet<C, M> allConfigs, double budget, long seed, TestSet<C, M> allTestConfigsNoTarget) {
      if (lyn_print) {
         System.out.println("Initializing");
      }
      if (debug_logs) {
         debugWriter.println("Initializing");
         debugWriter.flush();
      }
      
      String timeout = Main.timeoutToStr(timeoutType);

      final State<C, M> currState = new State<>();
      final TrainingSet<C, M> trainingSet = emptyTrainingSet();
      lhs.setDataset(trainingSet.instances());
      lhs.createLHSset((int) seed);
      currState.setBudget(budget);
      currState.setTestSet(allConfigs);
      currState.setTrainingSet(trainingSet);
      int total = initTrainSamples;

      PredictiveModel<C, M> model = buildPredictiveModel(currState.getTrainingSet(), currState.getParams());
      model.train();
      
      while (total > 0) {
         C nextConfig = lhs.getSample();

         this.nextDFO = distanceFromOpt(allTestConfigsNoTarget, nextConfig, T_max);
                  
         long setupCost = setupConfig(nextConfig, currState, this.costGenerator);
         SamplingResult<C> samplingResult = sample(nextConfig, this.costGenerator, currState, model);
         updateState(currState, setupCost, samplingResult);
         total--;
         model = buildPredictiveModel(currState.getTrainingSet(), currState.getParams());
         model.train();
         writeRunLogs(seed, timeout, new ConfigUtility(nextConfig, -1, samplingResult.getExecutionCost()), -1.0, -1.0, -1.0, -1.0, null);
         
         // better config was found. Let us save the NEX so far
         if (this.nextDFO < this.currDFO) {
        	 this.currDFO = this.nextDFO;
        	 this.nexToOpt = (int) (cumulativeExplorations - 1);
         }
  
      }

      if (lyn_print) {
         System.out.println("Initialized.");
      }
      if (debug_logs) {
         debugWriter.println("Initialized.");
         debugWriter.flush();
      }

      return currState;
   }


   private class LynceusThread extends Thread {
      private Queue<C> allConfigs;
      private Queue<ConfigUtility<C>> results;
      private Queue<Double> times;
      private lynceus.State<C, M> state;
      private long horizon;
      private Pair<C, Double> bestSoFar;

      LynceusThread(Queue<C> allConfigs, Queue<ConfigUtility<C>> results, lynceus.State<C, M> state, long horizon, Pair<C, Double> bestSoFar, Queue<Double> times) {
         this.allConfigs = allConfigs;
         this.results = results;
         this.state = state;
         this.horizon = horizon;
         this.bestSoFar = bestSoFar;
         this.times = times;

         if (retrain_in_depth) {
            this.state.resetParams();
         }
      }

      @Override
      public void run() {
         C c;
         ConfigUtility<C> res;
         long start;
         long elapsedTime;

         while ((c = allConfigs.poll()) != null) {
            start = System.currentTimeMillis();
            res = utility(this.state, c, horizon, bestSoFar);
            elapsedTime = System.currentTimeMillis() - start;
            results.add(res);
            times.add(elapsedTime / 1000.0);
         }
      }
   }


   private boolean checkEarlyStopCondition(double impr, double cU, double prevU, Pair<C, Double> previousBestInTrain, Pair<C, Double> bestSoFar, Double optCostSoFar) {

	   if (prevU >= 0) {
          	final double previousImprovement = prevU / previousBestInTrain.getSnd();
        	if (this.es_type == earlyStopOptions.MIXED) {
        		/* 
        		 * ProteusTM's Early Stopping Condition Mixed with 
        		 * the early stopping condition Paolo and I discussed on 08/07/2019:
        		 * the exploration is terminated after k steps when:
				 *		(i) the EI decreased in the last 2 iterations;
				 *		(ii) the EI for the k-th exploration was marginal, i.e.,
				 *			lower than \epsilon with respect to the current best sampled;
				 *		(iii) the relative performance improvement achieved in the
				 *			k − 1-th iteration did not exceed \epsilon.
				 *	This depends on the expected number of uses of the model
				 *		==> \epsiolon =  (1 - (nUses / (nUses + 1)))
        		 */
        		if (cU < prevU) {
        			this.consecutiveAcceptableUtilities++;
	        		if (cU < (1 - (nUses / (nUses + 1))) && previousImprovement <= (1 - (nUses / (nUses + 1))) && this.consecutiveAcceptableUtilities > minAcceptableUtilities) {	
	                    if (lyn_print) {
	                        System.out.println("Predicted U is " + cU + " best in train is " + bestSoFar.getSnd() + "  improvement is only " + impr + ". Stopping");
	                     }
	            		 System.out.println("Predicted U is " + cU + " best so far is  " + bestSoFar.getSnd() + "  improvement is only " + impr + ". NEX=" + cumulativeExplorations + " Stopping");
	            		 Main.earlyStoppingCounter ++;         
	            		 return true;
	        		}
        		} else {
        			this.consecutiveAcceptableUtilities = 0;
        		}
        	} else if (this.es_type == earlyStopOptions.PROTEUS_TM) {
        		/* 
        		 * ProteusTM's Early Stopping Condition:
        		 * the exploration is terminated after k steps when:
				 *		(i) the EI decreased in the last 2 iterations;
				 *		(ii) the EI for the k-th exploration was marginal, i.e.,
				 *			lower than \epsilon with respect to the current best sampled;
				 *		(iii) the relative performance improvement achieved in the
				 *			k − 1-th iteration did not exceed \epsilon.
				 *		==> \epsiolon =  0.1
        		 */
        		if (cU < prevU) {
        			this.consecutiveAcceptableUtilities++;
	        		if (cU < 0.1 && previousImprovement <= 0.1 && this.consecutiveAcceptableUtilities > minAcceptableUtilities) {	
	                    if (lyn_print) {
	                        System.out.println("Predicted U is " + cU + " best in train is " + bestSoFar.getSnd() + "  improvement is only " + impr + ". Stopping");
	                     }
	            		 System.out.println("Predicted U is " + cU + " best so far is  " + bestSoFar.getSnd() + "  improvement is only " + impr + ". NEX=" + cumulativeExplorations + " Stopping");
	            		 Main.earlyStoppingCounter ++;         
	            		 return true;
	        		}
        		} else {
        			this.consecutiveAcceptableUtilities = 0;
        		}
        	} else if (this.es_type == earlyStopOptions.EI_LESS_THAN_10_PERCENT) {
	       		/*
	       		 * CherryPick's Early Stopping Condition:
	       		 * EI < 10% 
	       		 */
	       		if (impr < 0.1) {
	       			System.out.println("Predicted U is " + cU + " best so far is  " + bestSoFar.getSnd() + "  improvement is only " + impr + ". NEX=" + cumulativeExplorations + " Stopping");
	       		 	Main.earlyStoppingCounter ++;         
	                 return true;
	       		}
        	} else if (this.es_type == earlyStopOptions.EI_LESS_THAN_1_PERCENT) {
           		/*
           		 * CherryPick's Early Stopping Condition:
           		 * EI < 10% 
           		 */
	       		if (impr < 0.01) {
	       			System.out.println("Predicted U is " + cU + " best so far is  " + bestSoFar.getSnd() + "  improvement is only " + impr + ". NEX=" + cumulativeExplorations + " Stopping");
	       		 	Main.earlyStoppingCounter ++;         
	                 return true;
	       		}
        	} else if (this.es_type == earlyStopOptions.IMPR_LESS_THAN_10_PERCENT) {
        		/*
           		 * Early stop if 
           		 * 	impr = EI / cost(x*) < 10% 3 times in a row
           		 */
        		if (impr < 0.1) {
        			this.consecutiveAcceptableUtilities++;
        			if (this.consecutiveAcceptableUtilities > minAcceptableUtilities) {	
        				System.out.println("Predicted U is " + cU + " best so far is  " + bestSoFar.getSnd() + "  improvement is only " + impr + ". NEX=" + cumulativeExplorations + " Stopping");
        				Main.earlyStoppingCounter ++;         
        				return true;
        			}
        		} else {
        			this.consecutiveAcceptableUtilities = 0;
        		}
        	} else if (this.es_type == earlyStopOptions.OUR_ES) {
          		/*
           		 * This is the early stopping condition Paolo and I discussed on 08/07/2019
           		 * This depends on the expected number of uses of the model 
           		 * and on the total budget (exploration and use) provided by the user
           		 */
           		if (impr < (1 - (nUses / (nUses + 1))) || optCostSoFar*(nUses + 1) + this.cumulativeCost > bTotal) {
           			System.out.println("Predicted U is " + cU + " best so far is  " + bestSoFar.getSnd() + "  improvement is only " + impr + ". NEX=" + cumulativeExplorations + " Stopping");
               		Main.earlyStoppingCounter ++;         
           			return true;
           		}
        	} else if (this.es_type == earlyStopOptions.OUR_ES_3) {
          		/*
           		 * This is the early stopping condition Paolo and I discussed on 08/07/2019
           		 * This depends on the expected number of uses of the model 
           		 * and on the total budget (exploration and use) provided by the user
           		 */
           		if (impr < (1 - (nUses / (nUses + 1))) || optCostSoFar*(nUses + 1) + this.cumulativeCost > bTotal) {
           			this.consecutiveAcceptableUtilities++;
        			if (this.consecutiveAcceptableUtilities > minAcceptableUtilities) {
	           			System.out.println("Predicted U is " + cU + " best so far is  " + bestSoFar.getSnd() + "  improvement is only " + impr + ". NEX=" + cumulativeExplorations + " Stopping");
	               		Main.earlyStoppingCounter ++;         
	           			return true;
        			}
           		} else {
           			this.consecutiveAcceptableUtilities = 0;
           		}
        	} else {	// no early stopping 
	            if (impr < epsilon_utility) {
	               this.consecutiveAcceptableUtilities++;
	               if (impr < epsilon_utility && consecutiveAcceptableUtilities > minAcceptableUtilities) {
	                  if (lyn_print) {
	                     System.out.println("Predicted U is " + cU + " best so far is  " + bestSoFar.getSnd() + "  improvement is only " + impr + ". Stopping");
	                  }
	                  if (debug_logs) {
	                     debugWriter.println("Predicted U is " + cU + " best so far is  " + bestSoFar.getSnd() + "  improvement is only " + impr + ". Stopping");
	                     debugWriter.flush();
	                  }
	                  return true;
	               }
	            } else {
	               consecutiveAcceptableUtilities = 0;
	            }
        	}
 	   }
	   return false;
   }   

   private void writeRunLogs(long seed, String timeout, ConfigUtility configUtility, double mu, double sigma, double lowestMu, double EIcOfLowestMu, C configOfLowestMu) {
	   if (runLogs) { 
	         //runLogsWriter.println("runID;wkld;optimizer;budget;bootstrapMethod;initSamples;nUses;timeout;earlyStop;lookahead;gamma;gh;maxTimePerc;explorationNumber;config;DFO(config);currBestDFO;EIc;MU;SIGMA;LowestMu;EIcOfLowestMu;configOfLowestMu;avgMu;avgSigma;avgEI;mu50;sigma50;ei50;mu90;sigma90;ei90");
	         runLogsWriter.println(seed + ";" + Main.file + ";" + this.opt_type + ";" + discreteBudget + ";" + Main.bootstrap + ";" + initTrainSamples + ";" + nUses + ";" + timeout + ";" + Main.earlyStop + ";" + horizon + ";" + gamma + ";" + gaussHermitePoints + ";" + maxTimePerc + ";" + cumulativeExplorations + ";" + configUtility.getConfiguration().toString() + ";" + this.nextDFO + ";" + this.currDFO + ";" + configUtility.getUtility()+ ";" + mu + ";" + sigma + ";" + lowestMu + ";" + EIcOfLowestMu + ";" + configOfLowestMu + ";" + this.avgMu + ";" + this.avgSigma + ";" + this.avgEI + ";" + this.mu50 + ";" + this.sigma50 + ";" + this.ei50 + ";" + this.mu90 + ";" + this.sigma90 + ";" + this.ei90);
	         runLogsWriter.flush();
       }
   }
   
   
   private double writeEstimationErrorLogs(long seed, String timeout) {
     double finalError = 0.0;
     int size = 0;
     for (int i = 0; i < estimationErrors.size(); i++) {
   	  if (estimationErrors.get(i).getFst().getFst() != -1) {
   		  finalError += estimationErrors.get(i).getFst().getFst();
   		  size++;
   	  }
         if (estimationErrors_logs) {
            //estimationErrorsWriter.println("wkld;optimizer;budget;bootstrapMethod;initSamples;nUses;timeout;earlyStop;numSeeds;seed;lookahead;gamma;gh;maxTimePerc;estimationError;exploration;modelCost;realCost;executionCost;costPerTimeUnit");
            estimationErrorsWriter.println(Main.file + ";" + this.opt_type + ";" + discreteBudget + ";" + Main.bootstrap + ";" + initTrainSamples + ";" + nUses + ";" + timeout + ";" + Main.earlyStop + ";" + Main.numSeeds + ";" + seed + ";" + horizon + ";" + gamma + ";" + gaussHermitePoints + ";" + maxTimePerc + ";" + estimationErrors.get(i).getFst().getFst() + ";" + estimationErrors.get(i).getFst().getSnd() + ";" + estimationErrors.get(i).getSnd().getFst() + ";" + estimationErrors.get(i).getSnd().getSnd() + ";" + estimationErrors.get(i).getSnd().getTrd() + ";" + costGenerator.costPerConfigPerMinute(this.estimationErrorsConfigs.get(i)));
            estimationErrorsWriter.flush();
         }
      }
     if (size != 0)
   	  finalError /= size;      
     
     return finalError;

   }

}
