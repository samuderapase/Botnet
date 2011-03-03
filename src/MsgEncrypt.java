import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.Security;

import javax.crypto.*;

/**
 * Class MsgEncrypt allows the user to encrypt messages and to decrypt
 * messages that were encrypted by this MsgEncrypt object or a similar
 * one that has the same public and private keys. 
 *
 */
public class MsgEncrypt {

	/** Holds the cipher for this object **/
	private Cipher cipher;
	/** Holds the public key **/
	private Key publicKey;
	/** Holds the private key **/
	private Key privateKey;
	
	/** master key for the master bot **/
	private static Key masterPublicKey;
	
	/**
	 * Private constructor that creates a new MsgEncrypt object
	 * 
	 * @param key != null
	 * @param secretKey != null
	 * @param masterPublicKey != null
	 * @throws Exception if encryption fails
	 */
	private MsgEncrypt(Key publicKey, Key privateKey, Key masterPublicKey) throws Exception {
		this.publicKey = publicKey;
		this.privateKey = privateKey;
		if (MsgEncrypt.masterPublicKey == null) 
			MsgEncrypt.masterPublicKey = masterPublicKey;
		else if (!MsgEncrypt.masterPublicKey.equals(masterPublicKey))
			throw new IllegalArgumentException("Compromised");
		cipher = Cipher.getInstance("RSA");
	}
	
	/**
	 * Creates an instance of a MsgEncrypt object and returns that object
	 * 
	 * @param key != null
	 * @return a new instance of a MsgEncrypt object
	 * @throws Exception if encryption fails
	 */
	public static MsgEncrypt getInstance(Key key) throws Exception {
		KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
		KeyPair kp = kpg.generateKeyPair();
		Key publicKey = kp.getPublic();
		Key privateKey = kp.getPrivate();
		return new MsgEncrypt(publicKey, privateKey, key);
	}
	
	/**
	 * @return the public key for this MsgEncrypt object
	 */
	public Key getPublicKey() {
		return publicKey;
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
	public SealedObject encryptMsg(String msg, Key publicKeyOther) throws Exception {
		cipher.init(Cipher.ENCRYPT_MODE, publicKeyOther);
		return new SealedObject(msg, cipher);
	}
	
	/**
	 * Decrypts the given SealedObject and returns the decrypted message
	 * 
	 * @param msg != null
	 * @return the decrypted message
	 * @throws Exception if the decryption fails
	 */
	public String decryptMsg(SealedObject so) throws Exception {
		cipher.init(Cipher.DECRYPT_MODE, this.privateKey);
		return (String)so.getObject(cipher);
	}
	
	/**
	 * Used for testing the object
	 * 
	 * @param args
	 * @throws NoSuchAlgorithmException
	 */
	public static void main(String[] args) throws Exception {
		KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
		KeyPair kp = kpg.generateKeyPair();
		Key publicKey = kp.getPublic();
		
		KeyPairGenerator bad_kpg = KeyPairGenerator.getInstance("RSA");
		KeyPair bad_kp = bad_kpg.generateKeyPair();
		Key bad_publicKey = bad_kp.getPublic();
		
		MsgEncrypt msgE = MsgEncrypt.getInstance(publicKey);
		String msg = "kill all humans";

		SealedObject ciphertext = msgE.encryptMsg(msg, msgE.getPublicKey());
		String plaintext = msgE.decryptMsg(ciphertext);
		System.out.println("msg = " + msg);
		System.out.println("ciphertext = " + ciphertext);
		System.out.println("plaintext = " + plaintext);
		
		try {
			MsgEncrypt m = MsgEncrypt.getInstance(bad_publicKey);
		} catch (IllegalArgumentException e) {
			System.out.println("Success");
		}
	}

}
