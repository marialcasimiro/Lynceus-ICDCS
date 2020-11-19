package lynceus.results;

import lynceus.Configuration;
import lynceus.Lynceus;

/**
 * @author Diego Didona
 * @email diego.didona@epfl.ch
 * @since 14.03.18
 */
public class OptResult implements java.io.Serializable {



   private Lynceus.optimizer opt;
   private double explCost;
   private long explTimeTaken;
   private long wallclockTimeTaken;
   private long explNumExpls;
   private double distFromOpt;
   private double initDistFromOpt;
   private Configuration finalConfig;
   private long seed;
   private long initTain;
   private long horizon;
   private long gaussHermitPoints;
   private double epsilon;
   private double budget;
   private boolean withinTmax;
   private double estimationError;
   private long numTimeouts;
   private int nexToOpt;
   private double experimentTime;
   private double optCost;

   public OptResult(Lynceus.optimizer op, double explCost, long explTimeTaken, long wallclockTimeTaken, long explNumExpls, double initDistFromOpt, double distFromOpt, Configuration finalConfig, long seed, long initTain, long horizon, long gaussHermitPoints, double epsilon, double budget,boolean withinTmax, double error, long numTimeouts, int nexToOpt, double expTime, double optCost) {
      this.explCost = explCost;
      this.explTimeTaken = explTimeTaken;
      this.wallclockTimeTaken = wallclockTimeTaken;
      this.explNumExpls = explNumExpls;
      this.initDistFromOpt = initDistFromOpt;
      this.distFromOpt = distFromOpt;
      this.finalConfig = finalConfig;
      this.seed = seed;
      this.initTain = initTain;
      this.horizon = horizon;
      this.gaussHermitPoints = gaussHermitPoints;
      this.epsilon = epsilon;
      this.opt = op;
      this.budget = budget;
      this.withinTmax = withinTmax;
      this.estimationError = error;
      this.numTimeouts = numTimeouts;
      this.nexToOpt = nexToOpt;
      this.experimentTime = expTime;
      this.optCost = optCost;
   }

   public boolean isWithinTmax() {
      return withinTmax;
   }

   public double getBudget() {
      return budget;
   }

   public Lynceus.optimizer getOpt() {
      return opt;
   }

   public double getExplCost() {
      return explCost;
   }

   public long getExplTimeTaken() {
      return explTimeTaken;
   }

   public long getWallclockTimeTaken() {
      return wallclockTimeTaken;
   }

   public long getExplNumExpls() {
      return explNumExpls;
   }

   public double getInitDistFromOpt() {
	   return initDistFromOpt;
   }
   
   public double getDistFromOpt() {
      return distFromOpt;
   }

   public Configuration getFinalConfig() {
      return finalConfig;
   }

   public long getSeed() {
      return seed;
   }

   public long getInitTain() {
      return initTain;
   }

   public long getHorizon() {
      return horizon;
   }

   public long getGaussHermitPoints() {
      return gaussHermitPoints;
   }

   public double getEpsilon() {
	   return epsilon;
   }

   public double getEstimationError() {
	   return estimationError;
   }
   
   public long getNumTimeouts() {
	   return numTimeouts;
   }
	
   public int getNexToOpt() {
	   return nexToOpt;
   }
   
   public double getExperimentTime() {
	   return experimentTime;
   }
   
   public double getOptCost() {
	   return this.optCost;
   }
   
   @Override
   public String toString() {
	   return "OptResult [opt=" + opt + ", explCost=" + explCost + ", explTimeTaken=" + explTimeTaken
			+ ", wallclockTimeTaken=" + wallclockTimeTaken + ", explNumExpls=" + explNumExpls + ", distFromOpt="
			+ distFromOpt + ", initDistFromOpt=" + initDistFromOpt + ", finalConfig=" + finalConfig + ", seed=" + seed
			+ ", initTain=" + initTain + ", horizon=" + horizon + ", gaussHermitPoints=" + gaussHermitPoints
			+ ", epsilon=" + epsilon + ", budget=" + budget + ", withinTmax=" + withinTmax + ", estimationError="
			+ estimationError + ", numTimeouts=" + numTimeouts + ", nexToOpt=" + nexToOpt + ", experimentTime="
			+ experimentTime + "]";
}

   
	
}
