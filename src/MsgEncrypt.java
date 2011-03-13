import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.security.AlgorithmParameterGenerator;
import java.security.AlgorithmParameters;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
 
import javax.crypto.*;
import javax.crypto.spec.DHParameterSpec;
//import sun.misc.BASE64Encoder;
//import sun.misc.BASE64Decoder;

//import org.apache.commons.codec.*;
import org.apache.commons.codec.binary.Base64;

/**
 * Class MsgEncrypt allows the user to encrypt messages and to decrypt
 * messages that were encrypted by this MsgEncrypt object or a similar
 * one that has gone through the handshaking process. 
 *
 * @author Robert Johnson and Roy McElmurry
 */
public class MsgEncrypt { 
	
	/** Holds the cipher for this object **/
	private Cipher cipher;
	/** Holds the public key for this object **/
	private Key pubKey;
	/** Holds the private key for this object **/
	private Key privKey;
	/** Holds the mac for this object **/
	private Mac mac;
	/** Holds the serialized version of the public key **/
	private String strPubKey;
	/** Holds the private key that will be used for encryption and decryption **/
	private Key msgKey;
	
	/**
	 * Creates a new MsgEncrypt object with the given parameters
	 * 
	 * @param g != null, must satisfy DH key exchange
	 * @param p != null, must satisfy DH key exchange
	 * @param l must satisfy DH key exchange
	 */
	private MsgEncrypt(BigInteger g, BigInteger p, int l) {
		KeyPair keyPair = getKeyPair(g, p, l);
		this.pubKey = keyPair.getPublic();
		this.privKey = keyPair.getPrivate();
		ByteArrayOutputStream baos = null;
		ObjectOutputStream oos = null;
		try {
			baos = new ByteArrayOutputStream();
			oos = new ObjectOutputStream(baos);
			oos.writeObject(pubKey);
			byte[] pubKeyBytes = baos.toByteArray();
			this.strPubKey = new Base64().encodeToString(pubKeyBytes);
			oos.close();
			baos.close();
		} catch (Exception e) {
			System.out.println("Not persisted");
			e.printStackTrace();
		}
	}
	
	private MsgEncrypt() {}
	
	/**
	 * Returns a new instance of a MsgEncrypt object
	 * 
	 * @param g != null, must satisfy DH key exchange
	 * @param p != null, must satisfy DH key exchange
	 * @param l must satisfy DH key exchange
	 * @return a new MsgEncrypt object with the given parameters
	 */
	public static MsgEncrypt getInstance(BigInteger g, BigInteger p, int l) {
		return new MsgEncrypt(g, p, l);
	}
	
	/**
	 * Returns a new instance of a MsgEncrypt object that
	 * doesn't have any parameters. A call to setPubParams
	 * must follow a call to this method to set the public
	 * parameters that will be used by this object.
	 * 
	 * @return a new MsgEncrypt object that doesn't have any parameters
	 */
	public static MsgEncrypt getInstance() {
		return new MsgEncrypt();
	}
	
	/**
	 * Sets the public parameters to the ones given in string
	 * parameter
	 * 
	 * @param params must be of the form g p l, where g, p, and l
	 * are the public parameters for DH key exchange
	 */
	public void setPubParams(String params) {
		String[] parts = params.split(" ");
		BigInteger g = new BigInteger(parts[0]);
		BigInteger p = new BigInteger(parts[1]);
		int l = Integer.parseInt(parts[2]);
		KeyPair keyPair = getKeyPair(g, p, l);
		this.pubKey = keyPair.getPublic();
		this.privKey = keyPair.getPrivate();
		ByteArrayOutputStream baos = null;
		ObjectOutputStream oos = null;
		try {
			baos = new ByteArrayOutputStream();
			oos = new ObjectOutputStream(baos);
			oos.writeObject(pubKey);
			byte[] pubKeyBytes = baos.toByteArray();
			this.strPubKey = new Base64().encodeToString(pubKeyBytes);
			oos.close();
			baos.close();
		} catch (Exception e) {
			System.out.println("Not persisted");
			e.printStackTrace();
		}
	}
	
	/**
	 * Gets the public information for a DH key exchange or null if the parameters
	 * could not be made
	 * 
	 * @return a PubInfo object that holds the public information
	 *         for a DH key exchange or null if the parameters could not
	 *         be made
	 */
	public static PubInfo getPubParams() {
		try {
			AlgorithmParameterGenerator paramGen = AlgorithmParameterGenerator.getInstance("DH");
			paramGen.init(1024);
			AlgorithmParameters params = paramGen.generateParameters();
	
			DHParameterSpec dhSpec = (DHParameterSpec)params.getParameterSpec(DHParameterSpec.class);
			
			BigInteger g = dhSpec.getG();
			BigInteger p = dhSpec.getP();
			int l = dhSpec.getL();
			
			return new PubInfo(g, p, l);
		} catch (Exception e) {
			System.out.println("Could not make public parameters");
			return null;
		}
	}
	
