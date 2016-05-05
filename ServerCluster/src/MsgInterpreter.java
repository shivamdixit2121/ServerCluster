import java.nio.ByteBuffer;
import java.util.Vector;

class MsgNames {
	final static int MN_VPORT_MSG = 1;
	final static int MN_ID_INFO = 2;
	final static int MN_ID_INFO_ACK = 3;
	final static int MN_QUESTION = 4;
	final static int MN_RESPONSE = 5;
	final static int MN_IF_DUPLICATE = 6;
	final static int MN_IF_DUPLICATE_ANS = 7;
	final static int MN_DUPLICATE_FOUND = 8; // no more used
	final static int MN_UNIQUE_CONNECTION = 9;
	final static int MN_LOCAL_VPORT_MSG = 10;
	final static int MN_CONNECT_TO = 11;
	final static int MN_CLOSE_CONNECTION = 12;

	// ///
	final static int MN_NEW_CONNECTION_MADE = 13;
	final static int MN_CONNECTION_CLOSED = 14;

	final static int MN_UNABLE_TO_MAKE_REQUESTED_CONNECTION = 15;
	final static int MN_REQUESTED_CONNECTION_MADE = 16;
	final static int MN_REQUESTED_CONNECTION_CLOSED = 17;

	// ///
	final static int MN_CONNECTION_START = 18;
	final static int MN_CONNECTION_START_ACK = 19;
	final static int MN_CONNECTION_START_ACK_ACK = 20;
	final static int MN_MAKE_CONNECTION = 21;
	final static int MN_ADD_ME = 22;
	final static int MN_VERIFY_INCOMING_CONNECTION = 23;
	final static int MN_INCOMING_CONNECTION_VERIFICATION_RESULT = 24;
	final static int MN_ADD_ME_RESULT = 25;
	final static int MN_MAKE_CONNECTION_RESULT = 26;
	final static int MN_INCOMING_CONNECTION_RESULT = 27;

	final static int MN_LIST_NODES = 28;
	final static int MN_LIST_NODES_RESULT = 29;

	final static int MN_SID_REQUEST = 30;
	final static int MN_SID_REQUEST_SUCCESS = 31;
	final static int MN_SID_REQUEST_FAILURE = 32;
}

class MsgContexts {
	final static int MC_MP = 0;
	final static int MC_NON_MP = 1;
	final static int MC_LB = 2;
	final static int MC_WORKER = 3;
	final static int MC_REC_THREAD = 4;
	final static int MC_LOCAL = 5;

	final static int MAX_MC_VALUE = 5;
}

abstract class MsgInterpreter {

	int type;

	SidCTCNAddrPort otherComponent; // = new SidCTCNAddrPort;
	VarByte result; // = new VarUin8;
	VarShort protocolThread; // = new VarUin16;

	Vector<Variable[]> msgFormats[][];

	protected abstract void setMsgFormats();

	public MsgInterpreter(int type) {
		this.type = type;

		msgFormats = new Vector[MsgContexts.MAX_MC_VALUE][MsgContexts.MAX_MC_VALUE];

		otherComponent = new SidCTCNAddrPort();
		result = new VarByte();
		protocolThread = new VarShort();

		setMsgFormats();
	}

	int interpret(int sourceMsgContext, ByteBuffer buffer) throws Exception {
		short msgName = buffer.getShort();
		Variable[] msgFormat = msgFormats[sourceMsgContext][type].get(msgName);
		if (msgFormat == null)
			return -1;
		for (int i = 0; i < msgFormat.length; i++)
			msgFormat[i].read(buffer);
		return 0;
	}

	int construct(int targetMsgContext, short msgName, ByteBuffer buffer)
			throws Exception {
		Variable[] msgFormat = msgFormats[type][targetMsgContext].get(msgName);
		if (msgFormat == null)
			return -1;
		buffer.putShort(msgName);
		for (int i = 0; i < msgFormat.length; i++)
			msgFormat[i].write(buffer);
		return 0;
	}

	int interpret(int sourceMsgContext, Msg msg) throws Exception {
		return interpret(sourceMsgContext, ByteBuffer.wrap(msg.toByteArray()));
	}

