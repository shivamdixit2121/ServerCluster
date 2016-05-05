import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingQueue;

public class Sender implements Runnable {
	LinkedBlockingQueue<MsgOut> moq;
	Thread t;

	public Sender() {
		moq = new LinkedBlockingQueue<>(1000);
	}

	void start() {
		t = new Thread(this);
		t.start();
	}

	public LinkedBlockingQueue<MsgOut> getMoq() {
		return moq;
	}

	@Override
	public void run() {
		try {
			MsgOut msgOut;
			ByteBuffer buffer = ByteBuffer.allocate(100000);
			while (true) {
				msgOut = moq.take();
				buffer.clear();
				switch (msgOut.cm.type) {
				case CM.LB:
				case CM.MP:
				case CM.WORKER: {
					CM cm = msgOut.cm;
					System.out.println("Sending : to" + cm.type);
					buffer.putInt(msgOut.toByteArray().length);
					// System.out.println("Sending : " +
					// msgOut.toByteArray().length);
					buffer.put(msgOut.toByteArray());
					buffer.limit(buffer.position());
					buffer.rewind();
					int bytesSent = 0, totalBytes = buffer.limit();
					try {
						while (bytesSent != totalBytes)
							bytesSent += cm.socketChannel.write(buffer);
					} catch (IOException e) {
					}
				}
					break;
				case CM.CLIENT_BINARY:
				case CM.CLIENT_TEXT: {
					CM cm = msgOut.cm;
					buffer.put(msgOut.toByteArray());
					buffer.limit(buffer.position());
					buffer.rewind();
					int bytesSent = 0, totalBytes = buffer.limit();
					try {
						while (bytesSent != totalBytes)
							bytesSent += cm.socketChannel.write(buffer);
					} catch (IOException e) {
					}
				}
					break;
				// default: {
				// MsgOutTCPNormal msgOutTN = (MsgOutTCPNormal) msgOut;
				// CM cm = msgOutTN.cm;
				// buffer.put(msgOut.toByteArray());
				// buffer.limit(buffer.position());
				// buffer.rewind();
				// int bytesSent = 0, totalBytes = buffer.limit();
				// try {
				// while (bytesSent != totalBytes)
				// bytesSent += cm.socketChannel.write(buffer);
				// } catch (IOException e) {
				// }
				// cm.socketChannel.close();
				// }
				// break;
				}
				if (msgOut.closeConnection) {
					try {
						msgOut.cm.socketChannel.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}