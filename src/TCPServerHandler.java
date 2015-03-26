import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;
import java.util.StringTokenizer;


public class TCPServerHandler extends Thread {
	Server myServer;
	Socket theClient;
	String command;
	String client, book, commandType;
	int sleepTime;

	public TCPServerHandler(Socket s, Server server, String command){
		this.theClient = s;
		this.myServer = server;
		this.command = command;
	}

	// Updates this server's copy of the Library to reflect changes made by
	// other servers.
	// Updates the server's copy of the Library when another server has made a change
	public void updateLib(String command) {
		Scanner sc = new Scanner(command);
		try {
			sc.next();
			String client = sc.next();
			String book = sc.next();
			String action = sc.next();

			if (action.equals("reserve")) {
				myServer.lib.reserveBook(client,book);
			} else if (commandType.equals("return")) {
				myServer.lib.returnBook(client,book);
			}
		} catch (Exception e) {

		} finally {
			sc.close();
		}
	}

	public void sendLib(String command) {
		Scanner sc = new Scanner(command);
		try {
			sc.next();
			int requestingId = sc.nextInt();

			ObjectOutputStream out = new ObjectOutputStream(theClient.getOutputStream());
			out.writeObject(myServer.lib.clients);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			sc.close();
		}
	}

	public void run(){
		StringTokenizer st = null;
		PrintWriter pout = null;

		try{
			// get the request that was sent
			pout = new PrintWriter(theClient.getOutputStream());
			st = new StringTokenizer(command);
			client = st.nextToken();

			if (client.charAt(0) == 'u') {	// a server has just updated their Lib
				updateLib(command);
			} else if (client.charAt(0) == 's') {
				sendLib(command);
			} else {						// handle a "request", "ack", or "release"
				myServer.lm.handleMsg(command,myServer);
			}
			theClient.close();
			return;
		} catch(Exception e){ e.printStackTrace(); }
	}
}
