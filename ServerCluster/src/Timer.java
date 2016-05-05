import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.LinkedBlockingQueue;

abstract class TimedEvent {
	int time;
	LinkedBlockingQueue<Msg> targetMsgQueue;

	TimedEvent(int time, LinkedBlockingQueue<Msg> targetMsgQueue) {
		this.time = time;
		this.targetMsgQueue = targetMsgQueue;
	}

	abstract int execute();
}

public class Timer implements Runnable {
	int uTime;
	int sleepTime = 1000; // ms

	Map<Integer, Vector<TimedEvent>> events;
	Thread t;
	boolean keepRunning;
	LinkedBlockingQueue<TimedEvent> requests;

	Timer() {
		events = new HashMap<Integer, Vector<TimedEvent>>();
		requests = new LinkedBlockingQueue<TimedEvent>();
		keepRunning = false;
	}

	void set(TimedEvent te) {
		requests.add(te);
	}

	void start() {
		keepRunning = true;
		t = new Thread(this);
		t.start();
	}

	void stop() {
		keepRunning = false;
		try {
			t.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void run() {
		while (keepRunning) {
			while (true) {
				TimedEvent te = requests.poll();
				if (te == null)
					break;
				Vector<TimedEvent> tev = events.get(te.time);
				if (tev == null) {
					tev = new Vector<TimedEvent>();
					events.put(te.time, tev);
				}
				tev.add(te);
			}
			try {
				Thread.sleep(sleepTime);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			uTime++;
			Vector<TimedEvent> tev = events.get(uTime);
			if (tev != null) {
				for (TimedEvent te : tev) {
					MsgLocal m = new MsgLocal(Msg.TIMER, 0, te);
					try {
						m.getDataOut().writeShort(Msg.MN_ALARM);
						te.targetMsgQueue.add(m);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
	}
}
