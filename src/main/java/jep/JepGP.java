package jep;

import weka.classifiers.Classifier;
import weka.core.Capabilities;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.CSVSaver;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.NominalToBinary;

import java.io.File;

/**
 * @author Diego Didona
 * @email diego.didona@epfl.ch
 * @since 2019-02-15
 */
public class JepGP implements Classifier {
   static ThreadLocal<Jep> jeps;
   static ThreadLocal<String> numpies;

   static Jep theOne = null;

   static {
      jeps = new ThreadLocal<>();
      numpies = new ThreadLocal<>();
      if (false) {
         try {
            theOne = new Jep();
            theOne.eval("import numpy as np1");
            theOne.eval("from sklearn.gaussian_process import GaussianProcessRegressor");
            theOne.eval("from sklearn.gaussian_process.kernels import Matern");
            System.out.println("The one created");
            throw new RuntimeException("Do not use theone. It won't work");
         } catch (JepException e) {
            throw new RuntimeException(e);
         }
      }

   }

   private Instances instances;
   private final String id;

   private final static String norm_y = "True";
   //private final static String alpha = ", alpha=1.0";
   private final static String alpha = "";
   private String numpy_id;

   /*
   Either we use a set of Jeps, but then the problem is that we should build and query all together
   Otherwise a model can be replace between creation and use.

   Maybe we can use different model names.
   gp1...gpN. We synchronize and each Jep basically stores a whole lot of models

   The other solution is: we assign an id to a thread.
   That thread creates its Jep and saves it to static hashmap by index.
   When the thread is closed, it also disposes the Jep in its thread
    */


   /*
   Note. A thread can only have one Jep.
   This is a problem b/c within the same thread we may want to instantiate a model multiple
   times.
   Either we handle this carefully (especially when speculating)
   Or we must be sure that we re-use the interpreter
   E.g., we initialize the interpeter at the beginning of the thread code
   and then we pass it as a reference every time...

   For now, let's try to destroy it and re-create it every time
    */

   public JepGP(String id) {
      this.id = id;
   }

   public void destroyModel() {

   }

   private Instances filter(Instances instances) {
      try {
         final NominalToBinary nominalToBinary = new NominalToBinary();
         nominalToBinary.setInputFormat(instances);
         return Filter.useFilter(instances, nominalToBinary);
      } catch (Exception e) {
         throw new RuntimeException(e);
      }
   }

   private Instance filter(Instance instance) {
      try {
         final NominalToBinary nominalToBinary = new NominalToBinary();
         nominalToBinary.setInputFormat(instances);
         nominalToBinary.input(instance);
         nominalToBinary.batchFinished();
         return nominalToBinary.output();
      } catch (Exception e) {
         throw new RuntimeException(e);
      }
   }


   @Override
   public void buildClassifier(Instances instances) {
      this.instances = instances;

      /*
      Apparently, if the main thread spawns other threads
      if those threads open a new Jep instance, that is not ok
      (I should create a minimal example to check)
      So either we run with a single thread or what can we do?

      Note if I only create a single thread, I have no problem.
      If I create two threads I have problems...
      This is because in the code of Lynceus if thread == 1
      I do not spawn the thread... (Checked)

      WHat if I use a single JEP instance for everybody? There's a lock that should prevent concurrent access
      Still... our code is extremely model-intensive. If we do not allow concurrency in the model invocation
      we're better off using a single thread...
       */

      try {
         Thread currentThread = Thread.currentThread();
         Jep tj;
         if (theOne != null) {
            numpy_id = "1";
            tj = theOne;
         } else {
            if (jeps.get() == null) {
               //System.out.println(currentThread.getName() + " NEW JEP id " + id);
               numpy_id = id;

               JepConfig jepConfig = new JepConfig();
               jepConfig.setRedirectOutputStreams(false);
               //jepConfig.addSharedModules("numpy");
               jeps.set(new Jep(jepConfig));
               numpies.set(numpy_id);
               tj = jeps.get();
               //if (currentThread.getName().equalsIgnoreCase("main")) {
               tj.eval("import numpy as np" + numpy_id);
               tj.eval("from sklearn.gaussian_process import GaussianProcessRegressor");
               tj.eval("from sklearn.gaussian_process.kernels import Matern");
               //}
            } else {
               numpy_id = numpies.get();
               // System.out.println(currentThread.getName() + " OLD JEP  id " + id+" with numpy_id "+numpy_id);
               //System.out.println("ID " + id + " jeps.get() is NOT null");
               tj = jeps.get();
            }
         }

         String trainFile = generateTraiFile();
         //http://www.singularsys.com/jep/doc/javadoc/index.html

         tj.eval("train_data" + id + " = np" + numpy_id + ".loadtxt(fname='" + trainFile + "', delimiter=',', skiprows=1)");
         //this.jep.eval("print train_data"));
         tj.eval("kernel" + id + " = Matern(length_scale=1.0, length_scale_bounds=(1e-4, 1e4), nu=2.5)");
         tj.eval("gp" + id + "= GaussianProcessRegressor(kernel=kernel" + id + ",normalize_y=" + norm_y + alpha + ")");
         tj.eval("last_col" + id + " = np" + numpy_id + ".size(train_data" + id + ", 1)-1");
         tj.eval("X" + id + " = np" + numpy_id + ".delete(train_data" + id + ", last_col" + id + ", 1)");
         tj.eval("Y" + id + " = np" + numpy_id + ".delete(train_data" + id + ", np" + numpy_id + ".s_[0:last_col" + id + "], 1)");
         //Can't understand why the gp.fit leads to printing the params of the gp...
         tj.eval("gp" + id + ".fit(X" + id + ",Y" + id + ")");
         tj.eval("theta=gp"+id+".kernel_.theta");
         Object arr = tj.getValue("theta");
         Double theta = extractDoubleFromPrediction(arr,0);
         //NB: the value is "flattened" and log-transformed. The base is "e", not 10
         System.out.println("theta "+theta);

         //tj.eval("print 'Classifier " + id + " built'");
         //tj.eval("print gp" + id);
      } catch (Exception e) {
         throw new RuntimeException(e);
      }

   }

