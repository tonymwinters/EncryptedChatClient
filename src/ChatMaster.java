import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

public class ChatMaster implements ActionListener {

	private MainGUI sGUI;
	private Server server;
	private Client client;
	int machineID;

	public ChatMaster(MainGUI sGUI, Server server, Client client) {

		this.sGUI = sGUI;
		this.server = server;
		this.client = client;

	}

	public void send(String message) {
		try {
			sGUI.refresh(message);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public Socket getSocket(){
		return server.getSocket();
	}

	public void serverSend(String text) throws Exception {
		if(machineID == 0){
			server.sendText(text);
		}else{
			client.sendText(text);
		}
		
	}

	public void startServer(int portNumber) {
		server = new Server(portNumber, this);

		Thread serverThread = new Thread(server);
		serverThread.start();
		machineID = 0;
		System.out.println("Server Thread is RUNNING");
	}

	public void startClient(String hostName, int portNumber)
			throws UnknownHostException, IOException {
		client = new Client(hostName, portNumber, this);
		Thread clientThread = new Thread(client);
		clientThread.start();
		machineID = 1;
		System.out.println("Client Thread RUNNING");
	}

	public void actionPerformed(ActionEvent event) {

	}

}
