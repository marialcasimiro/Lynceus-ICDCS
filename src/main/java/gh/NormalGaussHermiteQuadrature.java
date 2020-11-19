package gh;

/**
 * @author Diego Didona
 * @email diego.didona@epfl.ch
 * @since 10.03.18
 */
public class NormalGaussHermiteQuadrature {

   public static GaussHermiteParams _decompose(int p, double m, double s) {
      final Function f = new NormalPDF(m, s);
      return GaussHermiteQuadrature.decompose(f, p);
   }

   /*
   https://keisan.casio.com/exec/system/1281195844
    */
   public static GaussHermiteParams decompose(int p, double mu, double s2) {

      if (p == 1) {
         final GaussHermiteParams ghp = new GaussHermiteParams(p);
         ghp.addValues(0, 1, mu);
         return ghp;
      }
      if (p == 3){
    	 final GaussHermiteParams ghp = new GaussHermiteParams(p);
    	 final double sqrt2 = Math.sqrt(2.0);
         final double sqrtpi = Math.sqrt(Math.PI);
         final double stdv = Math.sqrt(s2);
    	 ghp.addValues(0, 0.29540897, (sqrt2 * stdv * -1.22474487 + mu) / sqrtpi);
         ghp.addValues(1, 1.18163590, (sqrt2 * stdv * 0 + mu) / sqrtpi);
         ghp.addValues(2, 0.29540897, (sqrt2 * stdv * 1.22474487 + mu) / sqrtpi);
    	 return ghp;
      }
      if (p == 5) {
         final GaussHermiteParams ghp = new GaussHermiteParams(p);
         final double sqrt2 = Math.sqrt(2.0);
         final double sqrtpi = Math.sqrt(Math.PI);
         final double stdv = Math.sqrt(s2);
         ghp.addValues(0, 0.0199532, (sqrt2 * stdv * -2.02018 + mu) / sqrtpi);
         ghp.addValues(1, 0.393619, (sqrt2 * stdv * -0.958573 + mu) / sqrtpi);
         ghp.addValues(2, 0.945309, (sqrt2 * stdv * 0 + mu) / sqrtpi);
         ghp.addValues(3, 0.393619, (sqrt2 * stdv * 0.958573 + mu) / sqrtpi);
         ghp.addValues(4, 0.0199532, (sqrt2 * stdv * 2.02018 + mu) / sqrtpi);
         return ghp;
      }
      throw new IllegalArgumentException("GHP only with p = 1, p = 3 or p = 5");

   }
   
   public static void main(String[] args) {
	   	double mu = 100;
	  	double s = 40;
	  	GaussHermiteParams g = decompose(5, mu, s);
	  	g.checkIntegral(mu);
   }
   
}
