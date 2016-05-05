import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;

public class Worker extends NonMPComponent<Worker> {
	int load = 0;
	int lbCount = 0;
	int id;
	int work = 0, done = 0;

	Thread t;
	LinkedBlockingQueue<Msg> workQueue = new LinkedBlockingQueue<>();

	public Worker(int ownPort, int mpPort, LinkedBlockingQueue<Msg> mpMsgQueue) {
		super(ownPort, mpPort, mpMsgQueue);
		work();
		// try {
		// receiver.startListning(3000);
		// } catch (IOException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
	}

	int getLoad() {
		return work - done;
	}

	int work() {
		t = new Thread(new Runnable() {

			@Override
			public void run() {
				while (true) {
					try {
						Msg m = workQueue.take();
						DataInputStream in = m.getDataIn();
						in.readShort();
						int jobID = in.readInt();
						int a = in.readInt();
						int b = in.readInt();
						int c = in.readInt();

						System.out.println("New Job");

						// tedious work;
						System.out.println("load : " + load);
						for (int i = 0; i < 1; i++) {
							for (int j = 0; j < c; j++)
								;
							try {
								Thread.sleep(10);
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
						//

						MsgOut mo = new MsgOut(Msg.LB);
						mo.cm = m.cm;
						DataOutputStream out = mo.getDataOut();
						out.writeShort((short) Msg.MN_JOB_RESULT);
						out.writeInt(jobID);
						out.writeInt(a + b);

						msgOutQueue.add(mo);

					} catch (Exception e) {
						e.printStackTrace();
					} finally {
						done++;
						System.out.println("Done : " + done);
					}
				}
			}
		});
		t.start();
		return 0;
	}

	@Override
	void initializeHandlers() {
		handlers.put(Msg.MN_CONNECT_TO,
				new MsgHandler<Worker>(1 >> CM.MP, this) {

					@Override
					int handle(Msg m, int senderId, Worker o) throws Exception {
						DataInputStream in = m.getDataIn();
						in.readShort();
						MsgOut mout = new MsgOut(Msg.LB);
						mout.getDataOut().writeShort(Msg.MN_NEW_WORKER_JOIN);
						connectTo(in.readInt(), CM.LB, CM.WORKER, mout);
						return 0;
					}
				});
		handlers.put(Msg.MN_TELL_LOAD,
				new MsgHandler<Worker>(1 >> CM.MP, this) {

					@Override
					int handle(Msg m, int senderId, Worker o) throws Exception {
						DataInputStream in = m.getDataIn();
						in.readShort();

						MsgOut mout = new MsgOut(Msg.MP);
						DataOutputStream out = mout.getDataOut();
						out.writeShort(Msg.MN_TELL_LOAD_ANS);
						out.writeInt(load);

						msgOutQueue.add(mout);

						return 0;
					}
				});

		// handlers.put(Msg.MN_CLIENT_REQUEST, new MsgHandler<Worker>(
		// 1 >> CM.CLIENT, this) {
		//
		// @Override
		// int handle(Msg m, int senderId, Worker o) throws Exception {
		// DataInputStream in = m.getDataIn();
		// in.readShort();
		//
		// System.out.println(m.toString());
		//
		// MsgOutTCPNormal mo = new MsgOutTCPNormal(Msg.CLIENT);
		// mo.write("HTTP/1.1 200 OK\nServer: ServerCluster\nContent-Type: text/html;charset=ISO-8859-1\nVary: Accept-Encoding\n\n"
		// .getBytes());
		// mo.write(("Load " + load).getBytes());
		// mo.cm = m.cm;
		//
		// msgOutQueue.add(mo);
		//
		// return 0;
		// }
		// });

		handlers.put(Msg.MN_NEW_WORKER_JOIN_RESULT, new MsgHandler<Worker>(
				1 >> CM.LB, this) {

			@Override
			int handle(Msg m, int senderId, Worker o) throws Exception {
				DataInputStream in = m.getDataIn();
				in.readShort();

				MsgOut mo = new MsgOut(Msg.LB);
				mo.cm = m.cm;
				DataOutputStream out = mo.getDataOut();
				out.writeShort((short) Msg.MN_LOAD_UPDATE);
				out.writeInt(load);

				msgOutQueue.add(mo);

				timer.set(new TimedEvent(timer.uTime + 5, msgQueue) {

					@Override
					int execute() {
						MsgOut mo = new MsgOut(Msg.LB);
						mo.cm = m.cm;
						DataOutputStream out = mo.getDataOut();
						try {
							out.writeShort((short) Msg.MN_LOAD_UPDATE);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						try {
							out.writeInt(load);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

						msgOutQueue.add(mo);

						return 0;
					}
				});

				return 0;
			}
		});

		handlers.put(Msg.MN_JOB, new MsgHandler<Worker>(1 >> CM.LB, this) {

			@Override
			int handle(Msg m, int senderId, Worker o) throws Exception {
				workQueue.add(m);
				work++;
				System.out.println("Work : " + work);
				return 0;
			}
		});
	}

	@Override
	void handleTextBasedClient(MsgTCPNormal msg) {
		// TODO Auto-generated method stub

	}
}
