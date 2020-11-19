package lynceus.aws;

/**
 * @author Diego Didona
 * @email diego.didona@epfl.ch
 * @since 24.04.18
 */
public interface AWSConfig {
   int numInstances();
   AWSDirectory.AWSInstanceType type();
   AWSDirectory.AWSInstanceSize size();
}
