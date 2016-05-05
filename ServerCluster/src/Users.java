import java.awt.Font;
import java.awt.List;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

class User extends JFrame implements Runnable {
	public void run() {
		boolean hard = false;
		for (int i = 0;; i++, hard = !hard) {
			try {
				Socket sock = new Socket("localhost", 2201);
				DataOutputStream out = new DataOutputStream(
						sock.getOutputStream());
				out.writeInt(10);
				out.writeInt(10);
				out.writeInt(hard ? 300 : 100);
				if (i % 100 == 0) {
					System.out.println("Queries fired = " + i);
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				// sock.close();
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				// e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				// e.printStackTrace();
			}
		}
	}
}

public class Users {
	public static void main(String args[]) {
		UserPool users = new UserPool();
		users.setSize(users.w, users.h);
		users.show();
	}
}

class UserPool extends JFrame implements ChangeListener {
	@Override
	public void stateChanged(ChangeEvent e) {

		if (e.getSource() == uslider) {
			userCount = uslider.getValue();
		} else if (e.getSource() == qslider) {
			qps = qslider.getValue();
		}
	}

	static int w = 300, h = 300;
	JPanel pnl;
	List nodes;
	JButton bStart;
	JLabel usersL = new JLabel("Users : "), userCountL = new JLabel();
	JLabel qpsL = new JLabel("Queries per second : "),
			qpsvL = new JLabel("100");

	JSlider uslider = new JSlider(1, 10);
	JSlider qslider = new JSlider(10, 100);

	int userCount = 2;
	int qps = 10;

	Socket s;
	InputStream in;
	OutputStream out;
	DataInputStream dataIn;
	DataOutputStream dataOut;

	void start() throws UnknownHostException, IOException {
		for (int i = 0; i < userCount; i++) {
			Thread t = new Thread(new User());
			t.start();
		}
		userCountL.setText(new Integer(userCount).toString());
	}

	public UserPool() {
		super("Simulation");

		pnl = new JPanel(null);

		usersL.setBounds(10, 10, 60, 30);
		userCountL.setBounds(70, 10, 100, 30);

		qpsL.setBounds(10, 40, 200, 30);
		qpsvL.setBounds(210, 40, 60, 30);

		pnl.add(usersL);
		pnl.add(userCountL);

		pnl.add(qpsL);
		pnl.add(qpsvL);

		bStart = new JButton("Start");
		bStart.setBounds(15 + 2 * (w - 30) / 4 - 40, h - 50, 100, 25);
		bStart.addMouseListener(new MouseListener() {

			@Override
			public void mouseReleased(MouseEvent e) {
				// TODO Auto-generated method stub

			}

			@Override
			public void mousePressed(MouseEvent e) {
				// TODO Auto-generated method stub

			}

			@Override
			public void mouseExited(MouseEvent e) {
				// TODO Auto-generated method stub

			}

			@Override
			public void mouseEntered(MouseEvent e) {
				// TODO Auto-generated method stub

			}

			@Override
			public void mouseClicked(MouseEvent e) {
				// TODO Auto-generated method stub
				try {
					start();
				} catch (UnknownHostException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		});
		pnl.add(bStart);

		uslider.addChangeListener(this);

		getContentPane().add(pnl);

	}
}
