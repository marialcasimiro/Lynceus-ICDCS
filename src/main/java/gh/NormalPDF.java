package gh;

import org.apache.commons.math3.distribution.NormalDistribution;

/**
 * @author Diego Didona
 * @email diego.didona@epfl.ch
 * @since 10.03.18
 */
public class NormalPDF implements Function {
   final NormalDistribution pdf;

   public NormalPDF(double m, double v) {
      this.pdf = new NormalDistribution(m, v);
   }

   public double eval(double x) {
      return pdf.density(x);
   }
}
