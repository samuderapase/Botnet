import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import javax.crypto.*;

/**
 * Class MsgEncrypt allows the user to encrypt messages and to decrypt
 * messages that were encrypted by this MsgEncrypt object or a similar
 * one that has the same public and private keys. 
 *
 */
public class MsgEncrypt2 {

	/** Holds the cipher for this object **/
	private Cipher cipher;
	
	private KeyPair kp;
	
	private Key macKey;
	
	private Mac mac;
	
	
	/**
	 * Private constructor that creates a new MsgEncrypt object
	 * 
	 * @param key != null
	 * @param secretKey != null
	 * @param masterPublicKey != null
	 * @throws Exception if encryption fails
	 */
	private MsgEncrypt2(KeyPair kp, Key macKey) throws Exception {
		this.kp = kp;
		cipher = Cipher.getInstance("RSA");
		
		this.macKey = macKey;
		mac = Mac.getInstance("HmacSHA512");
		mac.init(this.macKey);
	}
	
	/**
	 * Creates an instance of a MsgEncrypt object and returns that object
	 * 
	 * @param key != null
	 * @return a new instance of a MsgEncrypt object
	 * @throws Exception if encryption fails
	 */
	public static MsgEncrypt2 getInstance(KeyPair kp, Key macKey) throws Exception {
		return new MsgEncrypt2(kp, macKey);
	}
	
	/**
	 * @return the public key for this MsgEncrypt object
	 */
	public Key getPublicKey() {
		return kp.getPublic();
	}
	
	/**
	 * Encrypts the given msg with the public key and returns the SealedObject
	 * for the encryption of the string
	 * 
	 * @param msg != null
	 * @param publicKeyOther != null
	 * @return the encrypted message in a SealedObject
	 * @throws Exception if encryption fails
	 */
	public /*SealedObject*/Pair<SealedObject, String> encryptMsg(String msg) throws Exception {
		byte[] msgMac = mac.doFinal(msg.getBytes());
		String strMsgMac = new String(msgMac);
		cipher.init(Cipher.ENCRYPT_MODE, this.kp.getPublic());
		SealedObject so = new SealedObject(msg, cipher);
		Pair<SealedObject, String> p = new Pair<SealedObject, String>(so, strMsgMac);
		return p;
		//return new SealedObject(msg, cipher);
	}
	
	/**
	 * Decrypts the given SealedObject and returns the decrypted message
	 * 
	 * @param msg != null
	 * @return the decrypted message
	 * @throws Exception 
	 * @throws Exception if the decryption fails
	 */
	//public String decryptMsg(SealedObject so) throws Exception {
	public String decryptMsg(Pair<SealedObject, String> encryptedMsg) throws Exception {
		cipher.init(Cipher.DECRYPT_MODE, this.kp.getPrivate());
		String msg = (String)encryptedMsg.s.getObject(cipher);
		String strMac = encryptedMsg.t;
		byte[] msgMac = mac.doFinal(msg.getBytes());
		String strMsgMac = new String(msgMac);
		if (strMac.equals(strMsgMac))
			return msg;
		return null;
	}
	
	/**
	 * Used for testing the object
	 * 
	 * @param args
	 * @throws NoSuchAlgorithmException
	 */
	public static void main(String[] args) throws Exception {
		KeyGenerator kg = KeyGenerator.getInstance("HmacSHA512");
		Key macKey1 = kg.generateKey();
		Key macKey2 = kg.generateKey();
		
		KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
		KeyPair kp = kpg.generateKeyPair();
		
		
		Mac mac = Mac.getInstance("HmacSHA512");
		mac.init(macKey2);
		byte[] msgMac = mac.doFinal("Distroy bot".getBytes());
		String strMsgMac = new String(msgMac);
		Cipher cipher = Cipher.getInstance("RSA");
		cipher.init(Cipher.ENCRYPT_MODE, kp.getPublic());
		SealedObject so = new SealedObject("Distroy bot", cipher);
		Pair<SealedObject, String> p = new Pair<SealedObject, String>(so, strMsgMac);
		
		
		MsgEncrypt2 m = MsgEncrypt2.getInstance(kp, macKey1);
		String msg = "This should be encrypted...";
		Pair<SealedObject, String> encryptedMsg = m.encryptMsg(msg);
		String decryptedMsg = m.decryptMsg(p);
		if (decryptedMsg != null)
			System.out.println(decryptedMsg);
		else
			System.out.println("Not fooled");
		
		String correctDecryption = m.decryptMsg(encryptedMsg);
		if (correctDecryption != null)
			System.out.println("Yes!! msg = " + correctDecryption);
		else
			System.out.println("Damn");
	}

}
