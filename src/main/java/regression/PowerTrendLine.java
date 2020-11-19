package regression;

public class PowerTrendLine extends OLSTrendLine {
    
	/* superclass abstract methods */
	@Override
    protected double[] xVector(double x) {
        return new double[]{1,Math.log(x)};
    }

    @Override
    protected boolean logY() {return true;}

}
