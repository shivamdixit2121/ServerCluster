import java.awt.Frame;
import java.awt.Graphics;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;

class GM extends Frame implements Runnable {
	int xm = 100, ym = 300, sd = 0;
	int fxm = 100, fym = 300;
	double zm = 1;

	int steps = 20;

	Vector<int[]> valsVector = new Vector<>();
	int head = 1, tail = 0;

	void setVmax(int v) {
		vmax = 100 * ((v / 100) + 1);
		vZoom = ymax / vmax;
	}

	double xmax = 400, ymax = 400;
	double xdivs = 10, ydivs = 10;
	double xdivWidth = xmax / xdivs, ydivWidth = ymax / ydivs;

	double vmax = ymax;
	double vZoom = 1;
	double vwidth = vmax / vZoom;

	int l = 0;

	double stepWidth = xmax / steps;

	double xpad = 100, ypad = 100;

	int packs = 0;

	boolean connected = false;

	GM() {
		addWindowListener(new mwad());
		setVmax(100);
	}

	void start() {
		new Thread(this).start();
	}

	DataInputStream dataIn;

	public void run() {
		setVmax(100);
		repaint();
		int n;
		try {
			n = dataIn.readInt();
			System.out.println("N : " + n);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return;
		}
		valsVector = new Vector<>();
		for (int i = 0; i < n; i++)
			valsVector.add(new int[steps+1]);
		while (true) {
			try {
				for (int i = 0; i < n; i++) {
					int load = dataIn.readInt();
					valsVector.get(i)[nextI(head)] = load;
					System.out.println("Read : " + load);
					l = load;
				}
				head = nextI(head);
				if (head == tail)
					tail = nextI(tail);
				repaint();

			} catch (IOException e) {
				// e.printStackTrace();
			}

		}
	}

	int nextI(int i) {
		return (i + 1) % (steps+1);
	}

	int drawLine(double x1, double y1, double x2, double y2, Graphics g) {
		g.drawLine((int) (xpad + xmax - x1), (int) (ypad + ymax - y1),
				(int) (xpad + xmax - x2), (int) (ypad + ymax - y2));
		return 0;
	}

	int drawString(double x1, double y1, String s, Graphics g) {
		g.drawString(s, (int) (xpad + xmax - x1), (int) (ypad + ymax - y1));
		return 0;
	}

	public void paint(Graphics g) {
		if (connected)
			g.drawString("Connected " + l, 50, 50);
		for (int i = 0; i <= ydivs; i++) {
			drawLine(0, i * ydivWidth, xmax, i * ydivWidth, g);
			drawString(-5, i * ydivWidth, (int) ((i * ydivWidth) / vZoom) + "",
					g);
		}
		for (int i = 0; i <= xdivs; i++) {
			drawLine(i * xdivWidth, 0, i * xdivWidth, ymax, g);
			drawString(i * xdivWidth, -15, (int) (i * xdivWidth / stepWidth)
					+ "", g);
		}
		int max = 0;
		for (int[] vals : valsVector) {
			for (int i : vals) {
				max = i > max ? i : max;
			}
		}
		setVmax(max);
		for (int[] vals : valsVector) {
			int points = 0;
			for (int i = tail; i != head; i = nextI(i)) {
				points++;
			}
			for (int i = tail, t = points; i != head; i = nextI(i), t--) {
				drawLine(stepWidth * t, vals[i] * vZoom, stepWidth * (t - 1),
						vals[nextI(i)] * vZoom, g);
			}
		}
	}
	// public void draw() {
	// String strarr, base = "E:\\eclipse IDE\\workspace_2\\prj_x";
	// if (data == 'h') {
	// strarr = base + "\\hour.sdata";
	// h = 30 * 24 - 1;
	// } else if (data == 't') {
	// {
	// strarr = base + "\\tick.sdata";
	// h = 30 * 24 * 3600 - 1;
	// }
	// try {
	// fis[0] = new FileInputStream(strarr);
	// } catch (FileNotFoundException e) {
	// System.out.print(e);
	// }
	// repaint();
	// }
	// }
}

class mwad extends WindowAdapter {
	public void windowClosing(WindowEvent we) {
		System.exit(0);
	}
}

class Graph {
	public static void main(String args[]) throws Exception {
		GM gm = new GM();
		gm.setSize(600, 600);
		gm.setVisible(true);

		ServerSocket ss = new ServerSocket(34000);
		Socket s = ss.accept();
		System.out.println("new source");
		gm.dataIn = new DataInputStream(s.getInputStream());
		gm.connected = true;
		gm.start();

	}
}