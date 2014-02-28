import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;


public class ClientRead implements Runnable{
	
	private ChatMaster master;
	private SecretKey sessionKey;
	private IvParameterSpec ivSpec;
	private Socket socket;
	
	public ClientRead(SecretKey clientSessionKey,
			IvParameterSpec ivSpec, ChatMaster master, Socket socket) {
		this.ivSpec = ivSpec;
		this.sessionKey = clientSessionKey;
		this.master = master;
		this.socket = socket;
	}
	
	public void run() {
		try {

			DataOutputStream out = new DataOutputStream(
					socket.getOutputStream());
			DataInputStream in = new DataInputStream(socket.getInputStream());

			Cipher readIn = Cipher.getInstance("DESede/CFB8/NoPadding");
			readIn.init(Cipher.DECRYPT_MODE, sessionKey, ivSpec);

			CipherInputStream cis = new CipherInputStream(in, readIn);
			System.out.println("Client Read Thread is Running");
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
			System.out.println("Counldn't Start Client Read Thread");
			e.printStackTrace();
		}
	}

}
