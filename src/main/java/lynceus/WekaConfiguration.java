package lynceus;

import java.util.ArrayList;

import weka.core.Instance;

/**
 * @author Diego Didona
 * @email diego.didona@epfl.ch
 * @since 11.03.18
 */
public interface WekaConfiguration {
   Instance toInstance();
   
   /** method that generates the neighbourhood for a hill climbing run **/
   ArrayList<WekaConfiguration> neighbourhood();
   
   boolean findPair(ArrayList<Pair> searchSpace, Pair pair);
}
