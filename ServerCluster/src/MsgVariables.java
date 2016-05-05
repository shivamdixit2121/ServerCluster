import java.nio.ByteBuffer;

abstract class Variable {
	abstract int read(ByteBuffer buffer);

	abstract int write(ByteBuffer buffer);
}

abstract class MultiVariable extends Variable {
	final static int MAX_COMPONENTS_TOKENS = 32;
	short count;

	int read(ByteBuffer buffer) {
		count = buffer.getShort();
		if (count < minAllowedCount() || count > maxAllowedCount())
			return -1;
		for (int i = 0; i < count; i++) {
			if (readBlock(buffer, i) != 0)
				return -1;
		}
		return 0;
	}

	int write(ByteBuffer buffer) {
		buffer.putShort(count);
		for (int i = 0; i < count; i++) {
			if (writeBlock(buffer, i) != 0)
				return -1;
		}
		return 0;
	}

	abstract int minAllowedCount();

	abstract int maxAllowedCount();

	abstract int readBlock(ByteBuffer buffer, int index);

	abstract int writeBlock(ByteBuffer buffer, int index);
}

// ///////////////////////////////////////////////////////
class VarByte extends Variable {

	byte value;

	@Override
	int read(ByteBuffer buffer) {
		value = buffer.get();
		return 0;
	}

	@Override
	int write(ByteBuffer buffer) {
		buffer.put(value);
		return 0;
	}

}

class VarShort extends Variable {
	private int value;

	@Override
	int read(ByteBuffer buffer) {
		value = buffer.getShort();
		return 0;
	}

	@Override
	int write(ByteBuffer buffer) {
		buffer.putShort((short) value);
		return 0;
	}

	public int getValue() {
		return value;
	}

	public void setValue(int value) {
		this.value = value;
	}
}

class VarInt extends Variable {

	int value;

	@Override
	int read(ByteBuffer buffer) {
		value = buffer.getInt();
		return 0;
	}

	@Override
	int write(ByteBuffer buffer) {
		buffer.putInt(value);
		return 0;
	}

}

class VarLong extends Variable {

	long value;

	@Override
	int read(ByteBuffer buffer) {
		value = buffer.getLong();
		return 0;
	}

	@Override
	int write(ByteBuffer buffer) {
		buffer.putLong(value);
		return 0;
	}

}

class VarIPAddress extends Variable {
final static int IPV4 = 1;
final static int IPV6 = 2;
byte addrVersion;
byte addr[] = new byte[16];

static String inStringFormat(byte[] addr) {
	String addrString = new String();
	for (int i = 0; i < 3; i++) {
		addrString = addrString.concat((int) (addr[i] & 0xff) + ".");
	}
	addrString = addrString.concat((int) (addr[3] & 0xff) + "");
	return addrString;
}

@Override
int read(ByteBuffer buffer) {
	addrVersion = buffer.get();
	if (addrVersion == IPV4) {
		buffer.get(addr, 0, 4);
		return 0;
	} else if (addrVersion == IPV6) {
		buffer.get(addr, 0, 16);
		return 0;
	}
	return -1;
}

@Override
int write(ByteBuffer buffer) {
	if (addrVersion == IPV4) {
		buffer.put(addr, 0, 4);
		return 0;
	} else if (addrVersion == IPV6) {
		buffer.put(addr, 0, 16);
		return 0;
	}
	return -1;
}

void init(byte a, byte b, byte c, byte d) {
	addrVersion = IPV4;
	addr[0] = a;
	addr[1] = b;
	addr[2] = c;
	addr[3] = d;
}
}

// ///////////////////////////////////////////////////////////////
class CTCNPort {
	int componentType;
	int componentNumber;
	int port;
}

class CTCNPortTokens extends MultiVariable {
	CTCNPort tokens[] = new CTCNPort[MAX_COMPONENTS_TOKENS];

	protected int readBlock(ByteBuffer in, int index) {
		tokens[index].componentType = in.getShort();
		tokens[index].componentNumber = in.getShort();
		tokens[index].port = in.getShort();
		return 0;
	}

	int writeBlock(ByteBuffer out, int index) {
		out.putShort((short) tokens[index].componentType);
		out.putShort((short) tokens[index].componentNumber);
		out.putShort((short) tokens[index].port);
		return 0;
	}

	int minAllowedCount() {
		return 0;
	}

	int maxAllowedCount() {
		return MAX_COMPONENTS_TOKENS;
	}

}

class SidCTCNAddrPort extends Variable {
	int sid;
	int componentType;
	int componentNumber;
	VarIPAddress addr;
	int port;

	int read(ByteBuffer in) {
		sid = in.getShort();
		componentType = in.getShort();
		componentNumber = in.getShort();
		addr.read(in);
		port = in.getShort();
		return 0;
	}

	int write(ByteBuffer out) {
		out.putShort((short) sid);
		out.putShort((short) componentType);
		out.putShort((short) componentNumber);
		addr.write(out);
		out.putShort((short) port);
		return 0;
	}

}

class SidAddrPort {
	int sid;
	VarIPAddress addr;
	int port;
};

class SidAddrPortTokens extends MultiVariable {
	SidAddrPort tokens[] = new SidAddrPort[MAX_COMPONENTS_TOKENS];

