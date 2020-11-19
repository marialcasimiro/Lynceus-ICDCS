package lynceus;

import java.util.ArrayList;

/**
 * @author Diego Didona
 * @email diego.didona@epfl.ch
 * @since 11.03.18
 */
public interface Configuration extends java.io.Serializable {
   int numAttributes();

   Object at(int i);

   Configuration clone();
    
}
