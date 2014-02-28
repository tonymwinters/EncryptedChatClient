// Tony Winters
// Assignment 6

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyAgreement;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESedeKeySpec;
import javax.crypto.spec.DHParameterSpec;
import javax.crypto.spec.IvParameterSpec;


public class Client implements Runnable {

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

	private Socket socket;
	private String hostName;
	private int portNumber;
	private boolean connected = false;
	private CipherOutputStream cos;
	private CipherInputStream cis;

	private ChatMaster master;

	private byte[] sendToServer;
	private Cipher fileCipher;
	private SecretKey sessionKey;
	private ClientRead readBack;

	private DataInputStream in;
	private DataOutputStream out;
	private IvParameterSpec ivSpec;

	private PublicKey serverPublicKey;
	private PublicKey pubKey;
	private PrivateKey privKey;

	public boolean getConnectionState() {
		if (connected == true) {
			return true;
		} else
			return false;
	}

	public Socket getSocket() {
		return socket;
	}

	public int getPortNumber() {
		return portNumber;
	}

	// Constructor for Client
	public Client(String hostNameIn, int portNumberIn, ChatMaster master)
			throws UnknownHostException, IOException {
		hostName = hostNameIn;
		portNumber = portNumberIn;
		this.master = master;

		try {
			// Create Key pair
			KeyPairGenerator keyGen;
			keyGen = KeyPairGenerator.getInstance("DH");
			keyGen.initialize(PARAMETER_SPEC);
			KeyPair aPair = keyGen.generateKeyPair();

			// Get Public and Private Keys
			pubKey = aPair.getPublic();
			privKey = aPair.getPrivate();

		} catch (Exception err) {
			System.out.println("Keys Weren't Generated");
			err.printStackTrace();
		}

	}

	public void run() {

		try {

			socket = new Socket(hostName, portNumber);
			connected = true;
			System.out.println("Connected To Server!");

			in = new DataInputStream(socket.getInputStream());
			out = new DataOutputStream(socket.getOutputStream());

			byte[] serversKeyBytes = new byte[in.readInt()];
			in.readFully(serversKeyBytes);

			X509EncodedKeySpec keySpec = new X509EncodedKeySpec(serversKeyBytes);
			KeyFactory myFactory = KeyFactory.getInstance("DH");
			serverPublicKey = myFactory.generatePublic(keySpec);

			out.writeInt(pubKey.getEncoded().length);
			out.write(pubKey.getEncoded());

			KeyAgreement bKeyAgree = KeyAgreement.getInstance("DH");
			bKeyAgree.init(privKey);
			bKeyAgree.doPhase(serverPublicKey, true);

			byte[] sessionKeyBytes = bKeyAgree.generateSecret();

			SecretKeyFactory skf = SecretKeyFactory.getInstance("DESede");
			DESedeKeySpec DESedeSpecA = new DESedeKeySpec(sessionKeyBytes);
			sessionKey = skf.generateSecret(DESedeSpecA);

			System.out.println("Client secrtet key: "
					+ toHex(sessionKey.getEncoded()));

			byte[] ivBytes = new byte[8];
			in.read(ivBytes);
			ivSpec = new IvParameterSpec(ivBytes);
			
			readBack = new ClientRead(sessionKey, ivSpec,
					master, socket);
			Thread thread = new Thread(readBack);
			thread.start();

			System.out.println("Everything was initialized!");

			// Create Encrypt Cipher Writing to Server
			fileCipher = Cipher.getInstance("DESede/CFB8/NoPadding");
			fileCipher.init(Cipher.ENCRYPT_MODE, sessionKey, ivSpec);
			cos = new CipherOutputStream(out, fileCipher);

			System.out.println("Ciphers were created");

			cos.write(toByteArray("Connection Made!\n"));
			
			

		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Something wrong with client thread");
		}
	}

	public void sendText(String text) throws IOException {

		sendToServer = toByteArray(text + "\n");
		cos.write(sendToServer);
	}

	public static String toString(byte[] bytes, int length) {
		char[] chars = new char[length];

		for (int i = 0; i != chars.length; i++) {
			chars[i] = (char) (bytes[i] & 0xff);
		}

		return new String(chars);
	}

	public static String toString(byte[] bytes) {
		return toString(bytes, bytes.length);
	}


	public static byte[] toByteArray(String string) {
		byte[] bytes = new byte[string.length()];
		char[] chars = string.toCharArray();

		for (int i = 0; i != chars.length; i++) {
			bytes[i] = (byte) chars[i];
		}

		return bytes;
	}

	private static String digits = "0123456789abcdef";

	public static String toHex(byte[] data, int length) {
		StringBuffer buf = new StringBuffer();

		for (int i = 0; i != length; i++) {
			int v = data[i] & 0xff;

			buf.append(digits.charAt(v >> 4));
			buf.append(digits.charAt(v & 0xf));
		}

		return buf.toString();
	}

	public static String toHex(byte[] data) {
		return toHex(data, data.length);
	}
}