	int readBlock(ByteBuffer in, int index) {
		tokens[index].sid = in.getShort();
		tokens[index].addr.read(in);
		tokens[index].port = in.getShort();
		return 0;
	}

	int writeBlock(ByteBuffer out, int index) {
		out.putShort((short) tokens[index].sid);
		tokens[index].addr.write(out);
		out.putShort((short) tokens[index].port);
		return 0;
	}

	int minAllowedCount() {
		return 0;
	}

	int maxAllowedCount() {
		return MAX_COMPONENTS_TOKENS;
	}
};

// ///////////////////////////////////////////////////////////////
//class VarUserIds extends MultiVariable {
//	int ids[];
//
//	public VarUserIds() {
//		ids = new int[32];
//	}
//
//	@Override
//	int maxAllowedCount() {
//		return 32;
//	}
//
//	@Override
//	int minAllowedCount() {
//		return 0;
//	}
//
//	@Override
//	int readBlock(ByteBuffer buffer, int index) {
//		ids[index] = buffer.getInt();
//		return 0;
//	}
//
//	@Override
//	int writeBlock(ByteBuffer buffer, int index) {
//		buffer.putInt(ids[index]);
//		return 0;
//	}
//}
//
//class VarS extends Variable {
//	String s;
//
//	public VarS() {
//		s = "";
//	}
//
//	@Override
//	int read(ByteBuffer buffer) {
//		s = "";
//		byte c = buffer.get();
//		while (c != '\0') {
//			s = s.concat(String.valueOf((char) c));
//			c = buffer.get();
//		}
//		s.concat(String.valueOf('\0'));
//		return 0;
//	}
//
//	@Override
//	int write(ByteBuffer buffer) {
//		for (int i = 0; i < s.length(); i++)
//			buffer.put((byte) s.charAt(i));
//		buffer.put((byte) '\0');
//		return 0;
//	}
//}
//
//// class CallServicesRequest extends CallServices {
//// int flags;
//// byte turnSlotIndexAtUser, stunSlotIndexAtUser;
//// int turnTokenAtUser, stunTokenAtUser;
////
////
//// @Override
//// int read(ByteBuffer buffer) {
//// // no need to be read by user.
//// return 0;
//// }
////
//// @Override
//// int write(ByteBuffer buffer) {
//// buffer.putInt(flags);
//// if ((flags & TURN_SERVICE) != 0) {
//// buffer.put(turnSlotIndexAtUser);
//// buffer.putInt(turnTokenAtUser);
//// }
//// if ((flags & STUN_SERVICE) != 0) {
//// buffer.put(stunSlotIndexAtUser);
//// buffer.putInt(stunTokenAtUser);
//// }
//// return 0;
//// }
//// }
//

//
//class VarCallServicesProvided extends Variable {
//	final static int TURN_SERVICE = 1 << 0;
//	final static int STUN_SERVICE = 1 << 1;
//	int flags;
//	long turnSlotId, stunSlotId;
//	int turnToken, stunToken;
//	byte turnEncryptionKey[] = new byte[32],
//			stunEncryptionKey[] = new byte[32];
//	VarIPAddress turnAddr = new VarIPAddress(), stunAddr = new VarIPAddress();
//	short turnPort, stunPort;
//
//	@Override
//	int read(ByteBuffer buffer) {
//		flags = buffer.getInt();
//
//		if ((flags & TURN_SERVICE) != 0) {
//			if (turnAddr.read(buffer) != 0)
//				return -1;
//			turnPort = buffer.getShort();
//			turnSlotId = buffer.getLong();
//			turnToken = buffer.getInt();
//			buffer.get(turnEncryptionKey);
//		}
//		if ((flags & STUN_SERVICE) != 0) {
//			if (stunAddr.read(buffer) != 0)
//				return -1;
//			stunPort = buffer.getShort();
//			stunSlotId = buffer.getLong();
//			stunToken = buffer.getInt();
//			buffer.get(stunEncryptionKey);
//		}
//
//		return 0;
//	}
//
//	@Override
//	int write(ByteBuffer buffer) {
//		// no need to be written by user.
//		return -1;
//	}
//}
//
//class VarCallUsersInfo extends MultiVariable {
//	int userIds[];
//	byte pairSpecificKeys[][];
//
//	VarCallUsersInfo() {
//		userIds = new int[32];
//		pairSpecificKeys = new byte[32][32];
//	}
//
//	int readBlock(ByteBuffer buffer, int index) {
//		userIds[index] = buffer.getInt();
//		buffer.get(pairSpecificKeys[index]);
//		return 0;
//	}
//
//	int writeBlock(ByteBuffer buffer, int index) {
//		return -1;
//	}
//
//	int minAllowedCount() {
//		return 0;
//	}
//
//	int maxAllowedCount() {
//		return 32;
//	}
//};
//
//class VarNewTurnUserIndicesInCall extends MultiVariable {
//	int userIndices[];
//
//	VarNewTurnUserIndicesInCall() {
//		userIndices = new int[32];
//	}
//
//	int readBlock(ByteBuffer buffer, int index) {
//		userIndices[index] = buffer.getShort();
//		return 0;
//	}
//
//	int writeBlock(ByteBuffer buffer, int index) {
//		return -1;
//	}
//
//	int minAllowedCount() {
//		return 0;
//	}
//
//	int maxAllowedCount() {
//		return 32;
//	}
//};