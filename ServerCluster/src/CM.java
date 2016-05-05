import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public class CM {
	final static int CLIENT_BINARY = 1;
	final static int LISTNING_INTERNAL = 2;
	final static int MP = 3;
	final static int LB = 4;
	final static int WORKER = 5;
	final static int TIMER = 6;
	final static int LISTNING_EXTERNAL_BINARY = 7;
	final static int LISTNING_EXTERNAL_TEXT = 8;
	final static int CLIENT_TEXT = 9;
	final static int UNKNOWN = 10;

	final static int MODE_LEN = 1;
	final static int MODE_PACK = 2;

	int type;
	SocketChannel socketChannel;
	ServerSocketChannel serverSocketChannel;
	byte[] internalBuffer = new byte[10000];
	int bufferOffset;
	int packLen;
	int mode = CM.MODE_LEN;
	WorkerHandle wh;

	CM() {
		type = UNKNOWN;
	}
}
