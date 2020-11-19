package lynceus.aws;

/**
 * @author Diego Didona
 * @email diego.didona@epfl.ch
 * @since 09.04.18
 */
public class AWSDirectory {

   public enum AWSInstanceType {
      M4, C4, R4, R3, I2
   }

   public enum AWSInstanceSize {
      large, xlarge, x2large
   }
   
   public enum AWSInstanceSpeed {
	  slow(0), fast(1);
	  private int id;
	   
	   AWSInstanceSpeed(int i) {
		   id = i;
	   }
   }

   public static AWSInstanceSize sizeFromAttributes(AWSInstanceType type, double vcpus, double ecus, double ram) {
      switch (type) {
         case C4: {
            if (vcpus == 2)
               return AWSInstanceSize.large;
            if (vcpus == 4)
               return AWSInstanceSize.xlarge;
            if (vcpus == 8)
               return AWSInstanceSize.x2large;
         }
         case M4: {
            if (vcpus == 2)
               return AWSInstanceSize.large;
            if (vcpus == 4)
               return AWSInstanceSize.xlarge;
            if (vcpus == 8)
               return AWSInstanceSize.x2large;
         }
         case R4: {
            if (vcpus == 2)
               return AWSInstanceSize.large;
            if (vcpus == 4)
               return AWSInstanceSize.xlarge;
            if (vcpus == 8)
               return AWSInstanceSize.x2large;
         }
         default:
            throw new RuntimeException("Cannot determine size for " + type + " " + vcpus + " " + ecus + " " + ram);

      }
   }
   
   public static AWSInstanceSize sizeFromAttributes(String type, double vcpus, double ecus, double ram) {
	   return sizeFromAttributes(AWSInstanceType.valueOf(type), vcpus, ecus, ram);
   }
   
   public static AWSInstanceSize sizeFromTypeAndCPU(AWSInstanceType type, double vcpus) {
	   switch (type) {
	   	case C4: {
		    if (vcpus == 2)
		       return AWSInstanceSize.large;
		    if (vcpus == 4)
		       return AWSInstanceSize.xlarge;
		    if (vcpus == 8)
		       return AWSInstanceSize.x2large;
		 }
		 case M4: {
		    if (vcpus == 2)
		       return AWSInstanceSize.large;
		    if (vcpus == 4)
		       return AWSInstanceSize.xlarge;
		    if (vcpus == 8)
		       return AWSInstanceSize.x2large;
		 }
		 case R3: {
		     if (vcpus == 2)
		        return AWSInstanceSize.large;
		     if (vcpus == 4)
		        return AWSInstanceSize.xlarge;
		     if (vcpus == 8)
		        return AWSInstanceSize.x2large;
		  }
		 case I2: {
		     if (vcpus == 4)
		        return AWSInstanceSize.xlarge;
		     if (vcpus == 8)
		        return AWSInstanceSize.x2large;
		  }
		
		 default:
		    throw new RuntimeException("Cannot determine size for " + type + " " + vcpus);
	   }
   }
   
   public static AWSInstanceSize sizeFromTypeAndCPU(String type, double vcpus) {
	   return sizeFromTypeAndCPU(AWSInstanceType.valueOf(type), vcpus);
   }

   

   public static double ecusFor(AWSDirectory.AWSInstanceSize size, AWSDirectory.AWSInstanceType type) {
      switch (type) {
         case C4: {
            switch (size) {
               case large:
                  return 8;
               case xlarge:
                  return 16;
               case x2large:
                  return 31;
            }
         }
         case M4: {
            switch (size) {
               case large:
                  return 6.5;
               case xlarge:
                  return 13;
               case x2large:
                  return 26;
            }
         }
         case R4: {
            switch (size) {
               case large:
                  return 7;
               case xlarge:
                  return 13.5;
               case x2large:
                  return 27;
            }
         }
      }
      throw new RuntimeException(type + ", " + size + " not recognized");
   }

   public static double ramFor(AWSDirectory.AWSInstanceSize size, AWSDirectory.AWSInstanceType type) {
      switch (type) {
         case C4: {
            switch (size) {
               case large:
                  return 3.75;
               case xlarge:
                  return 7.5;
               case x2large:
                  return 15.0;
            }
         }
         case M4: {
            switch (size) {
               case large:
                  return 8;
               case xlarge:
                  return 16;
               case x2large:
                  return 32;
            }
         }
         case R4: {
            switch (size) {
               case large:
                  return 15.25;
               case xlarge:
                  return 30.5;
               case x2large:
                  return 61;
            }
         }
         case R3: {
             switch (size) {
                case large:
                   return 15.25;
                case xlarge:
                   return 30.5;
                case x2large:
                   return 61;
             }
          }
         case I2: {
             switch (size) {
                case xlarge:
                   return 30.5;
                case x2large:
                   return 61;
             }
          }
      }
      throw new RuntimeException(type + ", " + size + " not recognized");
   }

   /*
   Taken from https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/ebs-ec2-config.html
    */
   public static double ebsBandwidthFor(AWSDirectory.AWSInstanceSize size, AWSDirectory.AWSInstanceType type) {
      switch (type) {
         case C4: {
            switch (size) {
               case large:
                  return 500;
               case xlarge:
                  return 750;
               case x2large:
                  return 1000;
            }
         }
         case M4: {
            switch (size) {
               case large:
                  return 450;
               case xlarge:
                  return 750;
               case x2large:
                  return 1000;
            }
         }
         case R4: {
            switch (size) {
               case large:
                  return 425;
               case xlarge:
                  return 850;
               case x2large:
                  return 1700;
            }
         }
      }
      throw new RuntimeException(type + ", " + size + " not recognized");
   }


