import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

abstract class Component<T> implements Runnable {
	int connectTo(int port, int type, int ownType, MsgOut m) {
		try {
			System.out.println("connecting : " + port);
			SocketChannel sc = SocketChannel.open();
			sc.connect(new InetSocketAddress("localhost", port));
			CM cm = receiver.register(sc, type);
			MsgOut mout = new MsgOut(type);
			mout.cm = cm;
			mout.getDataOut().writeByte(ownType);
			msgOutQueue.add(mout);
			if (m != null) {
				m.cm = cm;
				msgOutQueue.add(m);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return 1;
	}

	int connectTo(String addr, int port, int type, int ownType, MsgOut m) {
		try {
			System.out.println("connecting : " + port);
			SocketChannel sc = SocketChannel.open();
			sc.connect(new InetSocketAddress(addr, port));
			CM cm = receiver.register(sc, type);
			MsgOut mout = new MsgOut(type);
			mout.cm = cm;
			mout.getDataOut().writeByte(ownType);
			msgOutQueue.add(mout);
			if (m != null) {
				m.cm = cm;
				msgOutQueue.add(m);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return 1;
	}

	// CM newConnectionMan(int otherEndCT, int startingProtocolThread){
	//
	// }
	//
	// int HdNewConnectionMade(CM *pms) {
	// return 0;
	// }
	// int HdRequestedConnectionMade(ConnectionStartAction csa, CM *pms) {
	// return 0;
	// }
	// int HdConnectionClosed(CM *pms) {
	// return 0;
	// }
	// int HdRequestedConnectionClosed(ConnectionCloseAction cca, CM *pms) {
	// return 0;
	// }
	// int HdUnableToMakeRequestedConnection(ConnectionStartAction *csa, int
	// errorCode) {
	// return 0;
	// }
	Config config;
	LinkedBlockingQueue<Msg> msgQueue;
	LinkedBlockingQueue<MsgOut> msgOutQueue;
	Receiver receiver;
	Sender sender;
	Map<Integer, MsgHandler<T>> handlers;
	int ownPort;
	Timer timer;

	boolean keepRunning = false;

	Component() {
		config = null;
		msgQueue = null;
		msgOutQueue = null;
		receiver = null;
		sender = null;
		handlers = new HashMap<Integer, MsgHandler<T>>();
		timer = new Timer();
		timer.start();
		initializeHandlers();
	}

	abstract void initializeHandlers();

	// //////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// static int ComponentHdNewConnectionMade(MsgWrapper *mw, int senderId,
	// Component *c) {
	// MsgLocal *msgLocal = (MsgLocal*) mw->getMsg();
	// CM *cm = (CM*) msgLocal->userData;
	// switch (cm->type) {
	// case FD_TYPE_TCP_VPORT_SOCKET: {
	// CMVport *cmVport = (CMVport*) cm;
	// cmVport->mainUserCM =
	// c->newConnectionMan(cmVport->componentID.componentType,
	// CONNECTION_INITITED_BY_OTHER_SIDE);
	// if (!cmVport->mainUserCM)
	// c->vsocket->requestClose(cmVport, NULL);
	// else if (c->HdNewConnectionMade(cm) != 0)
	// c->vsocket->requestClose(cmVport, NULL);
	// return 0;
	// }
	// break;
	// }
	// }
	// static int ComponentHdRequestedConnectionMade(MsgWrapper *mw, int
	// senderId, Component *c) {
	// MsgLocal *msgLocal = (MsgLocal*) mw->getMsg();
	// CM *cm = (CM*) msgLocal->userData;
	// switch (cm->type) {
	// case FD_TYPE_TCP_VPORT_SOCKET: {
	// CMVport *cmVport = (CMVport*) cm;
	// ConnectionStartAction csa = cmVport->getCSA();
	// cmVport->setCSANull();
	//
	// cm->mainUserCM = c->newConnectionMan(cmVport->componentID.componentType,
	// CONNECTION_INITITED_BY_THIS_SIDE);
	// if (!cm->mainUserCM)
	// c->vsocket->requestClose(cmVport, NULL);
	// else if (c->HdRequestedConnectionMade(csa, cm) != 0)
	// c->vsocket->requestClose(cmVport, NULL);
	//
	// if (csa.userData) {
	// delete (int*) csa.userData;
	// }
	// return 0;
	// }
	// break;
	// }
	// return 1;
	// }
	// static int ComponentHdConnectionClosed(MsgWrapper *mw, int senderId,
	// Component *c) {
	// MsgLocal *msgLocal = (MsgLocal*) mw->getMsg();
	// CM *cm = (CM*) msgLocal->userData;
	// c->HdConnectionClosed(cm);
	// if (cm->type == FD_TYPE_TCP_VPORT_SOCKET) {
	// if (cm->mainUserCM)
	// delete (ComponentToComponentConnectionMan*) cm->mainUserCM;
	// }
	// c->vsocket->notifyConnectionCleanupComplete(cm);
	// return 0;
	// }
	// static int ComponentHdRequestedConnectionClosed(MsgWrapper *mw, int
	// senderId, Component *c) {
	// MsgLocal *msgLocal = (MsgLocal*) mw->getMsg();
	// CM *cm = (CM*) msgLocal->userData;
	// switch (cm->type) {
	// case FD_TYPE_TCP_VPORT_SOCKET:
	// case FD_TYPE_TCP_CLIENT:
	// case FD_TYPE_TCP_UNKNOWN:
	// case FD_TYPE_TCP_LOOPBACK_SOCKET: {
	// CMTCPConnected *cmConnected = (CMTCPConnected*) cm;
	// ConnectionCloseAction cca = cmConnected->getCCA();
	// if (cca.byVport != c->vsocket->getOwnVport()) {
	// c->HdConnectionClosed(cm);
	// } else {
	// c->HdRequestedConnectionClosed(cca, cm);
	// }
	// if (cm->type == FD_TYPE_TCP_VPORT_SOCKET) {
	// if (cm->mainUserCM)
	// delete (ComponentToComponentConnectionMan*) cm->mainUserCM;
	// }
	// if (cca.byVport == c->vsocket->getOwnVport()) {
	// cmConnected->setCCANull();
	// if (cca.userData)
	// delete (int*) cca.userData;
	// }
	// c->vsocket->notifyConnectionCleanupComplete(cm);
	// return 0;
	// }
	// break;
	// }
	// }
	// static int ComponentHdUnableToMakeRequestedConnection(MsgWrapper *mw, int
	// senderId, Component *c) {
	// MsgLocal *msgLocal = (MsgLocal*) mw->getMsg();
	// BufferInputStream st(msgLocal->getData(), msgLocal->getMsgLength());
	// ConnectionStartAction *csa = (ConnectionStartAction*) msgLocal->userData;
	// st.readPositiveInt(); //skip msgName;
	// int errorCode = st.readPositiveInt();
	// c->HdUnableToMakeRequestedConnection(csa, errorCode);
	// if (csa->userData)
	// delete (int*) csa->userData;
	// delete csa;
	// return 0;
	// }
	Thread t;

	void go() {
		keepRunning = true;
		t = new Thread(this);
		t.start();
	}

	int stop() {
		keepRunning = false;
		try {
			t.join();
			return 0;
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return 1;
	}

	public void run() {
		loop();
	}

	abstract void handleTextBasedClient(MsgTCPNormal msg);

	public void loop() {
		receiver.start();
		sender.start();
		Msg msg;
		CM cm;
		DataInputStream in;
		while (keepRunning) {
			try {
				msg = msgQueue.take();
				if (msg != null) {
					cm = msg.cm;
					if (cm == null)
						cm = new CM();
					if (cm != null && cm.type == CM.CLIENT_TEXT) {
						handleTextBasedClient((MsgTCPNormal) msg);
					} else {
						in = new DataInputStream(new ByteArrayInputStream(
								msg.toByteArray()));
						int msgName = in.readShort();
						MsgHandler<T> handler = handlers.get(msgName);
						if (handler != null) {
							handler.handle(msg, cm.type);
						}
					}
				}
			} catch (Exception e) {
				// e.printStackTrace();
			}
		}
	}
}

abstract class NonMPComponent<T> extends Component<T> {
	LinkedBlockingQueue<Msg> mpMsgQueue;
	int mpPort;

	public NonMPComponent(int ownPort, int mpPort,
			LinkedBlockingQueue<Msg> mpMsgQueue) {
		this.mpPort = mpPort;
		this.mpMsgQueue = mpMsgQueue;
		this.ownPort = ownPort;

		msgQueue = new LinkedBlockingQueue<Msg>();
		try {
			receiver = new Receiver(ownPort, msgQueue);
		} catch (IOException e) {
			e.printStackTrace();
		}
		sender = new Sender();
		msgOutQueue = sender.getMoq();

		connectToMP();

	}

	int connectToMP() {

		return 0;
	}

}
