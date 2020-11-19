package weka.gauss;

import no.uib.cipr.matrix.DenseCholesky;
import no.uib.cipr.matrix.DenseVector;
import no.uib.cipr.matrix.Matrices;
import no.uib.cipr.matrix.UpperSPDDenseMatrix;
import no.uib.cipr.matrix.UpperTriangDenseMatrix;
import no.uib.cipr.matrix.Vector;
import weka.Matern32Kernel;
import weka.classifiers.functions.GaussianProcesses;
import weka.classifiers.functions.supportVector.CachedKernel;
import weka.classifiers.functions.supportVector.Kernel;
import weka.core.Capabilities;
import weka.core.Instance;
import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.NominalToBinary;
import weka.tuning.ModelParams;

import java.util.logging.LogManager;

/**
 * @author Diego Didona
 * @email diego.didona@epfl.ch
 * @since 2019-02-13
 */
public class CustomGP extends GaussianProcesses {

   static {
      LogManager.getLogManager().reset(); //TO remove te logging of  org.netlib.
   }

   public double getTheta() {
      return ((Matern32Kernel) this.m_kernel).getSl2();
   }


   private static final boolean zeroMean = true;


   CustomGP() {
      m_checksTurnedOff = true;
   }

   /**
    * Computes standard deviation for given instance, without transforming target back into original space.
    */
   protected double computeStdDev(Instance inst, Vector k) throws Exception {


      /*  // == > weka code
      double kappa = m_actualKernel.eval(-1, -1, inst) + m_deltaSquared;

      double s = m_L.mult(k, new DenseVector(k.size())).dot(k);

      double sigma = m_delta;
      if (kappa > s) {
         sigma = Math.sqrt(kappa - s);
      }


      return sigma;   */


      // => textbook
      double kappa = m_actualKernel.eval(-1, -1, inst);
      double s = m_L.mult(k, new DenseVector(k.size())).dot(k);
      return Math.sqrt(kappa - s);


   }


   public ModelParams getModelParams() {
      Matern32Kernel m32 = (Matern32Kernel) this.m_kernel;
      return new ModelParams(m32.getSl2(), m32.getSf2());
   }

