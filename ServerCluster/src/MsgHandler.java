abstract class MsgHandler<T> {
	T o;
	int permittedSenders;

	MsgHandler(int permittedSenders, T o) {
		this.permittedSenders = permittedSenders;
		this.o = o;
	}

	int handle(Msg m, int senderId) throws Exception {
//		if ((permittedSenders & senderId) == 0)
//			return -1;
		return handle(m, senderId, o);
	}

	abstract int handle(Msg m, int senderId, T o) throws Exception;
}