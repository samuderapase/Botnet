import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

public class Tuple {
	
	public KeyGenerator key;
	public SecretKey secretKey;
	public MsgEncrypt msgE;
	
	public Tuple(KeyGenerator key, SecretKey secretKey, MsgEncrypt msgE) {
		this.key = key;
		this.secretKey = secretKey;
		this.msgE = msgE;
	}
}
