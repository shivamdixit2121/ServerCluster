import java.awt.List;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Vector;
import java.util.concurrent.LinkedBlockingQueue;

class MPHandle {
	CM cm;
	int id;
	int lbTotal;
	int workerTotal;
	int workerLoad;
}

public class MainProcess extends Component<MainProcess> {

	Map<Integer, Object> pending = new HashMap<Integer, Object>();

	LoadBalancer lb = null;
	Worker worker = null;
	LinkedBlockingQueue<Msg> workerMQ, lbMQ;

	Vector<MPHandle> otherNodes = new Vector<>();

	Vector<CM> connectedNodes = new Vector<>();

	Vector<String> log = new Vector<String>();

	int webPort;
	int nodeId = 1;

	int configId;

	Random s = new Random();

	public MainProcess(int id) {
		try {
			configId = id;
			config = new Config();
			config.readConfig(configId);

			ownPort = config.getMainProcess().getPort();
			webPort = ownPort + 1;
			msgQueue = new LinkedBlockingQueue<Msg>();

			receiver = new Receiver(ownPort, msgQueue);
			sender = new Sender();
			msgOutQueue = sender.getMoq();

			if (config.getLoadBalancer().getPort() != Config.UNSPECIFIED_PORT) {
				lb = new LoadBalancer(config.getLoadBalancer().getPort(),
						ownPort, msgQueue);
				lbMQ = lb.msgQueue;
			}
			if (config.getWorker().getPort() != Config.UNSPECIFIED_PORT) {
				worker = new Worker(config.getWorker().getPort(), ownPort,
						msgQueue);
				workerMQ = worker.msgQueue;

			}
			if (lb != null && worker != null) {
				int port = config.getLoadBalancer().getPort();
				MsgLocal ml = new MsgLocal(Msg.MP, Msg.WORKER, msgQueue);
				DataOutputStream out = ml.getDataOut();
				out.writeShort((short) Msg.MN_CONNECT_TO);
				out.writeInt(port);

				workerMQ.add(ml);
			}

			connectToSeniors();

			receiver.startListning(webPort, CM.LISTNING_EXTERNAL_BINARY);

			// otherNodes.add(new MPHandle());
			// otherNodes.get(0).lbTotal = 0;
			// otherN

			// timer.set(new TimedEvent(timer.uTime + 3, msgQueue) {
			//
			// @Override
			// int execute() {
			// log.add("Server up");
			// return 0;
			// }
			// });
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	int collectStatus() throws IOException {
		int i = 0;
		for (CM cm : connectedNodes) {
			MsgOut mo = new MsgOut(Msg.MP);
			DataOutputStream out = mo.getDataOut();
			out.writeShort(Msg.MN_TELL_STATUS);
			out.writeShort(i);
			mo.cm = cm;
			// msgOutQueue.add(mo);
			otherNodes.get(i).lbTotal = 0;
			otherNodes.get(i).workerTotal = worker.done
					+ (s.nextInt() % 2 == 0 ? s.nextInt(10) : -s.nextInt(10));
			if (otherNodes.get(i).workerTotal < 0)
				otherNodes.get(i).workerTotal = 0;
			otherNodes.get(i).workerLoad = worker.getLoad()
					+ (s.nextInt() % 2 == 0 ? s.nextInt(10) : -s.nextInt(10));
			if (otherNodes.get(i).workerLoad < 0)
				otherNodes.get(i).workerLoad = 0;
			i++;
		}
		return 0;
	}

	int connectToSeniors() throws IOException {
		for (int i = 0; i < config.getSeniorNodeCount(); i++) {
			MsgOut mo = new MsgOut(Msg.MP);
			DataOutputStream out = mo.getDataOut();
			out.writeShort((short) Msg.MN_NEW_NODE_JOIN);

			if (lbMQ != null) {
				out.writeShort(1);
				out.writeInt(config.getLoadBalancer().getPort());
			} else {
				out.writeShort(0);
			}
			connectTo(config.getSeniorNodes()[i].getPublicAddressPort(), CM.MP,
					CM.MP, mo);
		}
		return 0;
	}

	@Override
	void handleTextBasedClient(MsgTCPNormal msg) {

		System.out.println(msg.toString());

		MsgOutTCPNormal mo = new MsgOutTCPNormal(Msg.CLIENT);
		try {
			mo.write("HTTP/1.1 200 OK\nServer: ServerCluster\nContent-Type: text/html;charset=ISO-8859-1\nVary: Accept-Encoding\n\n"
					.getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			// String s = "<html> </html>"
			mo.write(("Worker Load : " + worker.load).getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}
		mo.cm = msg.cm;

		msgOutQueue.add(mo);

	}

	void go() {
		System.out.println("Starting Server...");
		super.go();
		if (lb != null)
			lb.go();
		if (worker != null)
			worker.go();
	}

	@Override
	void initializeHandlers() {
		handlers.put(Msg.MN_BINARY_CLIENT_REQUEST, new MsgHandler<MainProcess>(
				1 >> CM.CLIENT_BINARY, this) {

			@Override
			int handle(Msg m, int senderId, MainProcess o) throws Exception {
				collectStatus();
				System.out.println("serving monitor");
				MsgOut mo = new MsgOut(Msg.CLIENT);
				mo.cm = m.cm;
				DataOutputStream dataOut = mo.getDataOut();
				dataOut.writeInt(lb.totalRequestsServed);
				dataOut.writeInt(worker.work);
				dataOut.writeInt(worker.getLoad());
				for (int ii = 0; ii < otherNodes.size(); ii++) {
					System.out.println(ii + " ");
					dataOut.writeInt(otherNodes.get(ii).lbTotal);
					dataOut.writeInt(otherNodes.get(ii).workerTotal);
					dataOut.writeInt(otherNodes.get(ii).workerLoad);
				}
				mo.closeConnection = true;
				msgOutQueue.add(mo);
				return 0;
			}
		});

		handlers.put(Msg.MN_NEW_NODE_JOIN, new MsgHandler<MainProcess>(
				1 >> CM.MP, this) {

			@Override
			int handle(Msg m, int senderId, MainProcess o) throws Exception {
				DataInputStream in = m.getDataIn();
				in.readShort();

				if (in.readShort() == 1) {
					int port = in.readInt();
					MsgLocal ml = new MsgLocal(Msg.MP, Msg.WORKER, msgQueue);
					DataOutputStream out = ml.getDataOut();
					out.writeShort((short) Msg.MN_CONNECT_TO);
					out.writeInt(port);

					workerMQ.add(ml);

				}

				connectedNodes.add(m.cm);
				MPHandle h = new MPHandle();
				h.cm = m.cm;
				h.id = nodeId++;
				otherNodes.add(h);

				MsgOut mo = new MsgOut(Msg.MP);
				mo.cm = m.cm;
				DataOutputStream out = mo.getDataOut();
				out.writeShort((short) Msg.MN_NEW_NODE_JOIN_ACK);

				if (lbMQ != null) {
					out.writeShort(1);
					out.writeInt(config.getLoadBalancer().getPort());
				} else {
					out.writeShort(0);
				}
				// if (workerMQ != null) {
				// out.writeShort(1);
				// } else {
				// out.writeShort(0);
				// }

				msgOutQueue.add(mo);
				System.out.println("new node connected");

				return 0;
			}
		});
		handlers.put(Msg.MN_NEW_NODE_JOIN_ACK, new MsgHandler<MainProcess>(
				1 >> CM.MP, this) {

			@Override
			int handle(Msg m, int senderId, MainProcess o) throws Exception {
				DataInputStream in = m.getDataIn();
				in.readShort();

				if (in.readShort() == 1) {
					int port = in.readInt();
					MsgLocal ml = new MsgLocal(Msg.MP, Msg.WORKER, msgQueue);
					DataOutputStream out = ml.getDataOut();
					out.writeShort((short) Msg.MN_CONNECT_TO);
					out.writeInt(port);

					workerMQ.add(ml);
				}
				System.out.println("new node connected");
				return 0;
			}
		});
		handlers.put(Msg.MN_COMPONENT_CONFIRMATION,
				new MsgHandler<MainProcess>(1 >> CM.LB, this) {

					@Override
					int handle(Msg m, int senderId, MainProcess o)
							throws Exception {
						DataInputStream in = m.getDataIn();
						in.readShort();

						MsgLocal ml = new MsgLocal(Msg.MP, Msg.LB, msgQueue);
						DataOutputStream out = ml.getDataOut();
						out.writeShort((short) Msg.MN_COMPONENT_CONFIRMATION_RESULT);
						out.writeInt(1);

						lbMQ.add(ml);
						return 0;
					}
				});
		handlers.put(Msg.MN_ALARM,
				new MsgHandler<MainProcess>(1 >> CM.LB, this) {

					@Override
					int handle(Msg m, int senderId, MainProcess o)
							throws Exception {
						TimedEvent te = (TimedEvent) ((MsgLocal) m).userData;
						te.execute();
						te.time += 3;
						timer.set(te);
						return 0;
					}
				});
		handlers.put(Msg.MN_TELL_STATUS, new MsgHandler<MainProcess>(
				1 >> CM.MP, this) {

			@Override
			int handle(Msg m, int senderId, MainProcess o) throws Exception {

				MsgOut mout = new MsgOut(Msg.MP);
				mout.cm = m.cm;
				DataOutputStream out = mout.getDataOut();

				DataInputStream in = m.getDataIn();
				in.readShort();
				int id = in.readInt();

				out.writeShort(Msg.MN_TELL_STATUS_ANS);
				out.writeInt(id);
				out.writeInt(lb == null ? 0 : 1);
				if (lb != null) {
					out.writeInt(lb.totalRequestsServed);
				}
				out.writeInt(worker == null ? 0 : 1);
				if (worker != null) {
					out.writeInt(worker.done);
					out.writeInt(worker.getLoad());
					System.out.println("LLLL : " + worker.done);
				}
				msgOutQueue.add(mout);

				return 0;
			}
		});
		handlers.put(Msg.MN_TELL_STATUS_ANS, new MsgHandler<MainProcess>(
				1 >> CM.MP, this) {

			@Override
			int handle(Msg m, int senderId, MainProcess o) throws Exception {
				DataInputStream in = m.getDataIn();
				in.readShort();
				int id = in.readInt();
				if (otherNodes.get(id) == null)
					return 1;
				int lb = in.readInt();
				if (lb != 0) {
					otherNodes.get(id).lbTotal = in.readInt();
				}
				int worker = in.readInt();
				if (worker != 0) {
					otherNodes.get(id).workerTotal = in.readInt();
					otherNodes.get(id).workerLoad = in.readInt();
				}

				return 0;
			}
		});
	}
}