	/**
	 * Gets the KeyPair that will be used by this object and that uses
	 * the given parameters for DH key exchange
	 * 
	 * @param g != null
	 * @param p != null
	 * @return the KeyPair that will be used by this object
	 */
	private KeyPair getKeyPair(BigInteger g, BigInteger p, int l) {
		try {
			DHParameterSpec dhSpec = new DHParameterSpec(p, g, l);
			
			KeyPairGenerator keyGen = KeyPairGenerator.getInstance("DH");
			
			keyGen.initialize(dhSpec);
			
			KeyPair keyPair = keyGen.generateKeyPair();
			return keyPair;
		} catch (Exception e) {
			System.out.println("Could not create KeyPair");
			return null;
		}
	}
	
	/**
	 * Returns a serialized string version of the public
	 * key for this object
	 * 
	 * @return a serialized string version of the public key
	 */
	public String getStrKey() {
		return strPubKey;
	}
	
	/**
	 * Performs a handshake for key exchange between two people. This method
	 * must be called by both people to ensure that the agreement has been
	 * reached on both ends.
	 * 
	 * @param otherKey the serialized string version of the public key for
	 *        the other person. Must not be null.
	 */
	public void handShake(String otherKey) {
		System.out.println("Performing handshake...");
		try {
			byte[] otherPubBytes = new Base64().decode(otherKey);
			ByteArrayInputStream bais = new ByteArrayInputStream(otherPubBytes);
			ObjectInputStream ois = new ObjectInputStream(bais);
			
			KeyAgreement keyAgree = KeyAgreement.getInstance("DiffieHellman");
			keyAgree.init(privKey);
			Key otherPub = (Key) ois.readObject();
			keyAgree.doPhase(otherPub, true);
			msgKey = keyAgree.generateSecret("DESede");
			cipher = Cipher.getInstance("DESede");
			mac = Mac.getInstance("HmacSHA512");
			System.out.println("Handshake completed");
		} catch (Exception e) {
			System.out.println("Could not complete handshake...");
			System.out.println("Agreement not confirmed");
			e.printStackTrace();
		}
	}
	
	/**
	 * Encrypts the given msg and returns the ciphertext for the
	 * encryted message
	 * 
	 * @param msg != null
	 * @return the encrypted message or null if encryption fails
	 * @throws Exception if encryption fails
	 */
	public String encryptMsg(String msg) {
		try {
		cipher.init(Cipher.ENCRYPT_MODE, msgKey);
		mac.init(msgKey);
		byte[] c1 = cipher.doFinal(msg.getBytes());
		String c1Str = new Base64().encodeToString(c1);
		byte[] m = mac.doFinal(c1);
		String mStr = new Base64().encodeToString(m);
		return c1Str + "::::" + mStr;
		} catch (Exception e) {
			System.out.println("Could not encrypt the message");
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Decrypts the given String and returns the decrypted message if the
	 * message is verified to come from the correct sender, or null otherwise
	 * 
	 * @param encryptedMsg != null
	 * @return the decrypted message or null if the message is not verified
	 */
	public String decryptMsg(String encryptedMsg) {
		try {
			cipher.init(Cipher.DECRYPT_MODE, msgKey);
			mac.init(msgKey);
			String[] encMsgParts = encryptedMsg.split("::::");
			String encMsg = encMsgParts[0];
			String checkM = encMsgParts[1];
			byte[] encBytes = new Base64().decode(encMsg);
			byte[] message = cipher.doFinal(encBytes);
			byte[] m = mac.doFinal(encBytes);
			String mStr = new Base64().encodeToString(m);
			if (mStr.equals(checkM))
				return new String(message);
			System.out.println("MACs don't match...");
		} catch (Exception e) {
			System.out.println("Could not decrypt");
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Used for testing the object 
	 * 
	 * @param args
	 * @throws NoSuchAlgorithmException
	 */
	public static void main(String[] args) throws Exception {
		PubInfo info = MsgEncrypt.getPubParams();
		MsgEncrypt m1 = MsgEncrypt.getInstance();
		MsgEncrypt m2 = MsgEncrypt.getInstance();
		m1.setPubParams(info.toString());
		m2.setPubParams(info.toString());
		System.out.println(m1.getStrKey());
		m1.handShake(m2.getStrKey());
		m2.handShake(m1.getStrKey());

		System.out.println();
		String msg = "Please work so that crypto will be complete";
		String c = m1.encryptMsg(msg);
		String checkMsg = m2.decryptMsg(c);
		
		System.out.println("Are the original and decrypted msgs the same? " + msg.equals(checkMsg));
		System.out.println();
		System.out.println();
		System.out.println(m1.getStrKey().length());
	}
}
