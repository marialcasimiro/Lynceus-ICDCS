package lynceus;

import weka.core.Instances;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public abstract class LHS<C extends Configuration> {

   /* class attributes */
   private int numSamples;
   protected ArrayList<double[]> dims = new ArrayList<double[]>();
   private ArrayList<C> samplesLHS = new ArrayList<C>();
   private int nextSample;
   protected Instances dataset;
   protected int seed;

   /* class constructor */
   public LHS(int samples) {
      numSamples = samples;
      nextSample = 0;
      dims = initDims();
   }

   /* abstract methods to be implemented */

   /**
    * Initialise the dimension arrays
    *
    * @return a list with the initialised arrays
    */
   protected abstract ArrayList<double[]> initDims();

   /**
    * Retrieve new initial sample with values corresponding to index 'index' of all dimension arrays
    */
   protected abstract C newSample(int index);

   protected abstract ArrayList<Double> dimensions(C config);

   protected abstract void checkSamples(List<C> samples, String type, int seed);


   /* other methods */

   /**
    * Compute distance between a pair of initial samples/configurations
    */
   protected double computeDistance(C a, C b) {
      ArrayList<Double> dimsA = dimensions(a);
      ArrayList<Double> dimsB = dimensions(b);
      double distance = 0;

//		System.out.println("A = " + a + " ; B = " + b);
//		System.out.print("dimsA = " + dimsA.toString() + " ; dimsB = " + dimsB);

      for (int i = 0; i < dimsA.size(); i++) {
         distance = distance + Math.pow(dimsA.get(i) - dimsB.get(i), 2);
//		 System.out.println("DA = " + dimsA.get(i) + " ; DB = " + dimsB.get(i) + "; SQRT(DA-DB) = " + Math.pow(dimsA.get(i)-dimsB.get(i), 2) + " ; distance = " + distance);
      }
//		System.out.println(" ; min distance = " + Math.sqrt(distance));
      return Math.sqrt(distance);
   }

   public void setDataset(Instances dataset) {
      this.dataset = dataset;
      //System.out.println("Dataset set");
   }

   /**
    * Retrieve one sample from the chosen set of initial samples
    *
    * @return the next sample
    */
   public C getSample() {
      if (nextSample < numSamples) {
         C sample = samplesLHS.get(nextSample);
         nextSample++;
         return sample;
      } else {
         return null;
      }
   }

   /**
    * Generate a set of initial samples
    *
    * @return the generated set
    */
   private ArrayList<C> getSet(Random random) {
      ArrayList<C> newSet = new ArrayList<C>();

//		System.out.println("Getting " + numSamples + " samples");
      for (int i = 0; i < numSamples; i++) {
         C currSample = newSample(i);
         while (newSet.contains(currSample)) {
            randomize(random);
            currSample = newSample(i);
         }
         newSet.add(currSample);
//   	 System.out.println("Added sample " + i);
      }

      return newSet;
   }

   /*
    * Generate 100 sets of initial samples and compute their minimum distance
    * between pairs of initial samples. Choose the set that has the maximum
    * minimum distance.
    */
   public void createLHSset(int seed) {
      
	  this.seed = seed;
	  int numSets = 100;
      ArrayList<C> currSet = new ArrayList<C>();
      double currMinDistance = Double.NEGATIVE_INFINITY;
      double bestDistance = Double.NEGATIVE_INFINITY;   // the best distance is that maximum minimum distance

//		System.out.println("Building LHS sets");
      Random r = new Random(seed);
      for (int i = 0; i < numSets; i++) {
         randomize(r);
         currSet = getSet(r);
         currMinDistance = getMinDistance(currSet);
//			System.out.println("currMinDistance=" + currMinDistance);
         if (currMinDistance > bestDistance) {
            bestDistance = currMinDistance;
            samplesLHS = currSet;
         }
      }
      //checkSamples(samplesLHS, "lhs", seed);

//		System.out.println("bestDistance = " + bestDistance + " ; LHSSet = " + samplesLHS);
   }

   /**
    * Compute the minimum distance between pairs of initial samples of the set 'set'
    *
    * @param set: the set of initial samples that is under evaluation
    * @return minimum distance between pairs of initial samples
    */
   protected double getMinDistance(List<C> set) {
      double currDistance;
      double minDistance = Double.POSITIVE_INFINITY;

      for (int i = 0; i < set.size() - 1; i++) {
         for (int j = i + 1; j < set.size(); j++) {
            currDistance = computeDistance(set.get(i), set.get(j));
            if (currDistance < minDistance) {
               minDistance = currDistance;
            }
            //System.out.println("i = " + i + " ; j = " + j + " ; currDistance = " + currDistance + " ; minDistance = " + minDistance);
         }
      }

      return minDistance;
   }

   /**
    * Shuffle the several dimension arrays of the configuration
    */
   private void randomize(Random r) {
      for (int i = 0; i < dims.size(); i++) {
         shuffleArray(dims.get(i), r);
      }
   }

   private void shuffleArray(double[] a, Random random) {
      int n = a.length;
      //Random random = new Random();
      //random.nextInt();
      for (int i = 0; i < n; i++) {
         int change = i + random.nextInt(n - i);
         swap(a, i, change);
      }
   }

   private void swap(double[] a, int i, int change) {
      double helper = a[i];
      a[i] = a[change];
      a[change] = helper;
   }

}
