package regression;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression;

import lynceus.Main;
import lynceus.tensorflow.TensorflowConfig;
import lynceus.tensorflow.TensorflowConfigCostGenerator;
import lynceus.tensorflow.TensorflowDatapoint;
import weka.tensorflow.TensorflowConfigWekaTestSet;
import weka.tensorflow.WekaTensorflowConfigFactory;

public abstract class OLSTrendLine implements TrendLine{
	
	/* class attributes */
	RealMatrix coef = null; // will hold prediction coefs once we get values
	
	/* abstract methods */
    protected abstract double[] xVector(double x); // create vector of values from x
    protected abstract boolean logY(); // set true to predict log of y (note: y must be positive)
    
    /* interface methods */
    @Override
    public void setValues(double[] y, double[] x) {
        if (x.length != y.length) {
            throw new IllegalArgumentException(String.format("The numbers of y and x values must be equal (%d != %d)",y.length,x.length));
        }
        double[][] xData = new double[x.length][]; 
        for (int i = 0; i < x.length; i++) {
            // the implementation determines how to produce a vector of predictors from a single x
            xData[i] = xVector(x[i]);
        }
        if(logY()) { // in some models we are predicting ln y, so we replace each y with ln y
            y = Arrays.copyOf(y, y.length); // user might not be finished with the array we were given
            for (int i = 0; i < x.length; i++) {
                y[i] = Math.log(y[i]);
            }
        }
        OLSMultipleLinearRegression ols = new OLSMultipleLinearRegression();
        ols.setNoIntercept(true); // let the implementation include a constant in xVector if desired
        ols.newSampleData(y, xData); // provide the data to the model
        coef = MatrixUtils.createColumnRealMatrix(ols.estimateRegressionParameters()); // get our coefs
    }

    @Override
    public double predict(double x) {
        double yhat = coef.preMultiply(xVector(x))[0]; // apply coefs to xVector
        if (logY()) yhat = (Math.exp(yhat)); // if we predicted ln y, we still need to get y
        return yhat;
    }
   
    
    
    
    
    public static void main(String[] args) {
    	
//    	TrendLine linear = new PolyTrendLine(1);
    	TrendLine polinomial = new PolyTrendLine(2);
//    	TrendLine polinomial3 = new PolyTrendLine(3);
//    	TrendLine polinomial4 = new PolyTrendLine(4);
    	//TrendLine polinomial5 = new PolyTrendLine(5);
    	//TrendLine polinomial6 = new PolyTrendLine(6);
    	TrendLine logarithmic = new LogTrendLine();
    	
    	int			timeStep = 0;
    	double		trueValue = 0;
    	double[]	yAxis;	// time
    	double[]	xAxis;	// accuracy
    	String 		inputFile = "t2_rnn.csv";
    	//String 		inputFile = "t2_cnn_intermediate_values.csv";
    	//String 		inputFile = "t2_multilayer_intermediate_values.csv";
    	TensorflowConfig 	c = null;
    	ArrayList<Double>	values = new ArrayList<Double> ();
    	
    	
    	TensorflowConfigCostGenerator costGenerator = null;
		try {
			costGenerator = new TensorflowConfigCostGenerator("files/" + inputFile);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    	TensorflowConfigWekaTestSet testSet = WekaTensorflowConfigFactory.buildInitTestSet("files/tensorflow.arff");
    	
    	PrintWriter writer = null;
    	
    	String file = "files/" + inputFile + "_timeout_study.txt";
		File f = new File(file);
		if (f.exists()) {
			f.renameTo(new File(f.getAbsolutePath().concat(".").concat(String.valueOf(System.currentTimeMillis())).concat(".txt")));
		}
		try {
			writer = new PrintWriter(file, "UTF-8");
		} catch (FileNotFoundException | UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		writer.println("numPointsForPrediction;configIndex;config;regressionType;estimatedValue;trueValue;estimationError");
		writer.flush();
    	
		
		for (int i = 4 ; i < 21 ; i++) {
	    	for (int j = 0; j < testSet.size(); j++) {
	    		c = testSet.getConfig(j);
	    		values = TensorflowDatapoint.findDatapoint(c, costGenerator.getDataset()).getIntermediateValues();	// x Axis values
	    		
	    		if (i > values.size()) {
	    			continue;
	    		} else {
	    		
		    		yAxis = new double[i];
		    		xAxis = new double[i];
		    		yAxis[0] = xAxis[0] = 0;
		    		
		    		if (TensorflowDatapoint.findDatapoint(c, costGenerator.getDataset()).getAccuracy() < 0.85) {
		    			continue;
		    		} else {
		    			trueValue = TensorflowDatapoint.findDatapoint(c, costGenerator.getDataset()).getPerformance();
		    			timeStep = (int) Math.round(trueValue/values.size());
			    		
			    		for (int k = 1; k < i; k++) {
			    			xAxis[k] = values.get(k-1);
			    			yAxis[k] = yAxis[k-1] + timeStep;
			    		}

//			    		linear.setValues(yAxis, xAxis);
			    		polinomial.setValues(yAxis, xAxis);
//			    		polinomial3.setValues(yAxis, xAxis);
//			    		polinomial4.setValues(yAxis, xAxis);
			    		//polinomial5.setValues(yAxis, xAxis);
			    		//polinomial6.setValues(yAxis, xAxis);
			    		double[] y = new double[yAxis.length-1];
			    		double[] x = new double[xAxis.length-1];
			    		for (int k = 1; k < yAxis.length; k++) {
			    			y[k-1] = yAxis[k];
			    			x[k-1] = xAxis[k];
			    		}
			    	
			    		logarithmic.setValues(y, x);
//			    		writer.println(i + ";" + j + ";" + c + ";linear;" + linear.predict(0.85) + ";" + trueValue + ";" + Math.abs(trueValue-linear.predict(0.85)));
//			    		writer.println(i + ";" + j + ";" + c + ";polinomial;" + polinomial.predict(0.85) + ";" + trueValue + ";" + Math.abs(trueValue-polinomial.predict(0.85)));
//			    		writer.println(i + ";" + j + ";" + c + ";polinomial3;" + polinomial3.predict(0.85) + ";" + trueValue + ";" + Math.abs(trueValue-polinomial3.predict(0.85)));
//			    		writer.println(i + ";" + j + ";" + c + ";polinomial4;" + polinomial4.predict(0.85) + ";" + trueValue + ";" + Math.abs(trueValue-polinomial4.predict(0.85)));
			    		//writer.println(i + ";" + j + ";" + c + ";polinomial5;" + polinomial5.predict(0.85) + ";" + trueValue + ";" + Math.abs(trueValue-polinomial5.predict(0.85)));
			    		//writer.println(i + ";" + j + ";" + c + ";polinomial6;" + polinomial6.predict(0.85) + ";" + trueValue + ";" + Math.abs(trueValue-polinomial6.predict(0.85)));
			    		writer.println(i + ";" + j + ";" + c + ";logarithmic;" + logarithmic.predict(0.85) + ";" + trueValue + ";" + Math.abs(trueValue-logarithmic.predict(0.85)));
		    		}
	    		}
	    	}
		}	
    	
    }
	
}



