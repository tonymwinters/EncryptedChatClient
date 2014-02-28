import java.awt.Dimension;
import java.awt.Toolkit;

import javax.swing.JFrame;

public class ChatRoom {
	
	
	public static void main(String[] args) throws Exception {

		
		MainGUI sGUI = new MainGUI();
		sGUI.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		sGUI.setSize(700, 400);
		sGUI.setVisible(true);
		
		MainGUI cGUI = new MainGUI();
		cGUI.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		cGUI.setSize(700, 400);
		cGUI.setVisible(true);

		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		 
		int w = sGUI.getSize().width;
		int h = sGUI.getSize().height;
		int x = ((dim.width-w)/2);
		int y = (dim.height-h)/2;
	
		sGUI.setLocation(x, y);

		
	}
	

}