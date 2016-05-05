import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.security.acl.Owner;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

public class Receiver implements Runnable {

	Selector sel;
	Thread t;
	LinkedBlockingQueue<Msg> mq;
	ReentrantLock l1 = new ReentrantLock(), l2 = new ReentrantLock();

	int lPort;

	Receiver(int lPort, LinkedBlockingQueue<Msg> mq) throws IOException {
		this.lPort = lPort;
		sel = Selector.open();
		this.mq = mq;
	}

	CM register(SocketChannel socketChannel, int type) throws IOException {
		switch (type) {
		case CM.CLIENT_BINARY:
		case CM.CLIENT_TEXT:
		case CM.UNKNOWN:
		case CM.LB:
		case CM.MP:
		case CM.WORKER: {
			CM cm = new CM();
			socketChannel.configureBlocking(false);
			try {
				l1.lock();
				sel.wakeup();
				l2.lock();
				socketChannel.register(sel, SelectionKey.OP_READ, cm);
				l2.unlock();
				l1.unlock();
			} catch (Exception e) {
				return null;
			}
			cm.type = type;
			cm.socketChannel = socketChannel;
			return cm;
		}
		}
		return null;
	}

	void start() {
		t = new Thread(this);
		t.start();
	}

	Thread listingThread;
	boolean i = false;

	// private void startListning() throws IOException {
	// ServerSocketChannel ssc = ServerSocketChannel.open();
	// ssc.bind(new InetSocketAddress(lPort));
	// ssc.configureBlocking(false);
	// CM cm = new CM();
	// cm.type = CM.LISTNING_INTERNAL;
	// cm.serverSocketChannel = ssc;
	// ssc.register(sel, SelectionKey.OP_ACCEPT, cm);
	// }

	void startListning(int port, int type) throws IOException {
		if (type != CM.LISTNING_INTERNAL && type != CM.LISTNING_EXTERNAL_BINARY
				&& type != CM.LISTNING_EXTERNAL_TEXT)
			return;
		ServerSocketChannel ssc = ServerSocketChannel.open();
		ssc.bind(new InetSocketAddress(port));
		ssc.configureBlocking(false);
		CM cm = new CM();
		cm.type = type;
		cm.serverSocketChannel = ssc;
		ssc.register(sel, SelectionKey.OP_ACCEPT, cm);
	}