   double LML(Instances insts) throws Exception {

      m_Missing = null;

      if (getCapabilities().handles(Capabilities.Capability.NUMERIC_ATTRIBUTES)) {
         boolean onlyNumeric = true;
         if (!m_checksTurnedOff) {
            for (int i = 0; i < insts.numAttributes(); i++) {
               if (i != insts.classIndex()) {
                  if (!insts.attribute(i).isNumeric()) {
                     onlyNumeric = false;
                     break;
                  }
               }
            }
         }

         if (!onlyNumeric) {
            m_NominalToBinary = new NominalToBinary();
            m_NominalToBinary.setInputFormat(insts);
            insts = Filter.useFilter(insts, m_NominalToBinary);
         } else {
            m_NominalToBinary = null;
         }
      } else {
         m_NominalToBinary = null;
      }

      m_Filter = null;

      m_NumTrain = insts.numInstances();


      // Initialize kernel
      m_actualKernel = Kernel.makeCopy(m_kernel);
      if (m_kernel instanceof CachedKernel) {
         ((CachedKernel) m_actualKernel).setCacheSize(-1); // We don't need a cache at all
      }
      m_actualKernel.buildKernel(insts);

      // Compute average target value
      double sum = 0.0;
      for (int i = 0; i < insts.numInstances(); i++) {
         sum += insts.instance(i).classValue();
      }
      if (zeroMean) {
         m_avg_target = sum / (double) insts.numInstances();
      } else {
         m_avg_target = 0;
      }

      // Store squared noise level
      m_deltaSquared = m_delta * m_delta;


      // initialize kernel matrix/covariance matrix
      int n = insts.numInstances();
      m_L = new UpperSPDDenseMatrix(n);
      for (int i = 0; i < n; i++) {
         for (int j = i + 1; j < n; j++) {
            m_L.set(i, j, m_actualKernel.eval(i, j, insts.instance(i)));
         }
         m_L.set(i, i, m_actualKernel.eval(i, i, insts.instance(i)) + m_deltaSquared);
      }

      // Compute inverse of kernel matrix
      m_L = new DenseCholesky(n, true).factor((UpperSPDDenseMatrix) m_L).solve(Matrices.identity(n));
      m_L = new UpperSPDDenseMatrix(m_L); // Convert from DenseMatrix

      // Compute t
      Vector tt = new DenseVector(n);
      for (int i = 0; i < n; i++) {
         tt.set(i, (insts.instance(i).classValue() - m_avg_target));
      }
      m_t = m_L.mult(tt, new DenseVector(insts.numInstances()));


      Vector yT = tt.copy();


      //Compute the det via decomposition.
      //We stll keep the old code we used to double check
      final UpperTriangDenseMatrix up = new DenseCholesky(n, true).factor((UpperSPDDenseMatrix) m_L).getU();


      double determinant = 1;
      for (int l = 0; l < n; l++) {
         determinant *= up.get(l, l);
      }

      /*
      NOTE: mL is (K + sigma I )^(-1). We use it for mT
      We need the determinant of the UNinverted  (K + sigma I)
      Guess what? det(A)^(-1) = -det(A)
      So we compute the determinant of mL and  we change the sign
       */

      double logDet = -1 * (2 * Math.log(determinant)); //log(x^2) = 2log(x)
      //System.out.println("LogDet is " + logDet);

      /*
      double logDet = 0;
      for(int l=0;l<n;l++){
          logDet+=Math.log(up.get(l,l));
      }
      logDet*=2;

      System.out.println("LogDet is " + logDet);
      */

      /*
      Matrix mm = new Matrix(m_L.numRows(), m_L.numColumns());
      for (int r = 0; r < m_L.numRows(); r++) {
         for (int c = 0; c < m_L.numColumns(); c++) {
            mm.set(r, c, m_L.get(r, c));
         }
      }
      double det = mm.det();
      System.out.println("Det2 is " + det); */

      //It seems that the determinant is the same as in sklearn
      //log_likelihood_dims -= np.log(np.diag(LML)).sum()
      //If I see the convergence of the log for value "theta" I get the same value
      //as if I put exp(theta) as length in my kernel

      //This is also confirmed to be the same
      //log_likelihood_dims = -0.5 * np.einsum("ik,ik->k", y_train, alpha)
      //System.out.println("First is " + (yT.dot(m_t)));

      //And also this.
      final double constant = insts.numInstances() * Math.log(2 * Math.PI);

      //System.out.println("Constant is " + constant);
      double l = (yT.dot(m_t) + logDet + constant);
      l = -0.5 * l;
      double theta = ((Matern32Kernel) this.m_kernel).getSl2();
      //double logTheta = Math.log(theta);
      //System.out.println("W=FIT: " + theta + ", " + logTheta + " ( logL " + l + " )");
      return l;
   }


