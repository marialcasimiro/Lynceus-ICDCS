package lynceus;

/**
 * @author Diego Didona
 * @email diego.didona@epfl.ch
 * @since 15.03.18
 */
public class Pair<O, OO> {
	private O fst;
	private OO snd;

   public Pair(O fst, OO snd) {
      this.fst = fst;
      this.snd = snd;
   }

   
   /* getters */
   public O getFst() {
	  return fst;
   }
	
   public OO getSnd() {
	  return snd;
   }

   /* setters */
   public void setFst(O fst) {
	  this.fst = fst;
   }

   public void setSnd(OO snd) {
	  this.snd = snd;
   }


	@Override
	public String toString() {
		return "Pair [fst=" + fst + ", snd=" + snd + "]";
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((fst == null) ? 0 : fst.hashCode());
		result = prime * result + ((snd == null) ? 0 : snd.hashCode());
		return result;
	}


	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Pair other = (Pair) obj;
		if (fst == null) {
			if (other.fst != null)
				return false;
		} else if (!fst.equals(other.fst))
			return false;
		if (snd == null) {
			if (other.snd != null)
				return false;
		} else if (!snd.equals(other.snd))
			return false;
		return true;
	}
   
   
}
