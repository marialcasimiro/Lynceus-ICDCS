package regression;

public class ExpTrendLine extends OLSTrendLine {
    
	/* superclass abstract methods */
	@Override
    protected double[] xVector(double x) {
        return new double[]{1,x};
    }

    @Override
    protected boolean logY() {return true;}

}

