class Msg extends MsgBase {

	Msg(int type) {
		super(type);
	}
}

class MsgLocal extends Msg {
	int from;
	int to;
	Object userData;
	public static final int UNSPECIFIED = -1;

	MsgLocal(int to, Object userData) {
		super(LOCAL);
		from = UNSPECIFIED;
		this.to = to;
		this.userData = userData;
	}

	MsgLocal(int from, int to, Object userData) {
		super(LOCAL);
		this.from = from;
		this.to = to;
		this.userData = userData;
	}

	int setFrom(int from) {
		this.from = from;
		return 0;
	}

	int getFrom() {
		return from;
	}

	int setTo(int to) {
		this.to = to;
		return 0;
	}

	int getTo() {
		return to;
	}
}

// /////////////////////////////////////////////
class MsgTCP extends Msg {
	MsgTCP(int type) {
		super(type);
	}
}

class MsgTCPNormal extends MsgTCP {
	MsgTCPNormal(int type) {
		super(type);
	}
}

class MsgTCPMagicNumberProtocol extends MsgTCP {
	MsgTCPMagicNumberProtocol(int type) {
		super(type);
	}
}