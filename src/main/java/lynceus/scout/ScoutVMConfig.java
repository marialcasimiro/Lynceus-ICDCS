package lynceus.scout;

import lynceus.Configuration;
import lynceus.aws.AWSDirectory;

import java.util.Objects;

import static lynceus.aws.AWSDirectory.*;
import static lynceus.aws.AWSDirectory.cpuFrequencyFor;
import static lynceus.aws.AWSDirectory.ebsBandwidthFor;

/**
 * @author Diego Didona
 * @email diego.didona@epfl.ch
 * @since 24.04.18
 */
public class ScoutVMConfig implements Configuration {

   /* class attributes */
   private AWSDirectory.AWSInstanceType type;
   private AWSDirectory.AWSInstanceSize size;
   private double vcpus;
   private double ecus;
   private double frequency;
   private double ram;
   private double ebs_band;
   private String net_band;
   private double num_instances;

   /* class constructors */
   public ScoutVMConfig(AWSDirectory.AWSInstanceType type, AWSDirectory.AWSInstanceSize size, double vcpus, double ecus, double frequency, double ram, double ebs_band, String net_band, double num_instances) {
      this.type = type;
      this.size = size;
      this.vcpus = vcpus;
      this.ecus = ecus;
      this.frequency = frequency;
      this.ram = ram;
      this.ebs_band = ebs_band;
      this.net_band = net_band;
      this.num_instances = num_instances;
   }

   /* getters */
   public AWSDirectory.AWSInstanceType getType() {
      return type;
   }

   public AWSDirectory.AWSInstanceSize getSize() {
      return size;
   }

   public double getVcpus() {
      return vcpus;
   }

   public double getEcus() {
      return ecus;
   }

   public double getFrequency() {
      return frequency;
   }

   public double getRam() {
      return ram;
   }

   public double getEbs_band() {
      return ebs_band;
   }

   public String getNet_band() {
      return net_band;
   }

   public double getNum_instances() {
      return num_instances;
   }
   
   
   /* interface methods to be implemented */
   @Override
   public Configuration clone() {
      return new ScoutVMConfig(this.type, this.size, this.vcpus, this.ecus, this.frequency, this.ram,this.ebs_band,this.net_band, this.num_instances);
   }

   @Override
   public int numAttributes() {
      return 8;
   }

   @Override
   /*Size is not used as an attribute. We just use it for identifying the cost  */
   public Object at(int i) {
      switch (i) {
         case 0:
            return vcpus;
         case 1:
            return ecus;
         case 2:
            return frequency;
         case 3:
            return ram;
         case 4:
            return ebs_band;
         case 5:
            return net_band;
         case 6:
            return num_instances;
         case 7:
        	 return type;
         default:
            throw new RuntimeException("Attribute " + i + " not defined for " + this.getClass());
      }
   }
   

   /* other methods */
   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      //NB: I want to consider as equal different objects that refer to the same config, even across classes (i.e., config vs wekaconfig etc)
      //if (o == null || getClass() != o.getClass()) return false;
      ScoutVMConfig that = (ScoutVMConfig) o;
      return Double.compare(that.vcpus, vcpus) == 0 &&
            Double.compare(that.ecus, ecus) == 0 &&
            Double.compare(that.frequency, frequency) == 0 &&
            Double.compare(that.ram, ram) == 0 &&
            Double.compare(that.ebs_band, ebs_band) == 0 &&
            Double.compare(that.num_instances, num_instances) == 0 &&
            type == that.type &&
            size == that.size &&
            Objects.equals(net_band, that.net_band);
   }

   @Override
   public int hashCode() {

      return Objects.hash(type, size, vcpus, ecus, frequency, ram, ebs_band, net_band, num_instances);
   }

   @Override
   public String toString() {
      return "ExtendedScoutVMConfig{" +
            "type=" + type +
            ", size=" + size +
            ", vcpus=" + vcpus +
            ", ecus=" + ecus +
            ", frequency=" + frequency +
            ", ram=" + ram +
            ", ebs_band=" + ebs_band +
            ", net_band='" + net_band + '\'' +
            ", num_instances=" + num_instances +
            '}';
   }

   public static ScoutVMConfig parseName(String name) {
         String[] split = name.split("_");
         int numInstances = Integer.parseInt(split[0]);
         AWSDirectory.AWSInstanceSize size;
         AWSDirectory.AWSInstanceType type;
         String type_size = split[1];
         if (type_size.contains("c4")) {
            type = AWSDirectory.AWSInstanceType.C4;
         } else if (type_size.contains("r4")) {
            type = AWSDirectory.AWSInstanceType.R4;
         } else if (type_size.contains("m4")) {
            type = AWSDirectory.AWSInstanceType.M4;
         } else {
            throw new RuntimeException(type_size + " has unrecognized type");
         }

         //note that "2xlarge" and "xlarge" contain "large" so we have  to check large as last
         //Note that the AWS name is "2xlarge" and not "x2large" as in our enum
         if (type_size.contains("2xlarge")) {
            size = AWSDirectory.AWSInstanceSize.x2large;
         } else if (type_size.contains("xlarge")) {
            size = AWSDirectory.AWSInstanceSize.xlarge;
         } else if (type_size.contains("large")) {
            size = AWSDirectory.AWSInstanceSize.large;
         } else {
            throw new RuntimeException(type_size + " has unrecognized size");
         }

         double ram = ramFor(size, type);
         double cpus = cpusFor(size, type);
         double ecus = ecusFor(size, type);
         String net = networkBandwidthFor(size, type);
         double ebs = ebsBandwidthFor(size, type);
         double hz = cpuFrequencyFor(size, type);

         return new ScoutVMConfig(type, size, cpus, ecus, hz, ram, ebs, net, numInstances);
   }

}
