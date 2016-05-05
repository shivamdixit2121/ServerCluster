class MsgOut extends MsgBase {
	final static int CLIENT = 1;
	final static int LOCAL = 2;

	boolean closeConnection = false;

	MsgOut(int type) {
		super(type);
	}
}

class MsgOutLocal extends MsgOut {
	MsgOutLocal() {
		super(LOCAL);
	}
}

// /////////////////////////////////////////////
class MsgOutTCP extends MsgOut {
	MsgOutTCP(int type) {
		super(type);
	}
}

class MsgOutTCPNormal extends MsgOutTCP {
	MsgOutTCPNormal(int type) {
		super(type);
	}
}

class MsgOutTCPMagicNumberProtocol extends MsgOutTCP {
	MsgOutTCPMagicNumberProtocol(int type) {
		super(type);
	}
}