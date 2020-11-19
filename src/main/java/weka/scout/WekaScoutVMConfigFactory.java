package weka.scout;

import lynceus.scout.ScoutVMConfig;
import lynceus.scout.ScoutVMCostGenerator;
import weka.scout.ScoutVMConfigWekaTestSet;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

/**
 * @author Diego Didona
 * @email diego.didona@epfl.ch
 * @since 09.04.18
 */
public class WekaScoutVMConfigFactory {
   /*
   All the workloads have the same set of potential configurations
   So to get the set of possible vm configs, I can look at the content of a random workload
    */
   private final static String data = "data/scout/osr_multiple_nodes_original/";
   private static String targetWkld = "join_bigdata_spark";

   public static ScoutVMConfigWekaTestSet buildInitTestSet(String arff) {

      try {
         final ScoutVMConfigWekaTestSet testSet = new ScoutVMConfigWekaTestSet(arff);
         String folder = WekaScoutVMConfigFactory.data + WekaScoutVMConfigFactory.targetWkld;
         
         File f = new File(folder);
         File[] files = f.listFiles();
         assert files != null;
         for (File ff : files) {
            if(ff.getName().contains(".DS"))
               continue;
            ScoutVMConfig c = ScoutVMConfig.parseName(ff.getName());
            testSet.addTestSample(c);
         }
         
         return testSet;

      } catch (Exception e) {
         e.printStackTrace();
         throw new RuntimeException(e);
      }
   }
   
   public static void setTargetWkld(int targetWkld) {
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
	         WekaScoutVMConfigFactory.targetWkld = files.get(targetWkld - 1).getName();
	         System.out.println("WKLD set " + targetWkld + " --- " + files.get(targetWkld - 1).getName());

	      } catch (Exception e) {
	         throw new RuntimeException(e);
	      }
   }


}
