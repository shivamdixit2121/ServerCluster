import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.LinkedBlockingQueue;

public class ServerCluster {

	public static void main(String[] args) throws IOException,
			InterruptedException {

		System.out.println("_");
		int i = Integer.parseInt(new BufferedReader(new InputStreamReader(
				System.in)).readLine());
		System.out.println(i);

		MainProcess mp = new MainProcess(i);
		mp.go();

		// Config c = new Config();
		// System.out.println("Config read result : " + c.readConfig());
		// System.out.println(c.publicAddress);
		//
		// LinkedBlockingQueue<Msg> mq = new LinkedBlockingQueue<>();
		// LinkedBlockingQueue<MsgOut> moq;
		//
		// Receiver rec = new Receiver(c.getMainProcess().getPort(), mq);
		// rec.start();
		//
		// Sender sender = new Sender();
		// moq = sender.getMoq();
		// sender.start();
		//
		// Msg msg;
		//
		// System.out.println("Server running at port "
		// + c.getMainProcess().getPort() + "...");
		// while (true) {
		// msg = mq.take();
		// switch (msg.type) {
		// case Msg.CLIENT: {
		// System.out.println(msg.toString());
		//
		// MsgOutTCPNormal mo = new MsgOutTCPNormal(MsgOut.CLIENT);
		// mo.cm = msg.cm;
		//
		// mo.write("HTTP/1.1 200 OK\nServer: ServerCluster\nContent-Type: text/html;charset=ISO-8859-1\nVary: Accept-Encoding\n\n"
		// .getBytes());
		// FileInputStream fin = new FileInputStream(
		// new File("index.html"));
		// byte b[] = new byte[1000];
		// while (true) {
		// if (fin.read(b) == -1)
		// break;
		// mo.write(b);
		// }
		// fin.close();
		// moq.add(mo);
		// }
		// }
		// }

	}
}
