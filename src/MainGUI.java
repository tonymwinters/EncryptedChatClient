import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.swing.Box;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JButton;

public class MainGUI extends JFrame {

	private JLabel portNumber_label;
	private JLabel hostName_label;
	private JLabel serverPort_label;
	private JLabel title_label;
	
	private JTextField serverPortNumber;
	private JTextField portNumber;
	private JTextField hostName;
	
	private JButton connectServerButton;
	private JButton connectClientButton;
	private JButton sendButton;
	
	private JTextArea chatTextArea;
	private JTextArea inputTextArea;
	private JScrollPane js;
	private JScrollPane js2;
	

	public boolean running = true;

	private Server myServer;
	private Client myClient;

	private ChatMaster master;

	public MainGUI() {

		setLayout(new FlowLayout());
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setTitle("Secure ChatRoom");
		
		serverPortNumber = new JTextField(20);

		portNumber = new JTextField(20);
		hostName = new JTextField(20);

		inputTextArea = new JTextArea();
		chatTextArea = new JTextArea();
		
		connectServerButton = new JButton();
		connectClientButton = new JButton();
		sendButton = new JButton();
		
		portNumber_label = new JLabel("Remote Port:");
		hostName_label = new JLabel("Remote Host:");
		serverPort_label = new JLabel("Server Port");
		title_label = new JLabel("Secure Chat");
		title_label.setFont(new Font("Helvitica", Font.PLAIN, 20));

		inputTextArea.setColumns(46);
		inputTextArea.setLineWrap(true);
		inputTextArea.setRows(4);
		
		js = new JScrollPane(inputTextArea);
		js2 = new JScrollPane(chatTextArea);

		chatTextArea.setColumns(54);
		chatTextArea.setEditable(false);
		chatTextArea.setFont(new java.awt.Font("Helvitica", 0, 13));
		chatTextArea.setLineWrap(true);
		chatTextArea.setRows(10);

		// Buttons and Handlers
		ConnectServer connectsHandler = new ConnectServer();
		connectServerButton.setText("Start Server");
		connectServerButton.addActionListener(connectsHandler);

		ConnectClient connectcHandler = new ConnectClient();
		connectClientButton.setText("Start Client");
		connectClientButton.addActionListener(connectcHandler);

		SendHandler sendHandler = new SendHandler();
		EnterPressed pressed = new EnterPressed();
		sendButton.setText("Send");
		sendButton.addActionListener(sendHandler);
		sendButton.addKeyListener(pressed);

		master = new ChatMaster(this, myServer, myClient);

		// Add to Panel
		add(title_label);
		add(Box.createRigidArea(new Dimension(200,0)));
		add(serverPort_label);
		add(serverPortNumber);
		add(Box.createRigidArea(new Dimension(304,0)));
		add(portNumber_label);
		add(portNumber);
		add(Box.createRigidArea(new Dimension(302,0)));
		add(hostName_label);
		add(hostName);
		add(js2);
		add(js);
		add(Box.createRigidArea(new Dimension(10,0)));
		add(sendButton);

		add(connectServerButton);
		add(connectClientButton);

	}

	public int getPortNumber() {
		return Integer.parseInt(portNumber.getText());
	}

	public String getHostName() {
		return hostName.getText();
	}
	
	public Socket getSocket(){
		return myServer.getSocket();
	}

	public void refresh(String message) {
		chatTextArea.append(message);
	}

	public int getServerPortNumber() {
		return Integer.parseInt(serverPortNumber.getText());
	}

	// Send Button Handler
	private class SendHandler implements ActionListener {
		public void actionPerformed(ActionEvent event) {
			String writtenText = inputTextArea.getText();
			master.send("You Said: " + writtenText + "\n");
			try {
				master.serverSend("User Says: " + writtenText + "\n");
			} catch (Exception e) {
				e.printStackTrace();
			}

			inputTextArea.setText("");
		}
	}

	// Connect Server Button Handler
	private class ConnectServer implements ActionListener {

		public void actionPerformed(ActionEvent event) {

			int findPort = getServerPortNumber();
			master.startServer(findPort);
			chatTextArea.append("Listening to Port Number: "
					+ getServerPortNumber() + "\n");

		} // end actionPerformed

	}

	// Connect Client Button Handler
	private class ConnectClient implements ActionListener {

		public void actionPerformed(ActionEvent event) {
			String iString = getHostName();
			int iInt = getPortNumber();
			try {
				master.startClient(iString, iInt);
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			chatTextArea.append("Connected to: " + iString + " Port: " + iInt
					+ "\n");

		} // end actionPerformed

	} // end connectButtonHandler
	
	private class EnterPressed implements KeyListener{

		public void keyPressed(KeyEvent key) {
			int code = key.getKeyCode();
			if(code == KeyEvent.VK_ENTER){
				String writtenText = inputTextArea.getText();
				master.send("You Said: " + writtenText + "\n");
				try {
					master.serverSend("User Says: " + writtenText + "\n");
				} catch (Exception e) {
					e.printStackTrace();
				}

				inputTextArea.setText("");
			}
			
		}

		@Override
		public void keyReleased(KeyEvent arg0) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void keyTyped(KeyEvent arg0) {
			
		}
		
	}

}