	int construct(int targetMsgContext, short msgName, MsgOut msgOut)
			throws Exception {
		ByteBuffer buffer = ByteBuffer.allocate(100000);
		if (construct(targetMsgContext, msgName, buffer) != 0)
			return -1;
		msgOut.write(buffer.array(), 0, buffer.position());
		return 0;
	}
}

// ////////////

class MPMsgInterpreter extends MsgInterpreter {
	CTCNPortTokens components; // = new CTCNPortTokens;
	VarShort sid; // = new VarUin16;
	SidAddrPortTokens nodeList; // = new SidAddrPortTokens;
	VarShort waitInterval; // = new VarUin16;

	MPMsgInterpreter() {
		super(MsgContexts.MC_MP);

		components = new CTCNPortTokens();
		sid = new VarShort();
		nodeList = new SidAddrPortTokens();
		waitInterval = new VarShort();
	}

	protected void setMsgFormats() {

		msgFormats[MsgContexts.MC_MP][MsgContexts.MC_MP].ensureCapacity(100);
		
		msgFormats[MsgContexts.MC_MP][MsgContexts.MC_MP].set(MsgNames.MN_CONNECTION_START,new Variable[] { protocolThread, components });
		msgFormats[MsgContexts.MC_MP][MsgContexts.MC_MP].set(MsgNames.MN_CONNECTION_START_ACK,new Variable[] { protocolThread, components });
		msgFormats[MsgContexts.MC_MP][MsgContexts.MC_MP].set(MsgNames.MN_CONNECTION_START_ACK_ACK,new Variable[] { protocolThread});
		msgFormats[MsgContexts.MC_MP][MsgContexts.MC_MP].set(MsgNames.MN_LIST_NODES,new Variable[] { protocolThread});
		msgFormats[MsgContexts.MC_MP][MsgContexts.MC_MP].set(MsgNames.MN_LIST_NODES_RESULT,new Variable[] { protocolThread, nodeList});
		msgFormats[MsgContexts.MC_MP][MsgContexts.MC_MP].set(MsgNames.MN_SID_REQUEST_SUCCESS,new Variable[] { protocolThread, sid});
		msgFormats[MsgContexts.MC_MP][MsgContexts.MC_MP].set(MsgNames.MN_SID_REQUEST_FAILURE,new Variable[] { protocolThread, waitInterval});

//		msgFormats[MC_LB][MC_MP].resize(100);
//		msgFormats[MC_LB][MC_MP][MsgNames.MN_VERIFY_INCOMING_CONNECTION].init(new Var*[50] { &protocolThread, &otherComponent, NULL });
//		msgFormats[MC_LB][MC_MP][MsgNames.MN_INCOMING_CONNECTION_RESULT].init(new Var*[50] { &protocolThread, &otherComponent, &result, NULL });
//
//		msgFormats[MC_NON_LB][MC_MP].resize(100);
//		msgFormats[MC_NON_LB][MC_MP][MsgNames.MN_MAKE_CONNECTION_RESULT].init(new Var*[50] { &protocolThread, &otherComponent, &result, NULL });
//
//		msgFormats[MC_MP][MC_NON_LB].resize(100);
//		msgFormats[MC_MP][MC_NON_LB][MsgNames.MN_MAKE_CONNECTION].init(new Var*[50] { &protocolThread, &otherComponent, NULL });
//
//		msgFormats[MC_MP][MC_LB].resize(100);
//		msgFormats[MC_MP][MC_LB][MsgNames.MN_INCOMING_CONNECTION_VERIFICATION_RESULT].init(new Var*[50] { &protocolThread, &otherComponent, &result, NULL });
	}
};

class NonMPMsgInterpreter extends MsgInterpreter {
	VarLong workload;
	VarLong jobSlotId;

	NonMPMsgInterpreter() {
		super(MsgContexts.MC_NON_MP);
		workload = new VarLong();
		jobSlotId = new VarLong();
	}

	protected void setMsgFormats() {
	}
}

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////

class LBMsgInterpreter extends NonMPMsgInterpreter {
	VarIPAddress addr;
	VarShort port;

	LBMsgInterpreter() {

		addr = new VarIPAddress();
		port = new VarShort();
	}

	protected void setMsgFormats() {
	}
}

class WorkerMsgInterpreter extends NonMPMsgInterpreter {
	WorkerMsgInterpreter() {

	}

	protected void setMsgFormats() {
	}
}
