package lynceus;

/**
 * @author Diego Didona
 * @email diego.didona@epfl.ch
 * @since 11.03.18
 */
public class SamplingResult<C extends Configuration> {

   private double executionCost;
   private double executioTime;
   private C config;
   private double executionRealCost;

   public SamplingResult(double target_y, double executioTime, C config, double executionFullCost) {
      this.executionCost = target_y;
      this.executioTime = executioTime;
      this.config = config;
      this.executionRealCost = executionFullCost;
   }
   
   /** 
    * 
    * @return predicted cost to run the job until the end
    */
   public double getExecutionCost() {
      return executionCost;
   }

   /**
    * 
    * @return predicted time to run the job until the end
    */
   public double getExecutioTime() {
      return executioTime;
   }

   public C getConfig() {
      return config;
   }

   /**
    * 
    * @return budget spent exploring/sampling the configuration
    */
   public double getExecutionRealCost(){
	   return executionRealCost;
   }

	public void setExecutionCost(double executionCost) {
		this.executionCost = executionCost;
	}
   
   
	
}
