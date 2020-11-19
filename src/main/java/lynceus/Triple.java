package lynceus;

/**
 * @author Diego Didona
 * @email diego.didona@epfl.ch
 * @since 22.03.18
 */
public class Triple<A, B, C> {
   public final A fst;
   public final B snd;
   public final C trd;

   public Triple(A fst, B snd, C trd) {
      this.fst = fst;
      this.snd = snd;
      this.trd = trd;
   }

	public A getFst() {
		return fst;
	}
	
	public B getSnd() {
		return snd;
	}
	
	public C getTrd() {
		return trd;
	}
   
   
   
}
