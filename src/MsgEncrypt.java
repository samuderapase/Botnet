import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.security.AlgorithmParameterGenerator;
import java.security.AlgorithmParameters;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Arrays;
import java.util.Random;
import javax.crypto.*;
import javax.crypto.spec.DHParameterSpec;
import org.apache.commons.codec.binary.Base64;

/**
 * Class MsgEncrypt allows the user to encrypt messages and to decrypt
 * messages that were encrypted by this MsgEncrypt object or a similar
 * one that has gone through the handshaking process. 
 *
 * @author Robert Johnson and Roy McElmurry
 */
public class MsgEncrypt { 
	private static final boolean DEBUG = true;
	
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
	/** Holds the private RSA key of this object **/
	private PrivateKey privRSAKey;
	/** Holds the public RSA key of this object **/
	private PublicKey pubRSAKey;
	
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
			if (DEBUG) {
				System.out.println("Not persisted");
			}
			e.printStackTrace();
		}
	}
	
	public Key getPub() {
		return pubKey;
	}
	
	public Key getPriv() {
		return privKey;
	}
	
	public Key getRSAPub() {
		return pubRSAKey;
	}
	
	public Key getRSAPriv() {
		return privRSAKey;
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
			if (DEBUG) {
				System.out.println("Not persisted");
			}
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
			if (DEBUG) {
				System.out.println("Could not make public parameters");
			}
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
			if (DEBUG) {
				System.out.println("Could not create KeyPair");
			}
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
		if (DEBUG) {
			System.out.println("Performing handshake...");
		}
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
			if (DEBUG) {
				System.out.println("Handshake completed");
			}
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
	 */
	public String encryptMsg(String msg) {
		try {
			cipher.init(Cipher.ENCRYPT_MODE, msgKey);
			mac.init(msgKey);
			byte[] c1 = cipher.doFinal(msg.getBytes());
			String c1Str = new Base64().encodeToString(c1);
			byte[] m = mac.doFinal(c1);
			String mStr = new Base64().encodeToString(m);
			return (c1Str + "::::" + mStr).replace("\r\n", "_").replace("\r", "-").replace("\n", "~");
		} catch (Exception e) {
			if (DEBUG) {
				System.out.println("Could not encrypt the message");
			}
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Encrypts the given msg and returns the ciphertext for the
	 * encryted message
	 * 
	 * @param msg != null
	 * @param nonce must be a nonce given from the proper sendee
	 * @return the encrypted message or null if encryption fails
	 */
	public String encryptMsg(String msg, int nonce) {
		try {
			cipher.init(Cipher.ENCRYPT_MODE, msgKey);
			mac.init(msgKey);
			byte[] c1 = cipher.doFinal(msg.getBytes());
			String c1Str = new Base64().encodeToString(c1);
			byte[] m = mac.doFinal(c1);
			String mStr = new Base64().encodeToString(m);
			return (c1Str + "::::" + mStr + "::::" + nonce).replace("\r\n", "_").replace("\r", "-").replace("\n", "~");
		} catch (Exception e) {
			if (DEBUG) {
				System.out.println("Could not encrypt the message");
			}
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
			encryptedMsg = encryptedMsg.replace("~", "\n").replace("-", "\r").replace("_", "\r\n");
			String[] encMsgParts = encryptedMsg.split("::::");
			String encMsg = encMsgParts[0];
			String checkM = encMsgParts[1];
			byte[] encBytes = new Base64().decode(encMsg);
			byte[] message = cipher.doFinal(encBytes);
			byte[] m = mac.doFinal(encBytes);
			String mStr = new Base64().encodeToString(m);
			if (mStr.equals(checkM))
				return new String(message);
			if (DEBUG) {
				System.out.println("MACs don't match...");
			}
		} catch (Exception e) {
			if (DEBUG) {
				System.out.println("Could not decrypt");
			}
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Decrypts the given String and returns the decrypted message if the
	 * message is verified to come from the correct sender, or null otherwise
	 * 
	 * @param encryptedMsg != null
	 * @param nonce must be the nonce sent to the sender to use in the encryption
	 *        of the message 
	 * @return the decrypted message or null if the message is not verified
	 */
	public String decryptMsg(String encryptedMsg, int nonce) {
		try {
			cipher.init(Cipher.DECRYPT_MODE, msgKey);
			mac.init(msgKey);
			encryptedMsg = encryptedMsg.replace("~", "\n").replace("-", "\r").replace("_", "\r\n");
			String[] encMsgParts = encryptedMsg.split("::::");
			String encMsg = encMsgParts[0];
			String checkM = encMsgParts[1];
			int checkNonce = Integer.parseInt(encMsgParts[2]);
			byte[] encBytes = new Base64().decode(encMsg);
			byte[] message = cipher.doFinal(encBytes);
			byte[] m = mac.doFinal(encBytes);
			String mStr = new Base64().encodeToString(m);
			if (mStr.equals(checkM) && checkNonce == nonce)
				return new String(message);
			if (DEBUG) {
				System.out.println("Verification failed...");
			}
		} catch (Exception e) {
			if (DEBUG) {
				System.out.println("Could not decrypt");
			}
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Constructs an RSA key pair and returns the public key or null if the key
	 * pair could not be generated
	 * 
	 * @return the RSA public key generated from the construction of the key pair
	 *         or null if the key pair could not be generated
	 */
	public Key getRSAPair() {
		try {
			SecureRandom random = new SecureRandom();
		    KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");

		    generator.initialize(1024, random);
		    KeyPair pair = generator.generateKeyPair();
		    PublicKey pubKey = pair.getPublic();
		    PrivateKey privKey = pair.getPrivate();
		    privRSAKey = privKey;
		    pubRSAKey = pubKey;
		    return pubKey;
		} catch (Exception e) {
			System.out.println("Could not get the RSA pair");
			e.printStackTrace();
			return null;
		}
		
	}
	
	/**
	 * Returns the public RSA information in the form "pubMod pubExp"
	 * 
	 * @return a string representation of the RSA public information in the form
	 *         "pubMod pubExp"
	 */
	public String getRSAPubInfo() {
		String pubKeyStr = pubRSAKey.toString();
		String[] parts = pubKeyStr.split("\n");
		String modulus = parts[1].split(": ")[1];
		String exp = parts[2].split(": ")[1];
		return modulus + " " + exp;
	}
	
	/**
	 * Generates the RSA public key from the given information. The information
	 * must be RSA public information and must be of the form "pubMod pubExp"
	 * 
	 * @param info must be RSA public information and of the form "pubMod pubExp"
	 */
	public void genRSAPubKey(String info) {
		try {
			String[] parts = info.split(" ");
			BigInteger mod = new BigInteger(parts[0]);
			BigInteger exp = new BigInteger(parts[1]);
			KeySpec ks = (KeySpec)new RSAPublicKeySpec(mod, exp);
			KeyFactory kf = KeyFactory.getInstance("RSA");
			pubRSAKey = kf.generatePublic(ks);
		} catch (Exception e) {
			System.out.println("Could not generate the RSA public key");
			e.printStackTrace();
		}
	}
	
	/**
	 * Generates the private RSA key from the given information paramter. The
	 * info must be RSA private information and of the form "privMod privExp"
	 * 
	 * @param info must be RSA private information and of the form "privMod privExp"
	 */
	public void genRSAPrivKey(String info) {
		try {
			String[] parts = info.split(" ");
			BigInteger mod = new BigInteger(parts[0]);
			BigInteger exp = new BigInteger(parts[1]);
			KeySpec ks = (KeySpec)new RSAPrivateKeySpec(mod, exp);
			KeyFactory kf = KeyFactory.getInstance("RSA");
			privRSAKey = kf.generatePrivate(ks);
		} catch (Exception e) {
			System.out.println("Could not generate the RSA private key");
			e.printStackTrace();
		}
	}
	
	/**
	 * Encrypts the given message using reverse RSA (encrypts with the private key)
	 * and returns the ciphertext of the message in String form or null if the message
	 * could not be encrypted
	 * 
	 * @param msg != null
	 * @return ciphertext form of the message made from RSA encryption or null if the
	 *         message could not be encrypted
	 */
	public String encryptRSA(String msg) {
		try {
			Cipher cipher = Cipher.getInstance("RSA");
			cipher.init(Cipher.ENCRYPT_MODE, privRSAKey);
			byte[] msgBytes = msg.getBytes();
			String result = "";
			for (int i = 0; i < (int)Math.ceil(msgBytes.length * 1.0 /100); i++) {
				byte[] c = cipher.doFinal(Arrays.copyOfRange(msgBytes, i*100, Math.min((i+1)*100, msgBytes.length)));
				result += new Base64().encodeToString(c);
			}
			return result.replace("\r\n", "_").replace("\r", "~").replace("\n", "::");
		} catch (Exception e) {
			System.out.println("Could not encrypt the message");
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Decrypts the given encrypted message using reverse RSA, meaning that it
	 * uses the public key to decrypt, and returns the decrypte message or null 
	 * if the message could not be decrypted
	 * 
	 * @param encMsg != null and encrypted with either this object or a similar
	 *        object that has the proper decryption parameters
	 * @return the string representation of the message that was decrypted or null
	 *         if the message could not be decrypted
	 */
	public String decryptRSA(String encMsg) {
		try {
			encMsg = encMsg.replace("::", "\n").replace("~", "\r").replace("_", "\r\n");
			Cipher cipher = Cipher.getInstance("RSA");
			cipher.init(Cipher.DECRYPT_MODE, pubRSAKey);
			String result = "";
			for (int i = 0; i < encMsg.length()/178; i++) {
				String subMsg = encMsg.substring(i*178, Math.min((i+1)*178, encMsg.length()));
				
				byte[] msgBytes = cipher.doFinal(new Base64().decode(subMsg));
				result += new String(msgBytes);
			}
			return result;
		} catch (Exception e) {
			System.out.println("Could not decrypt the message");
			e.printStackTrace();
			return null;
		}
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
		//System.out.println(m1.getStrKey());
		m1.handShake(m2.getStrKey());
		m2.handShake(m1.getStrKey());
		//m1.getRSAPair();
		//System.out.println(m1.getRSAPubInfo());
		//m2.genRSAPubKey(m1.getRSAPubInfo());
		
		//System.out.println(m1.privRSAKey);
		
		String msg = "please work you fucking piece of shit";
		
		//System.out.println(m1.getStrKey());
		Random rand = new Random();
		int r = rand.nextInt(1000000000);
		//String c = m1.encryptRSA(m1.getStrKey());
		//System.out.println(c);
		//String checkMsg = m2.decryptRSA(c);
		//System.out.println(checkMsg);
		
		//System.out.println("Are the send string key and decrypted strings equal? " + m1.getStrKey().equals(checkMsg));

		//System.out.println(info);
		//System.out.println();
		//String msg = "Please work so that crypto will be complete";
		//String c = m1.encryptMsg(msg).replace("\r\n", "_");
		//String checkMsg = m2.decryptMsg(c.replace("_", "\r\n"));
		String c = m1.encryptMsg(msg, r);
		String checkMsg = m2.decryptMsg(c, r);
		System.out.println();
		System.out.println(c);
		System.out.println();
		
		System.out.println("Are the original and decrypted msgs the same? " + msg.equals(checkMsg));		
	}
}
