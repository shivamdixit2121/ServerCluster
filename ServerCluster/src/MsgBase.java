import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

abstract class MsgBase extends ByteArrayOutputStream {

	final static int MN_NEW_NODE_JOIN = 1;
	final static int MN_NEW_NODE_JOIN_ACK = 2;
	final static int MN_COMPONENT_CONFIRMATION = 3;
	final static int MN_COMPONENT_CONFIRMATION_RESULT = 4;
	final static int MN_NEW_WORKER_JOIN = 5;
	final static int MN_NEW_WORKER_JOIN_RESULT = 6;
	final static int MN_JOB = 7;
	final static int MN_JOB_RESULT = 8;
	final static int MN_LOAD_UPDATE = 9;
	final static int MN_BINARY_CLIENT_REQUEST = 10;
	final static int MN_CONNECT_TO = 11;
	final static int MN_ALARM = 12;
	final static int MN_TELL_LOAD = 13;
	final static int MN_TELL_LOAD_ANS = 14;
	final static int MN_TELL_STATUS = 15;
	final static int MN_TELL_STATUS_ANS = 16;
	final static int MN_TEXT_CLIENT_REQUEST = 17;
	final static int MN_SERVER_STATUS = 18;

	final static int LOCAL = 1;
	final static int MP = 2;
	final static int LB = 3;
	final static int WORKER = 4;
	final static int CLIENT = 5;
	final static int TIMER = 6;

	int type;

	MsgBase(int type) {
		this.type = type;
	}

	int getType() {
		return type;
	}

	public CM cm;

	DataInputStream getDataIn() {
		return new DataInputStream(new ByteArrayInputStream(toByteArray()));
	}

	DataOutputStream getDataOut() {
		return new DataOutputStream(this);
	}

	// abstract int getData();
	// abstract int destroy();
}
