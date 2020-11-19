package lynceus.results;

import lynceus.Lynceus;

import java.util.Objects;

/**
 * @author Diego Didona
 * @email diego.didona@epfl.ch
 * @since 14.03.18
 */
public class ExpParam implements java.io.Serializable {
   
   private final double budget;
   private final double epsilon;
   private final long seed;
   private final long wkldId;
   private final Lynceus.optimizer _optimizer;


   public ExpParam(double budget, double epsilon, long seed, long wkldId, Lynceus.optimizer opt) {
      this.budget = budget;
      this.epsilon = epsilon;
      this.seed = seed;
      this.wkldId = wkldId;
      this._optimizer = opt;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      ExpParam expParam = (ExpParam) o;
      return Double.compare(expParam.budget, budget) == 0 &&
            Double.compare(expParam.epsilon, epsilon) == 0 &&
            seed == expParam.seed &&
            wkldId == expParam.wkldId &&
            _optimizer == expParam._optimizer;
   }

   @Override
   public int hashCode() {

      return Objects.hash(budget, epsilon, seed, wkldId, _optimizer);
   }

   public double getBudget() {
      return budget;
   }

   public double getEpsilon() {
      return epsilon;
   }

   public long getSeed() {
      return seed;
   }

   public long getWkldId() {
      return wkldId;
   }

   public Lynceus.optimizer get_optimizer() {
	   return _optimizer;
   }

@Override
   public String toString() {
	   return "ExpParam [budget=" + budget + ", epsilon=" + epsilon + ", seed=" + seed + ", wkldId=" + wkldId
			   + ", _optimizer=" + _optimizer + "]";
   }
   
}
