package regression;

public class LinTrendLine implements TrendLine{
	
	// this class should only be used when there are only 2 points for the regression
	// and one of them is the origin
	
	double[] yAxis;	// acc
	double[] xAxis;	// time
	double	 a;		// slope of the line
	
	public LinTrendLine() {
		
	}

	public void setValues(double[] y, double[] x) {
		
		if (y.length < 2) {
			System.out.println("[ERROR] not enough data: only " + y.length + " point");
			return;
		}
		
		yAxis = y;
		xAxis = x;
		
		a = y[1]/x[1];
		// because the first point is the origin b = 0
		
//		yAxis = new double[y.length];
//		xAxis = new double[x.length];
//		
//		for(int i = 0; i < x.length; i++) {
//			yAxis[i] = y[i];
//		}
	}
	
	public double predict(double x) {
		// Y = aX + b
		return x*a;
	}
	
}