	public void run() {
		try {
			startListning(lPort, CM.LISTNING_INTERNAL);
			ByteBuffer buffer = ByteBuffer.allocate(100000);
			while (true) {
				try {
					l1.lock();
					l2.lock();
					l1.unlock();
					sel.select();
					Set<SelectionKey> keys = sel.selectedKeys();
					l2.unlock();
					Iterator<SelectionKey> i = keys.iterator();
					while (i.hasNext()) {
						SelectionKey k = i.next();
						if (k.isReadable() || k.isAcceptable()) {
							buffer.clear();
							CM cm = (CM) k.attachment();
							switch (cm.type) {
							// case CM.UNKNOWN: {
							// SocketChannel tcpChannel = (SocketChannel) k
							// .channel();
							// int br = tcpChannel.read(buffer);
							// if (buffer.position() == 0) {
							// if (br == -1) {
							// // // cleanup and continue
							// } else {
							// // data is not available now check again
							// // later
							// }
							// // Thread.sleep(1000);
							// continue;
							// }
							// ByteArrayInputStream in = new
							// ByteArrayInputStream(
							// buffer.array(), 0, buffer.position());
							// cm.type = in.read();
							// System.out.println("Connected : " + cm.type);
							// for (int ki = 1; ki < buffer.position(); ki++) {
							// cm.internalBuffer[cm.bufferOffset++] = buffer
							// .get(ki);
							// }
							// }
							// break;

							case CM.CLIENT_BINARY: {
								SocketChannel tcpChannel = (SocketChannel) k
										.channel();
								int br = tcpChannel.read(buffer);
								if (buffer.position() == 0) {
									if (br == -1) {
										// // cleanup and continue
									} else {
										// data is not available now check again
										// later
									}
									// Thread.sleep(1000);
									continue;
								}
								MsgTCPNormal msg = new MsgTCPNormal(Msg.CLIENT);
								msg.cm = cm;
								msg.getDataOut().writeShort(
										Msg.MN_BINARY_CLIENT_REQUEST);
								msg.write(buffer.array(), 0, buffer.position());
								mq.add(msg);
							}
								break;
							case CM.LB:
							case CM.MP:
							case CM.WORKER:
							case CM.UNKNOWN: {
								SocketChannel tcpChannel = (SocketChannel) k
										.channel();
								int br = tcpChannel.read(buffer);
								if (buffer.position() == 0) {
									if (br == -1) {
										// // cleanup and continue
									} else {
										// data is not available now check again
										// later
									}
									// Thread.sleep(1000);
									continue;
								}
								byte tempBuffer[] = new byte[br
										+ cm.bufferOffset], tempBuffer2[];
								for (int ii = 0; ii < tempBuffer.length; ii++) {
									if (ii < cm.bufferOffset)
										tempBuffer[ii] = cm.internalBuffer[ii];
									else
										tempBuffer[ii] = buffer.get(ii
												- cm.bufferOffset);
								}
								while (true) {
									if (cm.mode == CM.MODE_LEN) {
										cm.packLen = new DataInputStream(
												new ByteArrayInputStream(
														tempBuffer)).readInt();
										cm.mode = CM.MODE_PACK;
									} else {
										if (tempBuffer.length >= 4 + cm.packLen) {
											byte pack[] = new byte[cm.packLen];
											for (int li = 0; li < cm.packLen; li++) {
												pack[li] = tempBuffer[4 + li];
											}
											tempBuffer2 = new byte[tempBuffer.length
													- 4 - cm.packLen];
											int tb2i = 0;
											for (int li = 4 + cm.packLen; li < tempBuffer.length; li++) {
												tempBuffer2[tb2i++] = tempBuffer[li];
											}
											tempBuffer = tempBuffer2;
											switch (cm.type) {
											case CM.UNKNOWN: {
												cm.type = new DataInputStream(
														new ByteArrayInputStream(
																pack))
														.read();
											}
												break;
											case CM.LB:
											case CM.MP:
											case CM.WORKER: {
												MsgTCPNormal msg = new MsgTCPNormal(
														cm.type);
												msg.cm = cm;
												msg.write(pack);
												mq.add(msg);
											}
												break;
											}
											cm.mode = CM.MODE_LEN;
										}
									}
									if (tempBuffer.length < 4)
										break;
								}
								cm.bufferOffset = 0;
								for (int ki = 0; ki < tempBuffer.length; ki++) {
									cm.internalBuffer[cm.bufferOffset++] = tempBuffer[ki];
								}
							}
								break;
							case CM.CLIENT_TEXT: {
								SocketChannel tcpChannel = (SocketChannel) k
										.channel();
								int br = tcpChannel.read(buffer);
								if (buffer.position() == 0) {
									if (br == -1) {
										// // cleanup and continue
									} else {
										// data is not available now check again
										// later
									}
									// Thread.sleep(1000);
									continue;
								}
								MsgTCPNormal msg = new MsgTCPNormal(cm.type);
								msg.cm = cm;
								msg.getDataOut().writeShort(
										Msg.MN_TEXT_CLIENT_REQUEST);
								msg.write(buffer.array(), 0, buffer.position());
								mq.add(msg);
							}
								break;
							case CM.LISTNING_INTERNAL: {
								SocketChannel sc = cm.serverSocketChannel
										.accept();
								if (sc != null) {
									register(sc, CM.UNKNOWN);
									// System.out.println("new connection");
								}
							}
								break;
							case CM.LISTNING_EXTERNAL_BINARY: {
								SocketChannel sc = cm.serverSocketChannel
										.accept();
								if (sc != null) {
									register(sc, CM.CLIENT_BINARY);
									// System.out.println("new connection");
								}
							}
								break;
							case CM.LISTNING_EXTERNAL_TEXT: {
								SocketChannel sc = cm.serverSocketChannel
										.accept();
								if (sc != null) {
									register(sc, CM.CLIENT_TEXT);
									// System.out.println("new connection");
								}
							}
								break;
							}
						}
						i.remove();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
