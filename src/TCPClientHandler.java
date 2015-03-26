import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;
import java.util.StringTokenizer;


public class TCPClientHandler extends Thread {
	Server myServer;
	Socket theClient;
	String command;
	String client, book, commandType;
	int sleepTime;

	public TCPClientHandler(Socket s, Server server, String command) {
		this.theClient = s;
		this.myServer = server;
		this.command = command;
	}

	// Fulfills the client's request to "reserve"/"return" a book, or sleep for 
	// a given amount of time. If the thread needs to modify the resource,
	// it must "request" the CS, modify the resource, send a message to 
	// all other servers to "update" their copy of the resource, then "release"
	// the CS.
	private String fulfillClientRequest(String command) throws InterruptedException, IOException {
		boolean success;
		String tag = null;
		String ret = null;
		//		myServer.registerClientMessage();

		if (commandType.equals("reserve")) {		// request CS and reserve book
			tag = "reserve";
			success = myServer.lib.reserveBook(client,book);

		} else if (commandType.equals("return")) {	// request CS and return book
			tag = "return";
			success = myServer.lib.returnBook(client,book);
		} else {									// sleep
			Thread.sleep(sleepTime);
			String response = "Slept";
			return response;
		}

		if (success) {							// update all servers if successful
			myServer.lm.updateAll(tag,client,book);
			if(tag.equals("reserve"))
				ret = client + " " + book;
			else
				ret = client + " free " + book;
		} else {
			ret = "fail " + client + " " + book;
		}

		// Release the CS and return the success/failure message
		return ret;
	}

	// Parses the request from the server and operates accordingly.
	// This worker thread will "reserve" or "return" a book for a client (first
	// "request"-ing the CS), accept a server's "request" for CS and "ack" them back,
	// "update" the local copy of Library if another server has modified its own copy,
	// or handle a "release" of the CS from another server.
	public void run() {
		Scanner sc = null;
		StringTokenizer st = null;
		PrintWriter pout = null;

		try {
			// get the request that was sent
			sc = new Scanner(theClient.getInputStream());
			pout = new PrintWriter(theClient.getOutputStream());
			st = new StringTokenizer(command);
			client = st.nextToken();

			System.out.println("Servicing "+ command);

			book = st.nextToken();

			if (book.charAt(0) == 'b') {	// client request of the form 'ci bi command'
				commandType = st.nextToken();

			} else {						// client sleep request of the form 'ci time'
				commandType = "sleep";
				sleepTime = Integer.parseInt(book);
			}

			myServer.lm.requestCS();
			// fulfill client request and respond to them
			String ret = fulfillClientRequest(command);
			pout.println(ret);
			pout.flush();
			myServer.lm.releaseCS();
			theClient.close();

		} catch (Exception e) {
		} finally {
			sc.close();
		}
	}
}
