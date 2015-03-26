import java.net.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.io.*;

public class Server {
	static final int TIMEOUT = 100;
	static int numberOfServers = 0;
	static int numberOfBooks = 0;
	static List<Host> servers = new ArrayList<Host>();
	static List<CrashCommand> crashes = new ArrayList<CrashCommand>();
	static int myPort;
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
	boolean continueToCrash = false;
	boolean alive = true;

	Server(int numBooks, int id) {
		this.lib = new Library(numberOfBooks);
		this.ID = id;

		port = myPort;
		lm = new Lamport(servers, ID);

		if(crashes.isEmpty() == false){
			continueToCrash = true;
			K_MESSAGES = crashes.get(0).commands;
			SLEEP_TIME = crashes.get(0).sleepTime;
		}

		// start the listener
		try {
			System.out.println("server " + ID + " on port " + port);
			listener = new ServerSocket(port);
		} catch (IOException e1) {
			e1.printStackTrace();
			System.exit(1);
		}
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
						System.out.println("Server " + myServer.ID + " crashing");
						myServer.timeToSleep  = false;
						myServer.listener.close();
						Thread.sleep(myServer.SLEEP_TIME);

						crashes.remove(0);
						if(crashes.isEmpty() == false){
							myServer.K_MESSAGES = crashes.get(0).commands;
							myServer.SLEEP_TIME = crashes.get(0).sleepTime;
						}
						else{
							myServer.continueToCrash = false;
						}

						myServer.listener = new ServerSocket(myServer.port);
						Thread t = new Rebooter(myServer);
						t.start();
					}

					if(myServer.alive == true){
						s = myServer.listener.accept();
						// get the request that was sent
			            String command = new Scanner(s.getInputStream()).nextLine();
			            
						if(command.charAt(0) != 'c'){
							// Start worker to handle the server request
							Thread t = new TCPServerHandler(s, myServer, command);
							t.start();
						}
						else{
							// Start worker to handle the client request
							Thread t = new TCPClientHandler(s, myServer, command);
							t.start();

							if(myServer.continueToCrash == true){
								myServer.K_MESSAGES--;
								// After completing a client request, tell the main thread to 
								// sleep if it has fulfilled k requests
								if((myServer.K_MESSAGES == 0) && (myServer.continueToCrash == true)) {
									myServer.alive = false;
									myServer.timeToSleep = true;
								}
							}
						}
					}
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
			if(i == (id-1))
				myPort = Integer.parseInt(server[1]);
			servers.add(new Host(server[0], 100 + i*10));
		}

		//Save crash commands
		while(scan.hasNextLine()){
			String[] cmd = scan.nextLine().split(" ");
			crashes.add(new CrashCommand(Integer.parseInt(cmd[1]), Integer.parseInt(cmd[2])));
		}

		return id;
	}

}


