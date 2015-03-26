import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.io.*;

public class Client {
	static final int TIMEOUT = 100;
	Scanner scan;
	Scanner din;
    PrintStream pout;
    Socket server;
	int numOfServers;
	List<Host> servers = new ArrayList<Host>();
	static String name;
	
	Client() {
		try {
			this.scan = new Scanner(System.in);
			this.readInputFile(scan);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
    
    public static void main(String[] args) throws IOException {
    	Client myClient = new Client();
        
        try {
        	String msg = "";
        	while(myClient.scan.hasNextLine()){
        		msg = myClient.scan.nextLine();
        		if(msg.contains("sleep")){
        			String[] touba = msg.split(" ");
        			Thread.sleep(Integer.parseInt(touba[1]));
        		}
        		else{
        			myClient.sendCommand(name + " " + msg);
        		}
        	}
        	System.exit(0);
        } catch (Exception e) { e.printStackTrace(); }
    }
    
    public void getSocket(String ipa, int port) throws IOException {
        server = new Socket(ipa, port);
        din = new Scanner(server.getInputStream());
        pout = new PrintStream(server.getOutputStream());
    }
    
    public void sendCommand(String command) throws IOException {
    	Socket serversock = null;
    	
    	// Try each host in a round-robin fashion. Go to next after timeout
    	int i = 0;
    	Host h = servers.get(i);
    	
    	while (true) {
    		try {
				serversock = new Socket();   
				serversock.connect(new InetSocketAddress(h.name,h.port), TIMEOUT);
				
				din = new Scanner(serversock.getInputStream());
				pout = new PrintStream(serversock.getOutputStream());
				// send command to server
				System.out.println(command + " to server " + h.port);
		    	pout.println(command);
		    	pout.flush();
		    	
		    	// get and print the return value
		    	String retValue = din.nextLine();
		    	if (!retValue.equals("Slept")) {
		    		System.out.println(retValue);
		    	}
		    	
		    	serversock.close();
				break;
			} catch(NoSuchElementException e) {		// Connected to server but then the server drops the message
				i = (i+1) % numOfServers;
				h = servers.get(i);
				continue;
			} catch(ConnectException e) {			// The server is down
				i = (i+1) % numOfServers;
				h = servers.get(i);
				continue;
			} catch(Exception e){
				e.printStackTrace();
			}
    	}
    }
    
    private void readInputFile(Scanner scan) throws IOException {
    	//First line gives client name and numServers
    			String line = scan.nextLine();
    			StringTokenizer st = new StringTokenizer(line);
    			name = st.nextToken();
    			numOfServers = Integer.parseInt(st.nextToken());

    			String[] server;
    			//next N lines give ports of servers (first server = "nearest")
    			for(int i = 0; i < numOfServers; i++){
    				server = scan.nextLine().split(":");
    				servers.add(new Host(server[0], Integer.parseInt(server[1])));
    				System.out.println("Adding server " + server[0] + ":" + server[1]);
    			}
	}
}
