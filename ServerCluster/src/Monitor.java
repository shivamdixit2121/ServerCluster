import java.awt.List;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Random;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

class NodeStatus {
	int lbTotalServed;
	int workerLoad;
	int workerTotalServed;
}

class MonitorUI extends JFrame implements Runnable {
	JPanel pnl = new JPanel();
	JButton start = new JButton("Monitor");
	List lbTotalList = new List(), workerTotalList = new List(),
			workerLoadList = new List();
	int lbTotal, workerTotal, workerLoad;

	boolean graphConnected = false;
	DataOutputStream dataOutToGraph;

	public MonitorUI() {
		// TODO Auto-generated constructor stub
		MonitorUI t = this;

		start.setBounds(10, 400, 60, 30);
		start.addMouseListener(new MouseListener() {

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
				new Thread(t).start();
			}
		});

		lbTotalList.setBounds(10, 30, 300, 30);
		workerTotalList.setBounds(320, 30, 300, 30);
		workerLoadList.setBounds(630, 30, 300, 30);

		pnl.add(start);
		pnl.add(lbTotalList);
		pnl.add(workerTotalList);
		pnl.add(workerLoadList);
		getContentPane().add(pnl);

	}

	public void run() {
//		new Thread(new Runnable() {
//
//			@Override
//			public void run() {
//				try {
//					dataOutToGraph.writeInt(1);
//					Random rand = new Random();
//					int i = 0;
//					boolean increase = true;
//					while (true) {
//						// dataOutToGraph.writeInt(rand.nextInt(100));
//						dataOutToGraph.writeInt(i);
//						if (increase)
//							i += 30;
//						else
//							i -= 30;
//						if (i > 900 && increase)
//							increase = false;
//						if (i < 100 && !increase)
//							increase = true;
//						Thread.sleep(400);
//					}
//				} catch (Exception e) {
//
//				}
//
//			}
//		}).start();
		Socket s;
		InputStream in;
		OutputStream out;
		DataInputStream dataIn;
		DataOutputStream dataOut;
		while (true) {
			try {
				lbTotalList.removeAll();
				workerTotalList.removeAll();
				workerLoadList.removeAll();

				s = new Socket("localhost", 2101);
				in = s.getInputStream();
				out = s.getOutputStream();

				dataIn = new DataInputStream(in);
				dataOut = new DataOutputStream(out);

				dataOut.writeInt(Msg.MN_SERVER_STATUS);
				Vector<Integer> vals;
				int ni = 1;
				vals = new Vector<>();
				while (true) {
					try {
						lbTotal = dataIn.readInt();
						workerTotal = dataIn.readInt();
						workerLoad = dataIn.readInt();
						System.out.println("Node " + ni + " : lbTotal"
								+ lbTotal + " workerTotal : " + workerTotal
								+ " workerLoad : " + workerLoad);
						lbTotalList.add("Node " + ni + " : " + lbTotal);
						workerTotalList.add("Node " + ni + " : " + workerTotal);
						workerLoadList.add("Node " + ni + " : " + workerLoad);

						vals.add(workerLoad);
						ni++;
						if (s.isInputShutdown()) {
							s.close();
							break;
						}
					} catch (Exception e) {
						break;
					}
				}
				System.out.println("");
				if (!graphConnected) {
					dataOutToGraph.writeInt(ni - 1);
					graphConnected = true;
				}
				for (Integer val : vals) {
					dataOutToGraph.writeInt(val);
				}
			} catch (Exception e) {
			} finally {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
}

public class Monitor {

	public static void main(String[] args) throws Exception {
		MonitorUI mui = new MonitorUI();
		mui.setSize(800, 400);
		mui.show();
		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					Graph.main(null);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}).start();
		Thread.sleep(1000);
		System.out.println("Conn1");
		Socket s = new Socket("localhost", 34000);
		if (s != null)
			System.out.println("Conn2");
		mui.dataOutToGraph = new DataOutputStream(s.getOutputStream());
		try {
			Thread.sleep(1000 * 10000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
