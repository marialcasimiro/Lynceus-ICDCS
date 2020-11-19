package weka.cherrypick;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import lynceus.cherrypick.CherrypickConfig;


public class WekaCherrypickConfigFactory {
	

	private final static String data = "data/cherrypick/";
	private static String targetWkld = "CP_kmeans.csv";

	public static CherrypickConfigWekaTestSet buildInitTestSet(String arff) {
		
		FileReader csvData = null;
		int counter = 0;
		
		final CherrypickConfigWekaTestSet testSet = new CherrypickConfigWekaTestSet(arff);
		
		try{
			csvData = new FileReader(new File(data + targetWkld));
		}catch(Exception e) {
			System.out.print("[WekaCherrypickConfigFactory] ");
			e.printStackTrace();
		}
		
		CSVParser parser;
		try {
			parser = CSVParser.parse(csvData, CSVFormat.RFC4180);
			for (CSVRecord csvRecord : parser) {
	        	if(counter >= 1){
		           CherrypickConfig config = CherrypickConfig.parse(csvRecord);
		           testSet.addTestSample(config);
	        	}
	        	counter ++;
	        }
	        return testSet;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return testSet;
		
	}
   
	public static void setTargetWkld(int targetWkld) {
		try {
			File f = new File(data);
	        if (!f.exists() || f.getName().contains(".DS")) {
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
			WekaCherrypickConfigFactory.targetWkld = files.get(targetWkld - 1).getName();
			System.out.println("WKLD set " + targetWkld + " --- " + files.get(targetWkld - 1).getName());
		} catch (Exception e) {
	        throw new RuntimeException(e);
	    }
	}

}