   /**
    * Method for building the classifier.
    *
    * @param insts the set of training instances
    * @throws Exception if the classifier can't be built successfully
    */
   @Override
   public void buildClassifier(Instances insts) throws Exception {

	  /** m_Missing:	The filter used to get rid of missing values. */
      m_Missing = null;

      /* Assure that the data is numeric */
      if (getCapabilities().handles(Capabilities.Capability.NUMERIC_ATTRIBUTES)) {
         boolean onlyNumeric = true;
         if (!m_checksTurnedOff) {
            for (int i = 0; i < insts.numAttributes(); i++) {
               if (i != insts.classIndex()) {
                  if (!insts.attribute(i).isNumeric()) {
                     onlyNumeric = false;
                     break;
                  }
               }
            }
         }
         
         /** m_NominalToBinary:	The filter used to make attributes numeric. */
         if (!onlyNumeric) {
            m_NominalToBinary = new NominalToBinary();
            m_NominalToBinary.setInputFormat(insts);
            insts = Filter.useFilter(insts, m_NominalToBinary);
         } else {
            m_NominalToBinary = null;
         }
      } else {
         m_NominalToBinary = null;
      }

      /** m_Filter:	The filter used to standardize/normalize all values. */
      m_Filter = null; //No norm, no standard

      /** m_NumTrain:	The number of training instances */
      m_NumTrain = insts.numInstances();

      // Initialize kernel
      /** m_actualKernel: Actual kernel object to use */
      m_actualKernel = Kernel.makeCopy(m_kernel);
      /** m_kernel:	Template of kernel to use */
      if (m_kernel instanceof CachedKernel) {
         ((CachedKernel) m_actualKernel).setCacheSize(-1); // We don't need a cache at all
      }
      m_actualKernel.buildKernel(insts);

      // Compute average target value
      double sum = 0.0;
      for (int i = 0; i < insts.numInstances(); i++) {
         sum += insts.instance(i).classValue();
      }
      /** m_avg_target: The training data. */
      if (zeroMean) {
         m_avg_target = sum / (double) insts.numInstances();
      } else {
         m_avg_target = 0;
      }

      // Store squared noise level
      /** m_deltaSquared:	The squared noise value. */
      m_deltaSquared = m_delta * m_delta;

      // initialize kernel matrix/covariance matrix
      int n = insts.numInstances();
      m_L = new UpperSPDDenseMatrix(n);	// covariance matrix
      //System.out.println("W=Noise " + m_deltaSquared);
      for (int i = 0; i < n; i++) {
         for (int j = i + 1; j < n; j++) {
            m_L.set(i, j, m_actualKernel.eval(i, j, insts.instance(i)));
         }
         m_L.set(i, i, m_actualKernel.eval(i, i, insts.instance(i)) + m_deltaSquared);
      }
      //System.out.println("W=K(X,X)\n" + m_L);

      // Compute inverse of kernel matrix
      // new DenseCholesky(n, true): n: matrix size; true: for decomposing an upper symmettrical matrix
      // factor(m_L): calculates a cholesky decomposition
      // solve(I(n): solves for I(n), overwriting on return
      m_L = new DenseCholesky(n, true).factor((UpperSPDDenseMatrix) m_L).solve(Matrices.identity(n));
      m_L = new UpperSPDDenseMatrix(m_L); // Convert from DenseMatrix

      // Compute t
      Vector tt = new DenseVector(n);
      for (int i = 0; i < n; i++) {
         tt.set(i, (insts.instance(i).classValue() - m_avg_target));
      }
      /** m_t:	The vector of target values. */
      m_t = m_L.mult(tt, new DenseVector(insts.numInstances()));

      //System.out.println("W=alpha " + m_t);

   } // buildClassifier

   /**
    * Classifies a given instance.
    *
    * @param inst the instance to be classified
    * @return the classification
    * @throws Exception if instance could not be classified successfully
    */
   @Override
   public double classifyInstance(Instance inst) throws Exception {

      // Filter instance
      inst = filterInstance(inst);

      // Build K vector
      /** m_actualKernel:	Actual kernel object to use */
      Vector k = new DenseVector(m_NumTrain);
      for (int i = 0; i < m_NumTrain; i++) {
         k.set(i, m_actualKernel.eval(-1, i, inst));
      }

      /** m_t: 			The vector of target values. */
      /** m_avg_target:	The training data. */
      /* k.dot(m_t) = k_transposed * m_t */
      double result = (k.dot(m_t) + m_avg_target);

      if (result < 0) {
         //System.out.println("Negative " + inst + " <" + result + ", " + this.getStandardDeviation(inst) + ">");
         System.out.println("Negative");
         NominalToBinary nominalToBinary = new NominalToBinary();
         nominalToBinary.setInputFormat(inst.dataset());
         Instances instances = new Instances(inst.dataset());
         instances.clear();
         instances.add(inst);
         instances = Filter.useFilter(instances, nominalToBinary);
         System.out.println(instances.get(0));
      }

      return result;

   }

   public double getStandardDeviation(Instance inst) throws Exception {

      inst = filterInstance(inst);

      // Build K vector (and Kappa)
      Vector k = new DenseVector(m_NumTrain);
      for (int i = 0; i < m_NumTrain; i++) {
         k.set(i, m_actualKernel.eval(-1, i, inst));
      }

      return computeStdDev(inst, k);
   }
}
