import java.math.BigInteger;
import java.io.*;
import java.net.*;
import java.security.*;
import java.security.spec.*;
import javax.crypto.*;
import javax.crypto.spec.*;

public class Server implements Runnable {

	private static final byte SKIP_1024_MODULUS_BYTES[] = { (byte) 0xF4,
			(byte) 0x88, (byte) 0xFD, (byte) 0x58, (byte) 0x4E, (byte) 0x49,
			(byte) 0xDB, (byte) 0xCD, (byte) 0x20, (byte) 0xB4, (byte) 0x9D,
			(byte) 0xE4, (byte) 0x91, (byte) 0x07, (byte) 0x36, (byte) 0x6B,
			(byte) 0x33, (byte) 0x6C, (byte) 0x38, (byte) 0x0D, (byte) 0x45,
			(byte) 0x1D, (byte) 0x0F, (byte) 0x7C, (byte) 0x88, (byte) 0xB3,
			(byte) 0x1C, (byte) 0x7C, (byte) 0x5B, (byte) 0x2D, (byte) 0x8E,
			(byte) 0xF6, (byte) 0xF3, (byte) 0xC9, (byte) 0x23, (byte) 0xC0,
			(byte) 0x43, (byte) 0xF0, (byte) 0xA5, (byte) 0x5B, (byte) 0x18,
			(byte) 0x8D, (byte) 0x8E, (byte) 0xBB, (byte) 0x55, (byte) 0x8C,
			(byte) 0xB8, (byte) 0x5D, (byte) 0x38, (byte) 0xD3, (byte) 0x34,
			(byte) 0xFD, (byte) 0x7C, (byte) 0x17, (byte) 0x57, (byte) 0x43,
			(byte) 0xA3, (byte) 0x1D, (byte) 0x18, (byte) 0x6C, (byte) 0xDE,
			(byte) 0x33, (byte) 0x21, (byte) 0x2C, (byte) 0xB5, (byte) 0x2A,
			(byte) 0xFF, (byte) 0x3C, (byte) 0xE1, (byte) 0xB1, (byte) 0x29,
			(byte) 0x40, (byte) 0x18, (byte) 0x11, (byte) 0x8D, (byte) 0x7C,
			(byte) 0x84, (byte) 0xA7, (byte) 0x0A, (byte) 0x72, (byte) 0xD6,
			(byte) 0x86, (byte) 0xC4, (byte) 0x03, (byte) 0x19, (byte) 0xC8,
			(byte) 0x07, (byte) 0x29, (byte) 0x7A, (byte) 0xCA, (byte) 0x95,
			(byte) 0x0C, (byte) 0xD9, (byte) 0x96, (byte) 0x9F, (byte) 0xAB,
			(byte) 0xD0, (byte) 0x0A, (byte) 0x50, (byte) 0x9B, (byte) 0x02,
			(byte) 0x46, (byte) 0xD3, (byte) 0x08, (byte) 0x3D, (byte) 0x66,
			(byte) 0xA4, (byte) 0x5D, (byte) 0x41, (byte) 0x9F, (byte) 0x9C,
			(byte) 0x7C, (byte) 0xBD, (byte) 0x89, (byte) 0x4B, (byte) 0x22,
			(byte) 0x19, (byte) 0x26, (byte) 0xBA, (byte) 0xAB, (byte) 0xA2,
			(byte) 0x5E, (byte) 0xC3, (byte) 0x55, (byte) 0xE9, (byte) 0x2F,
			(byte) 0x78, (byte) 0xC7 };

	private static final BigInteger MODULUS = new BigInteger(1,
			SKIP_1024_MODULUS_BYTES);

	private static final BigInteger BASE = BigInteger.valueOf(2);

	private static final DHParameterSpec PARAMETER_SPEC = new DHParameterSpec(
			MODULUS, BASE);

	private Socket mySocket;
	private ServerSocket ss;
	private boolean connected = false;
	private int portNumber;
	private CipherInputStream cis;
	private IvParameterSpec ivSpec;
	private DataOutputStream out;
	private DataInputStream in;
	private PublicKey pubKey;
	private PrivateKey privKey;
	private SecretKey sessionKey;
	private ServerSend writeBack;
	
	private ChatMaster master;

	public boolean getConnectionState() {
		if (connected == true) {
			return true;
		} else
			return false;
	}

	public Socket getSocket() {
		return mySocket;
	}

	public int getPortNumber() {
		return portNumber;
	}

	public Server(int port, ChatMaster master) {
		portNumber = port;
		this.master = master;
		try {
			KeyPairGenerator keyGen = KeyPairGenerator.getInstance("DH");
			keyGen.initialize(PARAMETER_SPEC);
			KeyPair aPair = keyGen.generateKeyPair();

			pubKey = aPair.getPublic();
			privKey = aPair.getPrivate();

		} catch (Exception d) {
		}
	}

