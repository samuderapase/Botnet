import java.security.NoSuchAlgorithmException;
import javax.crypto.*;

/**
 * Class MsgEncrypt allows the user to encrypt messages and to decrypt
 * messages that were encrypted by this MsgEncrypt object or a similar
 * one that has the same public and private keys. 
 *
 */
public class MsgEncrypt {

	private Cipher encryptCipher;
	private Cipher decrpytCipher;
	private KeyGenerator key;
	private SecretKey secretKey;
	
	/**
	 * Private constructor that creates a new MsgEncrypt object
	 * 
	 * @param key != null
	 * @param secretKey != null
	 */
	private MsgEncrypt(KeyGenerator key, SecretKey secretKey) {
		this.key = key;
		this.secretKey = secretKey;
		try {
			encryptCipher = Cipher.getInstance("AES");
			encryptCipher.init(Cipher.ENCRYPT_MODE, secretKey);
			decrpytCipher = Cipher.getInstance("AES");
			decrpytCipher.init(Cipher.DECRYPT_MODE, secretKey);
		} catch (Exception e) {
			e.printStackTrace();
		} 
	}
	
	/**
	 * Returns a new instantiated MsgEncrypt object with the given public and
	 * private keys
	 * 
	 * @param key != null
	 * @param secretKey != null
	 * @return new instantiated MsgEncrypt object with the given public and
	 * private keys
	 */
	public static MsgEncrypt getInstance(KeyGenerator key, SecretKey secretKey) {
		return new MsgEncrypt(key, secretKey);
	}
	
	/**
	 * @return the public key for this MsgEncrypt object
	 */
	public KeyGenerator getPublicKey() {
		return key;
	}
	
	/**
	 * @return the private key for this MsgEncrypt object
	 */
	public SecretKey getSecretKey() {
		return secretKey;
	}
	
	/**
	 * Encrypts the given msg and returns the encrypted message
	 * 
	 * @param msg != null
	 * @return the encrypted message
	 */
	public String encryptMsg(String msg) {
		byte[] bytemsg = stringToByteArray(msg);
		try {
			byte[] cipherbyte = encryptCipher.doFinal(bytemsg);
			return byteArrayToString(cipherbyte);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Decrypts the given msg and returns the decrypted message
	 * 
	 * @param msg != null
	 * @return the decrypted message
	 */
	public String decryptMsg(String msg) {
		byte[] cipherByte = stringToByteArray(msg);
		try {
			byte[] plainbyte = decrpytCipher.doFinal(cipherByte);
			return byteArrayToString(plainbyte);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Converts from a string to a byte array
	 * 
	 * @param msg != null
	 * @return the byte array that corresponds to the given string
	 */
	private byte[] stringToByteArray(String msg) {
		byte[] bytemsg = new byte[msg.length()];
		for (int i = 0; i < msg.length(); i++) {
			bytemsg[i] = (byte)msg.charAt(i);
		}
		return bytemsg;
	}
	
	/**
	 * Converts the given byte array into a string and returns
	 * the string
	 * 
	 * @param cipher != null
	 * @return the String that corresponds to the given byte array
	 */
	private String byteArrayToString(byte[] cipher) {
		char[] msg = new char[cipher.length];
		for (int i = 0; i < cipher.length; i++) {
			msg[i] = (char)cipher[i];
		}
		return new String(msg);
	}
	
	/**
	 * Used for testing the object
	 * 
	 * @param args
	 * @throws NoSuchAlgorithmException
	 */
	public static void main(String[] args) throws NoSuchAlgorithmException {
		KeyGenerator key = KeyGenerator.getInstance("AES");
		MsgEncrypt msgE = MsgEncrypt.getInstance(key, key.generateKey());
		String msg = "kill all humans";
		String ciphertext = msgE.encryptMsg(msg);
		String plaintext = msgE.decryptMsg(ciphertext);
		System.out.println("msg = " + msg);
		System.out.println("ciphertext = " + ciphertext);
		System.out.println("plaintext = " + plaintext);
	}

}
