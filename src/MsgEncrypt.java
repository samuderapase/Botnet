import java.security.AlgorithmParameterGenerator;
import java.security.AlgorithmParameters;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;

import javax.crypto.*;
import javax.crypto.spec.DHParameterSpec;

/**
 * Class MsgEncrypt allows the user to encrypt messages and to decrypt
 * messages that were encrypted by this MsgEncrypt object or a similar
 * one that has the same public and private keys. 
 *
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
	
	
	/**
	 * Private constructor that creates a new MsgEncrypt object
	 * 
	 * @param key != null
	 * @param secretKey != null
	 * @param masterPublicKey != null
	 * @throws Exception if encryption fails
	 */
	private MsgEncrypt(KeyPair kp, Key otherPub) throws Exception {
		cipher = Cipher.getInstance("DESede");

		this.pubKey = kp.getPublic();
		KeyAgreement keyAgree = KeyAgreement.getInstance("DiffieHellman");
		keyAgree.init(kp.getPrivate());
		keyAgree.doPhase(otherPub, true);
		this.privKey = keyAgree.generateSecret("DESede");
		
		mac = Mac.getInstance("HmacSHA512");
	}
	
	/**
	 * Creates an instance of a MsgEncrypt object and returns that object
	 * 
	 * @param key != null
	 * @return a new instance of a MsgEncrypt object
	 * @throws Exception if encryption fails
	 */
	public static MsgEncrypt getInstance(KeyPair kp, Key otherPub) throws Exception {
		return new MsgEncrypt(kp, otherPub);
	}
	
	/**
	 * @return the public key for this MsgEncrypt object
	 */
	public Key getPublicKey() {
		return pubKey;
	}
	
	/**
	 * Encrypts the given msg with the public key and returns the encrypted
	 * String
	 * 
	 * @param msg != null
	 * @return the encrypted message
	 * @throws Exception if encryption fails
	 */
	public String encryptMsg2(String msg) throws Exception {
		cipher.init(Cipher.ENCRYPT_MODE, privKey);
		byte[] c1 = cipher.doFinal(msg.getBytes());
		String c1Str = new sun.misc.BASE64Encoder().encode(c1);
		mac.init(privKey);
		byte[] m = mac.doFinal(c1);
		String mStr = new sun.misc.BASE64Encoder().encode(m);
		String message = "msg::=" + c1Str + "\nmac::=" + mStr;
		byte[] msgBytes = cipher.doFinal(message.getBytes());
		String encMsg = new sun.misc.BASE64Encoder().encode(msgBytes);
		return encMsg;
	}
	
	/**
	 * Decrypts the given String and returns the decrypted message if the
	 * message is verified to come from the correct sender, or null otherwise
	 * 
	 * @param encryptedMsg != null
	 * @return the decrypted message or null if the message is not verified
	 * @throws Exception if the decryption fails
	 */
	public String decryptMsg2(String encryptedMsg) throws Exception {
		cipher.init(Cipher.DECRYPT_MODE, privKey);
		mac.init(privKey);
		byte[] encBytes = new sun.misc.BASE64Decoder().decodeBuffer(encryptedMsg);
		byte[] message = cipher.doFinal(encBytes);
		String parts = new String(message);
		try {
			String encMsg = parts.split("\nmac::=")[0].substring(6);
			String checkMac = parts.split("mac::=")[1];
			byte[] c1 = new sun.misc.BASE64Decoder().decodeBuffer(encMsg);
			mac.init(privKey);
			byte[] m = mac.doFinal(c1);
			byte[] checkM = new sun.misc.BASE64Decoder().decodeBuffer(checkMac);
			if (checkArrEquality(checkM, m)) {
				byte[] c1Bytes = new sun.misc.BASE64Decoder().decodeBuffer(encMsg);
				byte[] msg = cipher.doFinal(c1Bytes);
				return new String(msg);
			}
			return null;
		} catch (Exception e) {
			// only time this should happen is if encryptedMsg was not
			// made from one of the encrypt methods of a MsgEncrypt object
			return null;
		}
	}
	
	/**
	 * Checks if the two arrays are equal to each other. Equality means
	 * that the two arrays are of equal length and m1[i] = m2[i] for all i
	 * 
	 * @param m1 != null
	 * @param m2 != null
	 * @return true iff m1 = m2
	 */
	private boolean checkArrEquality(byte[] m1, byte[] m2) {
		if (m1.length != m2.length)
			return false;
		for (int i = 0; i < m1.length; i++) {
			if (m1[i] != m2[i])
				return false;
		}
		return true;
	}
	
	/**
	 * Used for testing the object
	 * 
	 * @param args
	 * @throws NoSuchAlgorithmException
	 */
	public static void main(String[] args) throws Exception {
		AlgorithmParameterGenerator paramGen = AlgorithmParameterGenerator.getInstance("DH");
		paramGen.init(1024);
		AlgorithmParameters params = paramGen.generateParameters();

		DHParameterSpec dhSpec = (DHParameterSpec)params.getParameterSpec(DHParameterSpec.class);

		
		KeyPairGenerator keyGen = KeyPairGenerator.getInstance("DH");
		
		keyGen.initialize(dhSpec);
		
		KeyPair aKeyPair = keyGen.generateKeyPair();
		KeyPair bKeyPair = keyGen.generateKeyPair();
		
		// This give the public keys...
		Key aPubKey = aKeyPair.getPublic();
		Key bPubKey = bKeyPair.getPublic();
		
		MsgEncrypt msgE = MsgEncrypt.getInstance(aKeyPair, bPubKey);
		MsgEncrypt msgE2 = MsgEncrypt.getInstance(bKeyPair, aPubKey);
		
		/*String msg = "Send out spam messages";
		String c = msgE.encryptMsg2(msg);
		String m = msgE2.decryptMsg2(c);
		System.out.println(msg);
		System.out.println(m);
		
		msg = "";
		for (int i = 0; i < 500; i++) {
			msg += i;
			c = msgE.encryptMsg2(msg);
			m = msgE2.decryptMsg2(c);
			System.out.println(msg);
			System.out.println(m);
		}*/
	}

}
