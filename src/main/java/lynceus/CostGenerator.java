package lynceus;

import lynceus.Main.timeout;

/**
 * @author Diego Didona
 * @email diego.didona@epfl.ch
 * @since 12.03.18
 */
public interface CostGenerator<C extends Configuration> {
   double setupCost(State state, C config);

   double deploymentCost(State state, C config);

   double costPerConfigPerMinute(C config);
   
   double[] getIntermediateValues(C config, int amount, timeout timeoutType);

   double getAccForSpecificTime(double time, C config);

}
