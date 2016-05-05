import java.io.BufferedReader;
import java.io.FileReader;
import java.net.InetSocketAddress;
import java.util.Stack;
import java.util.StringTokenizer;

public class Config {
	final static String DEFAULT_CONFIG_FILE = "config.txt";

	final static int UNSPECIFIED_PORT = -1;
	final static int DEFAULT_MAINPROCESS_PORT = 2100;

	final static int MAX_SENIOR_NODES = 10;

	final static String MAIN_PROCESS_KEYWORD = "Main_process";
	final static String LOAD_BALANCER_KEYWORD = "Load_balancer";
	final static String WORKER_KEYWORD = "Worker";
	final static String PUBLIC_ADDRESS_KEYWORD = "Public_address";
	final static String PRIVATE_ADDRESS_KEYWORD = "Private_address";
	final static String OWN_SERVER_ID = "Own_Server_ID";
	final static String PUBLIC_ADDRESS_AND_PORT_KEYWORD = "Public_address_and_port";
	final static String PRIVATE_ADDRESS_AND_PORT_KEYWORD = "Private_address_and_port";
	final static String SERVER_ID_ALLOCATION_RANGE_KEYWORD = "Server_ID_allocation_range";
	final static String PORT_KEYWORD = "Port";

	final static String COMPONENTS_START_TAG = "<components>";
	final static String COMPONENTS_END_TAG = "</components>";
	final static String OWN_START_TAG = "<own>";
	final static String OWN_END_TAG = "</own>";
	final static String NODE_START_TAG = "<node>";
	final static String NODE_END_TAG = "</node>";
	final static String SENIORS_START_TAG = "<seniors>";
	final static String SENIORS_END_TAG = "</seniors>";

	final static int ERROR_MODE = -1;
	final static int START = 0;
	final static int OWN_BLOCK = 1;
	final static int COMPONENTS_BLOCK = 2;
	final static int NODE_BLOCK = 3;
	final static int SENIORS_BLOCK = 4;

	final static int MAINPROCESS_TYPE = 1;
	final static int LOADBALANCER_TYPE = 2;
	final static int WORKER_TYPE = 3;

	final static int MAX_ADDR_LEN = 60;

	// ///////////////////////////////
	int ownServerId;
	int serverIdAllocationMin, serverIdAllocationMax;
	cnfg_MainProcess mainProcess = new cnfg_MainProcess();
	cnfg_LoadBalancer loadBalancer = new cnfg_LoadBalancer();
	cnfg_Worker worker = new cnfg_Worker();
	int minServerID, maxServerID;
	int seniorNodeCount;
	cnfg_ServerNode seniorNodes[] = new cnfg_ServerNode[Config.MAX_SENIOR_NODES];
	String publicAddress = new String(), privateAddress = new String();

	// ////////////////////////////////////////////////////

	public Config() {
		ownServerId = 0;
		serverIdAllocationMax = -1;
		serverIdAllocationMin = -1;
		seniorNodeCount = 0;
		minServerID = -1;
		maxServerID = -1;

	}

	int reset() {
		seniorNodeCount = 0;
		minServerID = -1;
		maxServerID = -1;

		publicAddress = new String();
		privateAddress = new String();

		return 0;
	}

	cnfg_MainProcess getMainProcess() {
		return mainProcess;
	}

	cnfg_LoadBalancer getLoadBalancer() {
		return loadBalancer;
	}

	cnfg_Worker getWorker() {
		return worker;
	}

	int getSeniorNodeCount() {
		return seniorNodeCount;
	}

	cnfg_ServerNode[] getSeniorNodes() {
		return seniorNodes;
	}

	int getOwnServerId() {
		return ownServerId;
	}

	int getServerIdAllocationMin() {
		return serverIdAllocationMin;
	}

	int getServerIdAllocationMax() {
		return serverIdAllocationMax;
	}

