import java.math.BigInteger;

/**
	 * Class that holds the public information for a DH key exchange
	 * 
	 * @author Robert Johnson and Roy McElmurry
	 * 
	 */
	public class PubInfo {

		/** The base g in DH **/
		public BigInteger g;
		/** The prime in DH **/
		public BigInteger p;
		public int l;
		
		/** Creates a new PubInfo object with the given params **/
		public PubInfo(BigInteger g, BigInteger p, int l) {
			this.g = g;
			this.p = p;
			this.l = l;
		}
		
		/**
		 * Returns a string representation of this PubInfo object.
		 * 
		 * Ex: g p l
		 * 
		 * @return a string representation of this object
		 */
		public String toString() {
			return this.g + " " + this.p + " " + this.l;
		}
	}