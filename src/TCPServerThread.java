import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;


public class TCPServerThread extends Thread {
	Server myServer;
	Socket theClient;
	String client, book, commandType;
	int sleepTime;
	
	public TCPServerThread(Socket s, Server server) {
		this.theClient = s;
		this.myServer = server;
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
			myServer.lm.requestCS();
			success = myServer.lib.reserveBook(client,book);
			
		} else if (commandType.equals("return")) {	// request CS and return book
			tag = "return";
			myServer.lm.requestCS();
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
		myServer.lm.releaseCS();
		return ret;
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
	
	// Parses the request from the server and operates accordingly.
	// This worker thread will "reserve" or "return" a book for a client (first
	// "request"-ing the CS), accept a server's "request" for CS and "ack" them back,
	// "update" the local copy of Library if another server has modified its own copy,
	// or handle a "release" of the CS from another server.
	public void run() {
		Scanner sc = null;
		Scanner st = null;
		PrintWriter pout = null;
		
		try {
			// get the request that was sent
			sc = new Scanner(theClient.getInputStream());
            pout = new PrintWriter(theClient.getOutputStream());
            String command = sc.nextLine();
            st = new Scanner(command);
            client = st.next();
            
            if (client.charAt(0) != 'c') {		// request from another server, not a client
            	if (client.charAt(0) == 'u') {	// a server has just updated their Lib
            		updateLib(command);
            	} else if (client.charAt(0) == 's') {
            		sendLib(command);
            	} else {						// handle a "request", "ack", or "release"
            		myServer.lm.handleMsg(command,myServer);
            	}
            	return;
            	
            } else if (myServer.clientMessagesReceived.get() < myServer.K_MESSAGES) {		//fulfill request if its not time to reboot
            	myServer.clientMessagesReceived.getAndIncrement();
            	book = st.next();
	            
	            if (book.charAt(0) == 'b') {	// client request of the form 'ci bi command'
	            	commandType = st.next();
	            	
	            } else {						// client sleep request of the form 'ci time'
	            	commandType = "sleep";
	            	sleepTime = Integer.parseInt(book);
	            }
	            
	            // fulfill client request and respond to them
	            String ret = fulfillClientRequest(command);
	            pout.println(ret);
	            pout.flush();
	            
	            // After completing a client request, tell the main thread to 
	            // sleep if it has fulfilled k requests
	            if((myServer.clientMessagesReceived.get() >= myServer.K_MESSAGES)) {
	    			myServer.timeToSleep = true;
	    		}
	            
            } else {	// dont fulfill client request if its time to reboot
            }
            theClient.close();
            
		} catch (Exception e) {
		} finally {
			sc.close();
		}
	}
}