	// ///////////////////////
	int readConfig(int configId) {
		int mode = START;
		Stack<Integer> prevModes = new Stack<>();
		int linesRead = 0;
		int currentLineKeywords = 0;
		String token = "";
		try {
			BufferedReader configFile = new BufferedReader(new FileReader(
					configId + DEFAULT_CONFIG_FILE));

			StringTokenizer st = new StringTokenizer("");
			while (true) {
				if (mode != ERROR_MODE) {
					String line = configFile.readLine();
					if (line == null) {
						if (mode != START)
							throw new Exception();
						break;
					}
					linesRead++;
					line.trim();
					if (line.startsWith("#") || line.isEmpty())
						continue;// its a comment or an empty line;
					st = new StringTokenizer(line);
					currentLineKeywords = 0;
					token = st.nextToken();
					currentLineKeywords++;
				}
				switch (mode) {
				case START:
					// cout << "\nSTART\n";
					if (token.compareTo(Config.OWN_START_TAG) == 0) {
						prevModes.push(mode);
						mode = OWN_BLOCK;
					} else if (token.compareTo(Config.SENIORS_START_TAG) == 0) {
						prevModes.push(mode);
						mode = SENIORS_BLOCK;
					} else
						mode = ERROR_MODE;
					break;
				case OWN_BLOCK:
					// cout << "\nOWN_BLOCK\n";
					if (token.compareTo(Config.OWN_SERVER_ID) == 0) {
						token = st.nextToken();
						currentLineKeywords++;
						ownServerId = Integer.parseInt(token);
					} else if (token.compareTo(Config.COMPONENTS_START_TAG) == 0) {
						prevModes.push(mode);
						mode = COMPONENTS_BLOCK;
					} else if (token
							.compareTo(Config.SERVER_ID_ALLOCATION_RANGE_KEYWORD) == 0) {
						int min = -1, max = -1;
						token = st.nextToken();
						currentLineKeywords++;
						min = Integer.parseInt(token);
						token = st.nextToken();
						currentLineKeywords++;
						max = Integer.parseInt(token);
						if (min < 1 || max < 1 || max < min)
							mode = ERROR_MODE;
						else {
							minServerID = min;
							maxServerID = max;
						}
					} else if (token.compareTo(Config.PUBLIC_ADDRESS_KEYWORD) == 0) {
						token = st.nextToken();
						currentLineKeywords++;
						publicAddress = token;
					} else if (token.compareTo(Config.PRIVATE_ADDRESS_KEYWORD) == 0) {
						token = st.nextToken();
						currentLineKeywords++;
						privateAddress = token;
					} else if (token.compareTo(Config.OWN_END_TAG) == 0) {
						mode = prevModes.pop();
					} else
						mode = ERROR_MODE;
					break;
				case SENIORS_BLOCK:
					// cout << "\nSENIORS_BLOCK\n";
					if (token.compareTo(Config.NODE_START_TAG) == 0) {
						prevModes.push(mode);
						mode = NODE_BLOCK;
						seniorNodes[seniorNodeCount] = new cnfg_ServerNode();
					} else if (token.compareTo(Config.SENIORS_END_TAG) == 0) {
						mode = prevModes.pop();
					} else
						mode = ERROR_MODE;
					break;
				case NODE_BLOCK:
					// cout << "\nNODE_BLOCK\n";
					if (token.compareTo(Config.PUBLIC_ADDRESS_AND_PORT_KEYWORD) == 0) {
						token = st.nextToken();
						currentLineKeywords++;
						String addr = token;
						int port = 0;
						token = st.nextToken();
						currentLineKeywords++;
						port = Integer.parseInt(token);
						if (port != 0) {
							seniorNodes[seniorNodeCount].setPublicAddress(addr,
									port);
						} else
							mode = ERROR_MODE;
					} else if (token
							.compareTo(Config.PRIVATE_ADDRESS_AND_PORT_KEYWORD) == 0) {
						token = st.nextToken();
						currentLineKeywords++;
						String addr = token;
						int port = 0;
						token = st.nextToken();
						currentLineKeywords++;
						port = Integer.parseInt(token);
						if (port != 0) {
							seniorNodes[seniorNodeCount].setPrivateAddress(
									addr, port);
						} else
							mode = ERROR_MODE;
					} else if (token.compareTo(Config.NODE_END_TAG) == 0) {
						seniorNodeCount++;
						mode = prevModes.pop();
					} else
						mode = ERROR_MODE;
					break;
				case COMPONENTS_BLOCK:
					// cout << "\nCOMPONENT_BLOCK\n";
					if (token.compareTo(Config.MAIN_PROCESS_KEYWORD) == 0) {
						token = st.nextToken();
						currentLineKeywords++;
						if (token.compareTo(Config.PORT_KEYWORD) == 0) {
							int port = 0;
							token = st.nextToken();
							currentLineKeywords++;
							port = Integer.parseInt(token);
							if (port != 0) {
								mainProcess.setPort(port);
							} else
								mode = ERROR_MODE;
						} else
							mode = ERROR_MODE;
					} else if (token.compareTo(Config.LOAD_BALANCER_KEYWORD) == 0) {
						token = st.nextToken();
						currentLineKeywords++;
						if (token.compareTo(Config.PORT_KEYWORD) == 0) {
							int port = 0;
							token = st.nextToken();
							currentLineKeywords++;
							port = Integer.parseInt(token);
							if (port != 0) {
								loadBalancer.setPort(port);
							} else
								mode = ERROR_MODE;
						} else
							mode = ERROR_MODE;
					} else if (token.compareTo(Config.WORKER_KEYWORD) == 0) {
						token = st.nextToken();
						currentLineKeywords++;
						if (token.compareTo(Config.PORT_KEYWORD) == 0) {
							int port = 0;
							token = st.nextToken();
							currentLineKeywords++;
							port = Integer.parseInt(token);
							if (port != 0) {
								worker.setPort(port);
							} else
								mode = ERROR_MODE;
						} else
							mode = ERROR_MODE;
					} else if (token.compareTo(Config.COMPONENTS_END_TAG) == 0) {
						mode = prevModes.pop();
					} else
						mode = ERROR_MODE;
					break;
				default:
					throw new Exception();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Parsing Error at line : " + linesRead
					+ "; at token :  " + token);
			return -1;
		}
		return 0;
	}
	// ///////////////////////

}

class cnfg_Component {
	int port;

	cnfg_Component() {
		port = Config.UNSPECIFIED_PORT;
	}

	void setPort(int port) {
		this.port = port;
	}

	int getPort() {
		return port;
	}
}

class cnfg_MainProcess extends cnfg_Component {
	cnfg_MainProcess() {
	}
}

class cnfg_LoadBalancer extends cnfg_Component {
	cnfg_LoadBalancer() {
	}
}

class cnfg_Worker extends cnfg_Component {
	cnfg_Worker() {
	}
}

class cnfg_ServerNode {
	InetSocketAddress publicAddress, privateAddress;

	cnfg_ServerNode() {
		publicAddress = new InetSocketAddress(0);
		privateAddress = new InetSocketAddress(0);
	}

	void setPublicAddress(String addr, int port) {
		publicAddress = new InetSocketAddress(addr, port);
	}

	void setPrivateAddress(String addr, int port) {
		privateAddress = new InetSocketAddress(addr, port);
	}

	InetSocketAddress getPublicAddress() {
		return publicAddress;
	}

	int getPublicAddressPort() {
		return publicAddress.getPort();
	}

	InetSocketAddress getPrivateAddress() {
		return privateAddress;
	}

	int getPrivateAddressPort() {
		return privateAddress.getPort();
	}
};
