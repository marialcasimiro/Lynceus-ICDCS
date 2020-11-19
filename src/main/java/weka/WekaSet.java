package weka;

import lynceus.Configuration;
import weka.core.Instance;
import weka.core.Instances;

/**
 * @author Diego Didona
 * @email diego.didona@epfl.ch
 * @since 12.03.18
 */
public interface WekaSet<C extends Configuration> {
   Instances instances();
   Instance getInstance(C c);

}
