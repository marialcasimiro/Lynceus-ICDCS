package gh;

/**
 * @author Diego Didona
 * @email diego.didona@epfl.ch
 * @since 09.03.18
 */
public class GaussHermiteQuadrature {

   /*https://github.com/ajt60gaibb/FastGaussQuadrature.jl*/

   /*http://keisan.casio.com/exec/system/1281195844*/

   /* from http://www.efunda.com/math/num_integration/findgausshermite.cfm*/
   private static double[] abscissas_10 = {-3.43616, -2.53273, -1.75668, -1.03661, -0.342901, 0.342901, 1.03661, 1.75668, 2.53273, 3.43616};
   private static double[] total_weight_10 = {1.02545, 0.820666, 0.741442, 0.703296, 0.687082, 0.687082, 0.703296, 0.741442, 0.820666, 1.02545};


   public static GaussHermiteParams decompose(Function f, int p) {
      if (p == 10) {
         return _decompose(f, 10);
      }
      throw new RuntimeException("GH decomposition with " + p + " points is not supported");

   }

   private static GaussHermiteParams _decompose(Function f, int p) {
      final GaussHermiteParams ghp = new GaussHermiteParams(p);
      for (int i = 0; i < p; i++) {
         double v = abscissas_10[i] * f.eval(abscissas_10[i]);
         double w = total_weight_10[i];
         ghp.addValues(i, w, v);
      }
      return ghp;
   }

}
