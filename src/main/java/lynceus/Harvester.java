package lynceus;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import lynceus.results.ExpParam;
import lynceus.results.OptResult;

public class Harvester {

	public static void main(String[] args) {
		
		final int numSeeds = 5;
		int numWklds = 10;
	    int initWklds = 2;
		long[] seeds = new long[numSeeds];
		final Random shuffleRandom = new Random(30031987);
	    List<Integer> wklds = new ArrayList<>();
	    HashMap<ExpParam, OptResult> hL = new HashMap<>();
	
	    String fileName;
	    
	    
		
		for (int i = initWklds; i <= numWklds; i++) {      //Start from wkld 2!!
	         wklds.add(i);
	    }
    	Collections.shuffle(wklds, shuffleRandom);

	    for (int id : wklds) {		
	        for (int s = 1; s <= numSeeds; s++) {
	           seeds[s - 1] = s;
	        }
	        		
	
	        fileName = "files/hashMaps/wkld" + id + ".txt";
	    	FileInputStream fileInputStream;
			try {
				fileInputStream = new FileInputStream(fileName);
				ObjectInputStream objectInputStream;
				try {
					objectInputStream = new ObjectInputStream(fileInputStream);
					try {
						hL = (HashMap) objectInputStream.readObject();
					} catch (ClassNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					try {
						objectInputStream.close();
						fileInputStream.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	        
	        
			/* Print results for workload */
	     
//	        Iterator<ExpParam> itr = hL.keySet().iterator();
//	        while(itr.hasNext()){
//	       		System.out.println(itr.next());
//	        }
//	        System.out.println("Size of the map: " + hL.size());
			printResults(hL, seeds);
	         
		}
	}


	private static void printResults(HashMap<ExpParam, OptResult> hL, long[] seeds) {
		double avgDist = 0, avgExpl = 0, avgCost = 0;
		double p99Dist, p99Expl, p99Cost;
		double p90Dist, p90Expl, p90Cost;
		int i = 0;

		double[] dists = new double[seeds.length];
		double[] expls = new double[seeds.length];	
		double[] costs = new double[seeds.length];
		double withinTMax = 0;
 
		Lynceus.optimizer opt = null;
		double b = 0.0;
		double e = 0.0;
		long id = 0;
		Iterator<ExpParam> itr = hL.keySet().iterator();
  
		while(itr.hasNext()){
   		 	
	   		ExpParam expParam = itr.next();		// each expParam represents a different seed
	   		opt = expParam.get_optimizer();
	   		b = expParam.getBudget();
	   		e = expParam.getEpsilon();
	   		id = expParam.getWkldId();
	   		//System.out.println(expParam);
	        OptResult o = hL.get(expParam);
	        if (o == null) {
	        	System.out.println("Continuing...");
	        	continue;
	        }
	        dists[i] = o.getDistFromOpt();
	        expls[i] = o.getExplNumExpls();
	        costs[i] = o.getExplCost();
	        avgDist += o.getDistFromOpt();
	        avgExpl += o.getExplNumExpls();
	        avgCost += o.getExplCost();
	        if (o.isWithinTmax()) {
	           withinTMax++;
	        }
	        i++;
   	 	}
		
		if (dists.length == 0)
	        return;
	    avgDist /= seeds.length;
	    avgCost /= seeds.length;
	    avgExpl /= seeds.length;
	    Arrays.sort(dists);
	    Arrays.sort(expls);
	    Arrays.sort(costs);
	    int index_99 = (int) Math.floor((99.0 / 100) * ((double) seeds.length));
	    if (index_99 == seeds.length)
	       index_99 = seeds.length - 2; //not the max
	
	    p99Cost = costs[index_99];
	    p99Expl = expls[index_99];
	    p99Dist = dists[index_99];
	
	    int index_90 = (int) Math.floor((90.0 / 100) * ((double) seeds.length));
	    if (index_90 == seeds.length)
	       index_90 = seeds.length - 2; //not the max
	
  	    p90Cost = costs[index_90];
	    p90Expl = expls[index_90];
	    p90Dist = dists[index_90];
	
	    System.out.println("--------------- NEW WKLD: " + id + " ---------------");
	    System.out.println("Optimizer = " + opt + " ; budget = " + b + " ; epsilon = " + e + " ; within TMax = " + withinTMax + " ; nb_seeds = " + seeds.length);
	    System.out.println("\tavgDist = " + avgDist + " ; avgExpl = " + avgExpl + " ; avgCost = " + avgCost);
	    System.out.println("\tp90Dist = " + p90Dist + " ; p90Expl = " + p90Expl + " ; p90Cost = " + p90Cost);
	    System.out.println("\tp99Dist = " + p99Dist + " ; p99Expl = " + p99Expl + " ; p99Cost = " + p99Cost + "\n");  

	}

}
