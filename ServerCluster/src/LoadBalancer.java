import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

class WorkerHandle {
	CM cm;
	int load;

	public WorkerHandle(CM cm, int load) {
		super();
		this.cm = cm;
		this.load = load;
	}

}

public class LoadBalancer extends NonMPComponent<LoadBalancer> {
	int jobId = 0;
	int turns = 0;
	int totalRequestsServed;
	Map<Integer, CM> jobs;
	int waitBeforeReorder = 10;

	int id1, id2, id3, id4;
	int iii = 0;
	LinkedList<WorkerHandle> workers = new LinkedList<WorkerHandle>();

	@Override
	void handleTextBasedClient(MsgTCPNormal msg) {
		new Thread(new Runnable() {

			@Override
			public void run() {
				int webPort = 10001;
				workers.add(new WorkerHandle(null, id1));
				workers.add(new WorkerHandle(null, id2));
				workers.add(new WorkerHandle(null, id3));
				workers.add(new WorkerHandle(null, id4));
				// TODO Auto-generated method stub
				ServerSocket ss;
				try {
					ss = new ServerSocket(webPort);
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
					return;
				}
				while (true) {
					Socket s;
					try {
						s = ss.accept();

						InputStream in = s.getInputStream();
						byte[] request = new byte[1000];
						in.read(request);
						System.out.println(new String(request));
						if (iii == 0) {
							workers.set(0, new WorkerHandle(null, id1++));
							iii = 1;
						} else if (iii == 1) {
							workers.set(1, new WorkerHandle(null, id2++));
							iii = 2;
						} else if (iii == 2) {
							workers.set(2, new WorkerHandle(null, id3++));
							iii = 3;
						} else if (iii == 3) {
							workers.set(3, new WorkerHandle(null, id4++));
							iii = 0;
						}
						OutputStream out;
						out = s.getOutputStream();
						out.write("HTTP/1.1 200 OK\nServer: ServerCluster\nContent-Type: text/html;charset=ISO-8859-1\nVary: Accept-Encoding\n\n"
								.getBytes());
						out.write(("Worker 1 Load : " + workers.get(0).load
								+ "<br> Worker 2 Load : " + workers.get(1).load
								+ "<br> Worker 3 Load : " + workers.get(2).load
								+ "<br> Worker 4 Load : " + workers.get(3).load)
								.getBytes());
						s.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}).start();
		// TODO Auto-generated method stub
	}

	public LoadBalancer(int ownPort, int mpPort,
			LinkedBlockingQueue<Msg> mpMsgQueue) {
		super(ownPort, mpPort, mpMsgQueue);
		try {
			receiver.startListning(ownPort + 1, CM.LISTNING_EXTERNAL_BINARY);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// try {
		// handleTextBasedClient(null);
		// } catch (IOException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
	}

	// void cycle(){
	// WorkerHandle wh = workers.get(0);
	// workers.set(0, element)
	// }

	void reorder() {
		LinkedList<WorkerHandle> _w = new LinkedList<WorkerHandle>();
		while (workers.size() > 0) {
			int min = workers.get(0).load;
			WorkerHandle wh = workers.get(0);
			for (WorkerHandle workerHandle : workers) {
				if (workerHandle.load < min) {
					min = workerHandle.load;
					wh = workerHandle;
				}
			}
			_w.add(wh);
			workers.remove(wh);
		}
		workers = _w;
	}

	@Override
	void initializeHandlers() {

		handlers.put(Msg.MN_NEW_WORKER_JOIN, new MsgHandler<LoadBalancer>(
				1 >> CM.WORKER, this) {

			@Override
			int handle(Msg m, int senderId, LoadBalancer o) throws Exception {
				// DataInputStream in = m.getDataIn();
				// in.readShort();
				// MsgLocal ml = new MsgLocal(Msg.LB, Msg.MP, msgQueue);
				// DataOutputStream out = ml.getDataOut();
				// out.writeShort((short) Msg.MN_COMPONENT_CONFIRMATION);
				// out.writeInt(in.readInt());
				//
				// mpMsgQueue.add(ml);

				DataInputStream in = m.getDataIn();
				in.readShort();
				MsgOut mout = new MsgOut(Msg.WORKER);
				mout.cm = m.cm;
				DataOutputStream out = mout.getDataOut();
				out.writeShort((short) Msg.MN_NEW_WORKER_JOIN_RESULT);
				out.writeInt(1);

				msgOutQueue.add(mout);
				WorkerHandle wh = new WorkerHandle(m.cm, 0);
				workers.add(wh);
				m.cm.wh = wh;
				reorder();

				System.out.println("Worker added");

				return 0;
			}
		});
		handlers.put(Msg.MN_COMPONENT_CONFIRMATION_RESULT,
				new MsgHandler<LoadBalancer>(1 >> CM.MP, this) {

					@Override
					int handle(Msg m, int senderId, LoadBalancer o)
							throws Exception {
						DataInputStream in = m.getDataIn();
						in.readShort();
						MsgOut mo = new MsgOut(Msg.WORKER);
						DataOutputStream out = mo.getDataOut();
						out.writeShort((short) Msg.MN_NEW_WORKER_JOIN_RESULT);
						int r = in.readInt();
						out.writeInt(in.readInt());

						msgOutQueue.add(mo);

						if (r == 1) {

						}

						return 0;
					}
				});
		handlers.put(Msg.MN_BINARY_CLIENT_REQUEST,
				new MsgHandler<LoadBalancer>(1 >> CM.CLIENT_BINARY, this) {

					@Override
					int handle(Msg m, int senderId, LoadBalancer o)
							throws Exception {
						System.out.println("serving");
						DataInputStream in = m.getDataIn();
						MsgOut mo = new MsgOut(Msg.WORKER);
						mo.cm = workers.get(0).cm;

						DataOutputStream out = mo.getDataOut();
						out.writeShort((short) Msg.MN_JOB);
						out.writeInt(jobId++);
						out.writeInt(in.readInt());
						out.writeInt(in.readInt());
						out.writeInt(in.readInt());

						msgOutQueue.add(mo);
						workers.get(0).load++;
						totalRequestsServed++;
						if (turns++ % waitBeforeReorder == 0)
							reorder();

						return 0;
					}
				});

		handlers.put(Msg.MN_LOAD_UPDATE, new MsgHandler<LoadBalancer>(
				1 >> CM.WORKER, this) {

			@Override
			int handle(Msg m, int senderId, LoadBalancer o) throws Exception {
				DataInputStream in = m.getDataIn();
				in.readShort();

				m.cm.wh.load = in.readInt();
				reorder();

				return 0;
			}
		});

		handlers.put(Msg.MN_JOB_RESULT, new MsgHandler<LoadBalancer>(
				1 >> CM.WORKER, this) {

			@Override
			int handle(Msg m, int senderId, LoadBalancer o) throws Exception {
				DataInputStream in = m.getDataIn();
				in.readShort();
				int jobID = in.readInt();
				MsgOut mo = new MsgOut(Msg.CLIENT);
				mo.cm = jobs.get(jobID);
				mo.closeConnection = true;
				DataOutputStream out = mo.getDataOut();
				out.writeInt(in.readInt());
				msgOutQueue.add(mo);
				jobs.remove(jobID);

				return 0;
			}
		});
	}
}