   public static String networkBandwidthFor(AWSDirectory.AWSInstanceSize size, AWSDirectory.AWSInstanceType type) {
      switch (type) {
         case C4: {
            switch (size) {
               case large:
                  return "MODERATE";
               case xlarge:
                  return "HIGH";
               case x2large:
                  return "HIGH";
            }
         }
         case M4: {
            switch (size) {
               case large:
                  return "MODERATE";
               case xlarge:
                  return "HIGH";
               case x2large:
                  return "HIGH";
            }
         }
         case R4: {
            switch (size) {
               case large:
                  return "UP_TO_10";
               case xlarge:
                  return "UP_TO_10";
               case x2large:
                  return "UP_TO_10";
            }
         }
      }
      throw new RuntimeException(type + ", " + size + " not recognized");
   }

   /*
   From https://aws.amazon.com/ec2/instance-types/
    */
   public static double cpuFrequencyFor(AWSDirectory.AWSInstanceSize size, AWSDirectory.AWSInstanceType type) {
      switch (type) {
         case C4: {
            switch (size) {
               case large:
                  return 2.9;
               case xlarge:
                  return 2.9;
               case x2large:
                  return 2.9;
            }
         }
         case M4: {
            switch (size) {
               case large:
                  return 2.4;
               case xlarge:
                  return 2.4;
               case x2large:
                  return 2.4;
            }
         }
         case R4: {
            switch (size) {
               case large:
                  return 2.3;
               case xlarge:
                  return 2.3;
               case x2large:
                  return 2.3;
            }
         }
      }
      throw new RuntimeException(type + ", " + size + " not recognized");
   }


   public static double cpusFor(AWSDirectory.AWSInstanceSize size, AWSDirectory.AWSInstanceType type) {
      switch (type) {
         case C4: {
            switch (size) {
               case large:
                  return 2;
               case xlarge:
                  return 4;
               case x2large:
                  return 8;
            }
         }
         case M4: {
            switch (size) {
               case large:
                  return 2;
               case xlarge:
                  return 4;
               case x2large:
                  return 8;
            }
         }
         case R4: {
            switch (size) {
               case large:
                  return 2;
               case xlarge:
                  return 4;
               case x2large:
                  return 8;
            }
         }
         case R3: {
             switch (size) {
                case large:
                   return 2;
                case xlarge:
                   return 4;
                case x2large:
                   return 8;
             }
          }
         case I2: {
             switch (size) {
                case xlarge:
                   return 4;
                case x2large:
                   return 8;
             }
          }
      }
      throw new RuntimeException(type + ", " + size + " not recognized");
   }

   public static double costPerConfigPerMinute(AWSInstanceType type, AWSInstanceSize size, double numInstances) {
      switch (type) {
         case C4: {
            switch (size) {
               case large:
                  return numInstances * 0.1 / 60.0;
               case xlarge:
                  return numInstances * 0.199 / 60.0;
               case x2large:
                  return numInstances * 0.398 / 60.0;
            }
         }
         case M4: {
            switch (size) {
               case large:
                  return numInstances * 0.1 / 60.0;
               case xlarge:
                  return numInstances * 0.2 / 60.0;
               case x2large:
                  return numInstances * 0.4 / 60.0;
            }
         }
         case R4: {
            switch (size) {
               case large:
                  return numInstances * 0.133 / 60.0;
               case xlarge:
                  return numInstances * 0.266 / 60.0;
               case x2large:
                  return numInstances * 0.532 / 60.0;
            }
         }
      }
      throw new RuntimeException(type + "," + size + " not recognized");
   }
   
   public static double cherrypickCostPerConfigPerMinute(AWSInstanceType type, AWSInstanceSize size, double numInstances) {
	   switch (type) {
       case C4: {
          switch (size) {
             case large:
                return numInstances * 0.138 / 60.0;
             case xlarge:
                return numInstances * 0.276 / 60.0;
             case x2large:
                return numInstances * 0.552 / 60.0;
          }
       }
       case M4: {
          switch (size) {
             case large:
                return numInstances * 0.147 / 60.0;
             case xlarge:
                return numInstances * 0.294 / 60.0;
             case x2large:
                return numInstances * 0.588 / 60.0;
          }
       }
       case R3: {
           switch (size) {
              case large:
                 return numInstances * 0.195 / 60.0;
              case xlarge:
                 return numInstances * 0.39 / 60.0;
              case x2large:
                 return numInstances * 0.78 / 60.0;
           }
        }
       case I2: {
           switch (size) {
              case xlarge:
                 return numInstances * 0.938 / 60.0;
              case x2large:
                 return numInstances * 1.876 / 60.0;
           }
        }
    }
    throw new RuntimeException(type + "," + size + " not recognized");
   }
   
   public static AWSInstanceSpeed speedForType(AWSInstanceType type) {
	      switch (type) {
	         case C4: {
	            return AWSInstanceSpeed.fast;
	         }
	         case M4: {
	        	 return AWSInstanceSpeed.slow;
	         }
	         case R3: {
	        	 return AWSInstanceSpeed.slow;
	          }
	         case I2: {
	        	 return AWSInstanceSpeed.slow;
	          }

	         default:
	            throw new RuntimeException("Cannot determine speed for " + type);
	      }
   }
   
}