   private String generateTraiFile() throws Exception {
      Instances train = this.instances;
      NominalToBinary nominalToBinary = new NominalToBinary();
      nominalToBinary.setInputFormat(instances);
      train = Filter.useFilter(train, nominalToBinary);
      CSVSaver csvSaver = new CSVSaver();
      String out = "files/jep/train" + this.id + ".csv";
      File fout = new File(out);
      if (fout.exists()) {
         if (!fout.delete()) {
            throw new RuntimeException("Could not delete existing " + fout.toString());
         }
         if (!fout.createNewFile()) {
            throw new RuntimeException("Could not create new " + fout.toString());
         }
      }
      csvSaver.setFile(fout);
      csvSaver.setInstances(train);
      csvSaver.writeBatch();  //This also closes file and writer
      //System.out.println("File written " + out + " full " + fout.getAbsolutePath());
      return out;
   }


   @Override
   public double classifyInstance(Instance i) {
      Instance filtered = filter(i);
      String string = stringify(filtered);

      try {
         final Jep tj;
         if (theOne == null)
            tj = jeps.get();
         else tj = theOne;
         //Z=np.reshape(Z,(1,-1))
         tj.eval("Z" + id + " = np" + numpy_id + ".reshape(" + string + ",(1,-1))");
         tj.eval("pred" + id + " = gp" + id + ".predict(Z" + id + ",return_std = False)");
         Object arr = (tj.getValue("pred" + id));
         return extractDoubleFromPrediction(arr, 0);
      } catch (Exception e) {
         throw new RuntimeException(e);
      }

   }

   private String stringify(Instance i) {
      final StringBuffer sb = new StringBuffer();
      int a = 0;
      sb.append("[[");
      for (; a < i.numAttributes() - 2; a++) {
         sb.append(i.value(a));
         sb.append(",");
      }
      sb.append(i.value(a));
      //Last attribute is the class attribute, which we do not need
      sb.append("]]");
      return sb.toString();
   }

   public double getStandardDeviation(Instance i) {
      Instance filtered = filter(i);
      String string = stringify(filtered);

      try {
         final Jep tj;
         if (theOne == null)
            tj = jeps.get();
         else tj = theOne;
         //Z=np.reshape(Z,(1,-1))
         tj.eval("Z" + id + " = np" + numpy_id + ".reshape(" + string + ",(1,-1))");
         tj.eval("mean" + id + ",stdv" + id + " = gp" + id + ".predict(Z" + id + ",return_std = True)");
         Object arr = (tj.getValue("stdv" + id));
         return extractDoubleFromPrediction(arr, 0);
      } catch (Exception e) {
         throw new RuntimeException(e);
      }
   }


   private Double extractDoubleFromPrediction(Object pred, int id) {
      /*
                       I don't know why on some machine the return value is a String.
                       Somewhere else it is an NDArray :\
                        */
      Double d;
      if (pred instanceof String) {
         String arrS = ((String) pred).replace("[", "");
         arrS = ((String) arrS).replace("]", "");
         d = Double.parseDouble(arrS);
      } else {
         // System.out.println(arr.getClass());
         d = (Double) (((NDArray<double[]>) pred).getData()[0]);
      }
      return d;
   }


   @Override
   public double[] distributionForInstance(Instance instance) throws Exception {
      return new double[]{classifyInstance(instance)};
   }

   @Override
   public Capabilities getCapabilities() {
      return null;
   }
}
