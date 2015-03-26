import java.net.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.io.*;

public class Server {
	static final int TIMEOUT = 900;
	static int numberOfServers = 0;
	static int numberOfBooks = 0;
	static List<Host> servers = new ArrayList<Host>();
	static List<String> commands = new ArrayList<String>();
	String ipa;
	int port;
	Library lib;
	final int ID;
	int K_MESSAGES;
	int SLEEP_TIME;
	AtomicInteger clientMessagesReceived = new AtomicInteger(0);
	Lamport lm;
	boolean timeToSleep = false;
	ServerSocket listener = null;

	Server(int numBooks, int id) {
		this.lib = new Library(numberOfBooks);
		this.ID = id;

		Host host = servers.get(id-1);
		ipa = host.name;
		port = host.port;
		lm = new Lamport(servers, ID);

		for (String c: commands) {
			StringTokenizer st = new StringTokenizer(c);
			st.nextToken();		// discard "crash"
			K_MESSAGES = Integer.parseInt(st.nextToken());
			SLEEP_TIME = Integer.parseInt(st.nextToken());
		}

		// Set to max value if there is no command for this server
		if (K_MESSAGES == 0) K_MESSAGES = Integer.MAX_VALUE;

		// start the listener
		try {
			listener = new ServerSocket(port);
		} catch (IOException e1) {
			e1.printStackTrace();
			System.exit(1);
		}

		// Get Library from any server that is up
		Thread t = new Rebooter(this);
		t.start();
	}

	public static void main(String[] args) throws Exception {

		Socket s;
		try {
			// parse arguments and read input file
			int serverID = readInputFile(new Scanner(System.in));

			Server myServer = new Server(numberOfBooks, serverID);

			try {
				// wait for requests and start a worker thread when accepted
				while (true) {
					if (myServer.timeToSleep) {		// sleep then reboot
						myServer.listener.close();
						Thread.sleep(myServer.SLEEP_TIME);
						myServer.listener = new ServerSocket(myServer.port);
						Thread t = new Rebooter(myServer);
						t.start();
					}

					s = myServer.listener.accept();

					// Start worker to handle the request
					Thread t = new TCPServerThread(s,myServer);
					t.start();
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				myServer.listener.close();
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
		}
	}

	public void registerClientMessage() {
		//		System.out.println("Register client request");
		//		clientMessagesReceived++;
	}

	private static int readInputFile(Scanner scan) throws IOException {
		String[] first = scan.nextLine().split(" ");
		int id = Integer.parseInt(first[0]);
		numberOfServers = Integer.parseInt(first[1]);
		numberOfBooks = Integer.parseInt(first[2]);

		//Get other server information
		for(int i = 0; i < numberOfServers; i++){
			//Add server to list
			String[] server = scan.nextLine().split(":");
			servers.add( new Host(server[0], Integer.parseInt(server[1])));
		}

		//Save crash commands
		while(scan.hasNextLine()){
			commands.add(scan.nextLine());
		}

		return id;
	}

}


