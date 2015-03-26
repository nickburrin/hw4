import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.PrintStream;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class Lamport {
    DirectClock v;
    int[] q; 		// request queue
    Socket server;
    Scanner din;
    PrintStream pout;
    int numberOfServers, id;
    List<Host> servers;
    
    public Lamport(List<Host> servers, int id) {
    	this.id = id-1;
    	this.numberOfServers = servers.size();
    	this.servers = servers;
        v = new DirectClock(numberOfServers, this.id);
        q = new int[numberOfServers];
        for (int j = 0; j < numberOfServers; j++)
            q[j] = Integer.MAX_VALUE;
    }
    
    // Requests CS by sending a "request" to all other servers and waits for
    // an "ack" from each server and for its request to be at the head of 
    // queue.
    public synchronized void requestCS() throws IOException {
        v.tick();
        q[id] = v.getValue(id);
        broadcastMsg("request", q[id]);
        while (!okayCS())
			try {
				wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
    }
    
    // Gets a copy of the library from an UP server
    public Object getLibrary() {
    	String msg = "sync " + (id+1);
    	for (int i = 0; i < numberOfServers; i++) {
            if (i != id) {
            	Object o = sendSyncMsg(i, msg);
            	if (o != null) {
            		return o;
            	}
            }
        }
    	return null;
    }
    
    
    // Releases the CS by sending a "release" message to all other servers
    public synchronized void releaseCS() throws IOException {
        q[id] = Integer.MAX_VALUE;
        broadcastMsg("release", v.getValue(id));
    }
    
    // Sends a message to all servers 
    public void broadcastMsg(String tag, int msg) throws IOException {
        for (int i = 0; i < numberOfServers; i++) {
            if (i != id) {
            	sendMsg(i, tag, msg);
            }
        }
    }
    
    // Sends an "update" message and the corresponding command to all servers
    public void updateAll(String action, String client, String book) {
    	String msg = "update " + client + " " + book + " " + action;
    	for (int i = 0; i < numberOfServers; i++) {
    	    if (i != id) {
    	    	sendUpdateMsg(i, msg);
    	    }
    	}		
    }
   
    // Sends an "update" to a single server
    public void sendUpdateMsg(int destId, String msg) {
    	Host h = servers.get(destId);
    	try {
    		getSocket(h);
    	} catch (IOException e) {
    		// TODO handle the downed server
    		handleDownServer(destId);
    		return;
    	}
    	
        pout.println(msg);
        pout.flush();
    }
    public void sendMsg(int destId, String tag, int msg) {
        sendMsg(destId, tag, String.valueOf(msg)+" ");
    }
    public void sendMsg(int destId, String tag, String msg) {
    	int offsetId = id + 1;
    	Host h = servers.get(destId);
    	
    	try {
    		getSocket(h);
    	} catch(NoSuchElementException e) {		// Connected to server but then the server closed the connection
			handleDownServer(destId);
    		return;
    	} catch (IOException e) {
    		// TODO handle the downed server
    		handleDownServer(destId);
    		return;
    	}
    	
        pout.println(offsetId + " " + (destId+1) + " " + tag + " " +  msg);
        pout.flush();
    }
    
    public Object sendSyncMsg(int destId, String msg) {
    	Host h = servers.get(destId);
    	Object lib = null;
    	try {
    		getSocket(h);
    		pout.println(msg);
    	    pout.flush();
    	    ObjectInputStream in = new ObjectInputStream(server.getInputStream());
    	    lib = in.readObject();
    	    
    	} catch(NoSuchElementException e) {		// Connected to server but then the server closed the connection
			handleDownServer(destId);
    		return null;
    		
    	} catch (IOException e) {
    		// TODO handle the downed server
    		handleDownServer(destId);
    		return null;
    		
    	} catch (Exception e) { 
    		e.printStackTrace();
    	}
    	
    	return lib;
    }
    
    // Gets a socket for communicating with another server
    public void getSocket(Host h) throws IOException, NoSuchElementException {
    	server = new Socket();   
		server.connect(new InetSocketAddress(h.name,h.port),Server.TIMEOUT);
    	
		din = new Scanner(server.getInputStream());
        pout = new PrintStream(server.getOutputStream());
    }
    
    public void handleDownServer(int downId) {		//spoof an ack with larger ts
    	q[downId] = Integer.MAX_VALUE;
    	v.receiveAction(downId,q[id]+1);
    }
    
    // Determines if this server can modify the resource.
    boolean okayCS() {
        for (int j = 0; j < numberOfServers; j++){
            if (isGreater(q[id], id, q[j], j))
                return false;
            if (isGreater(q[id], id, v.getValue(j), j))
                return false;
        }
        return true;
    }
    boolean isGreater(int entry1, int pid1, int entry2, int pid2) {
        if (entry2 == Integer.MAX_VALUE) return false;
        return ((entry1 > entry2)
                || ((entry1 == entry2) && (pid1 > pid2)));
    }
    
    // Handles a message from another server relating to mutual exclusion. An 
    // incoming message can be: a "request", in which case this server will "ack" 
    // the requesting server; a "release" in which case this server will remove the
    // remote server from the request queue; or an "ack" in which case this server will
    // notify a waiting worker process.
    public synchronized void handleMsg(String message, Server myServer) {
    	Scanner scanner = new Scanner(message);
    	try {
			int srcId = scanner.nextInt();
			int destId = scanner.nextInt();
			String tag = scanner.next();
			int timeStamp = scanner.nextInt();
			srcId--;
			v.receiveAction(srcId, timeStamp);
			
			if (tag.equals("request")) {
			    q[srcId] = timeStamp;
			    sendMsg(srcId, "ack", v.getValue(id));
			} else if (tag.equals("release"))
			    q[srcId] = Integer.MAX_VALUE;		// reset srcId's last request time
			notify(); // okayCS() may be true now
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			scanner.close();
		}
    }
}