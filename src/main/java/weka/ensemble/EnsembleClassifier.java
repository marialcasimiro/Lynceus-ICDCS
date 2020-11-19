package weka.ensemble;

import weka.classifiers.Classifier;
import weka.classifiers.trees.RandomForest;
import weka.classifiers.trees.RandomTree;
import weka.core.Capabilities;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * @author Diego Didona
 * @email diego.didona@epfl.ch
 * @since 21.03.18
 */
public class EnsembleClassifier implements Classifier {
   private final int numClassifiers;
   private final Classifier[] baseClassifiers;
   private final Random random;

   private final static boolean _bagging_ = true;

   static {
      if (!_bagging_) {
         throw new RuntimeException("Bagging has to be enabled");
      }
   }
   /*
   We implement a random forest by bagging.
   However, we have few data points.
   One idea is to introduce randomness in the learners
   by changing some params, e.g., the random number
   however, that's not as good as bagging (I tried it)
    */

   /* If we have less then 10 samples, we are using as many learners as samples */
   public EnsembleClassifier(int numClassifiers, long seed) {
      this.numClassifiers = numClassifiers;
      baseClassifiers = new Classifier[numClassifiers];
      for (int i = 0; i < numClassifiers; i++) {
         RandomTree rt = new RandomTree();
         rt.setSeed(i + 1);
         baseClassifiers[i] = rt;
         //baseClassifiers[i] = new M5P();
         //((M5P) baseClassifiers[i]).setMinNumInstances(1);
      }
      this.random = new Random(seed);
   }


   public void _buildClassifier(Instances data) throws Exception {
      final int minNumInstances = 1;
      final int size = data.size();
      Set<Integer> indices;
      int drawn;
      for (int i = 0; i < numClassifiers; i++) {
         indices = new HashSet<>();
         do {
            for (int j = 0; j < size; j++) {
               if (_bagging_)
                  drawn = this.random.nextInt(size);        //sample with replacement
               else
                  drawn = j;
               indices.add(drawn);
            }
         } while (indices.size() < minNumInstances);
         Instances instances = new Instances(data, 0);  //just copy the arff info
         for (int index : indices) {
            instances.add(new DenseInstance(data.get(index)));
         }
         //System.out.println("Building "+i+" with "+instances.size()+" over "+data.size()+ " "+((double)instances.size() / (double)data.size()));
         this.baseClassifiers[i].buildClassifier(instances);
      }
   }

   @Override
   public void buildClassifier(Instances data) throws Exception {
      for (int i = 0; i < numClassifiers; i++) {
         //We don't reduce the size of the data set
         //We fix the size, but we allow repetitions by sampling with
         //replacement. This is the original definition of Bagging
         final Instances resampled = data.resample(this.random);
         this.baseClassifiers[i].buildClassifier(resampled);
      }
   }

   @Override
   public double classifyInstance(Instance instance) throws Exception {
      double avg = 0;
      double v;
      for (Classifier c : baseClassifiers) {
         v = c.classifyInstance(instance);
         avg += v;
         //System.out.println(v);
      }
      return avg / numClassifiers;
   }

   private double[] classifyInstanceAndGetAllResults(Instance instance) throws Exception {
      double[] ret = new double[numClassifiers + 1];
      int i = 1;
      double v;
      for (Classifier c : baseClassifiers) {
         v = c.classifyInstance(instance);
         //System.out.println("[" + i + "] " + v);
         ret[0] += v;
         ret[i] = v;
         i++;
      }
      ret[0] /= numClassifiers;
      return ret;
   }

   @Override
   public double[] distributionForInstance(Instance instance) throws Exception {
      return new double[]{classifyInstance(instance)};
   }

   public double getStandardDeviation(Instance instance) {
      try {
    	 // this is a constant for dealing with the case when there are
    	 // few samples (i.e., less than 30) and all learners output the
    	 // same prediction. In this particular case, sigma will be zero
    	 // which is fishy because there is not enough knowledge for all 
    	 // learners to be correct and to assume that the model is 100% right
    	 int uncertaintyLevel = 10;	
         double[] all = classifyInstanceAndGetAllResults(instance);
         double avg = all[0];
         double std = 0;
         for (int i = 1; i < all.length; i++) {
            std += (all[i] - avg) * ((all[i] - avg));
         }
         
         /* if there is only 1 sample, there is only 1
          * classifier so sigma has to be high because
          * there is too few knowledge */
         if (numClassifiers <= 1) {
        	 if (avg != 0.0)
        		 return uncertaintyLevel*avg;
        	 else
        		 return 1.0;
         }

         double ret=Math.sqrt(std / (numClassifiers - 1));
         
         /* this corresponds to the case when there are
          * 30 samples or less and the learners all predict
          * the same. Since there is too few knowledge and
          * we don't trust the learners to be correct, we
          * increase the sigma by the uncertainty level
          * times the average */
         if (ret == 0 && all.length <= 30) {
        	 ret = uncertaintyLevel*avg;
         }
         
         return ret;
         /*
         double avg = this.classifyInstance(instance);
         double std = 0;

         for (Classifier c : baseClassifiers) {

            std += Math.pow(c.classifyInstance(instance) - avg, 2);
         }
         return Math.sqrt(std / (numClassifiers - 1));
         */
      } catch (Exception e) {
         throw new RuntimeException(e);
      }
   }

   @Override
   public Capabilities getCapabilities() {
      return null;
   }
}