	public String getHostName() {
		return this.getHostName();
	}

	public void run() {
		try {

			ss = new ServerSocket(portNumber);
			mySocket = ss.accept();
			connected = true;

			out = new DataOutputStream(mySocket.getOutputStream());
			in = new DataInputStream(mySocket.getInputStream());

			out.writeInt(pubKey.getEncoded().length);
			out.write(pubKey.getEncoded());

			byte[] clientsKeyBytes = new byte[in.readInt()];
			in.readFully(clientsKeyBytes);

			X509EncodedKeySpec keySpec = new X509EncodedKeySpec(clientsKeyBytes);
			KeyFactory myFactory = KeyFactory.getInstance("DH");
			PublicKey clientPubKey = myFactory.generatePublic(keySpec);

			KeyAgreement aKeyAgree = KeyAgreement.getInstance("DH");
			aKeyAgree.init(privKey);
			aKeyAgree.doPhase(clientPubKey, true);

			byte ivBytes[] = new byte[8];
			SecureRandom irandom = new SecureRandom();
			irandom.nextBytes(ivBytes);

			ivSpec = new IvParameterSpec(ivBytes);

			out.write(ivSpec.getIV());

			// Create Secret for Server
			byte[] sessionKeyBytes = aKeyAgree.generateSecret();

			// Create Session Key
			SecretKeyFactory skf = SecretKeyFactory.getInstance("DESede");
			DESedeKeySpec DESedeSpecA = new DESedeKeySpec(sessionKeyBytes);
			sessionKey = skf.generateSecret(DESedeSpecA);
			System.out.println("Server secrtet key: "
					+ toHex(sessionKey.getEncoded()));
			
			
			writeBack = new ServerSend(sessionKey, ivSpec,
					master, mySocket);
			Thread thread = new Thread(writeBack);
			thread.start();

			Cipher fileCipher = Cipher.getInstance("DESede/CFB8/NoPadding");
			fileCipher.init(Cipher.DECRYPT_MODE, sessionKey, ivSpec);
			cis = new CipherInputStream(in, fileCipher);

			
			int theChar = 0;
			theChar = cis.read();

			while (theChar != -1) {
				master.send(String.valueOf((char) theChar));
				theChar = cis.read();
			}

			in.close();
			out.close();
			cis.close();

		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	
	public void sendText(String text) throws IOException {

		writeBack.sendText(text);
	}

	/**
	 * Convert a byte array of 8 bit characters into a String.
	 * 
	 * @param bytes
	 *            the array containing the characters
	 * @param length
	 *            the number of bytes to process
	 * @return a String representation of bytes
	 */
	public static String toString(byte[] bytes, int length) {
		char[] chars = new char[length];

		for (int i = 0; i != chars.length; i++) {
			chars[i] = (char) (bytes[i] & 0xff);
		}

		return new String(chars);
	}

	/**
	 * Convert a byte array of 8 bit characters into a String.
	 * 
	 * @param bytes
	 *            the array containing the characters
	 * @return a String representation of bytes
	 */
	public static String toString(byte[] bytes) {
		return toString(bytes, bytes.length);
	}

	/**
	 * Convert the passed in String to a byte array by taking the bottom 8 bits
	 * of each character it contains.
	 * 
	 * @param string
	 *            the string to be converted
	 * @return a byte array representation
	 */
	public static byte[] toByteArray(String string) {
		byte[] bytes = new byte[string.length()];
		char[] chars = string.toCharArray();

		for (int i = 0; i != chars.length; i++) {
			bytes[i] = (byte) chars[i];
		}

		return bytes;
	}

	private static String digits = "0123456789abcdef";

	/**
	 * Return length many bytes of the passed in byte array as a hex string.
	 * 
	 * @param data
	 *            the bytes to be converted.
	 * @param length
	 *            the number of bytes in the data block to be converted.
	 * @return a hex representation of length bytes of data.
	 */
	public static String toHex(byte[] data, int length) {
		StringBuffer buf = new StringBuffer();

		for (int i = 0; i != length; i++) {
			int v = data[i] & 0xff;

			buf.append(digits.charAt(v >> 4));
			buf.append(digits.charAt(v & 0xf));
		}

		return buf.toString();
	}

	/**
	 * Return the passed in byte array as a hex string.
	 * 
	 * @param data
	 *            the bytes to be converted.
	 * @return a hex representation of data.
	 */
	public static String toHex(byte[] data) {
		return toHex(data, data.length);
	}

}// end class

