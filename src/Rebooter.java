import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

class Rebooter extends Thread {
		Server myServer;
		
		Rebooter(Server s) {
			myServer = s;
		}
		
		public void run() {
			reboot();
		}
		
		private void reboot() {
			// TODO Auto-generated method stub
			myServer.timeToSleep = false;
			syncResource();
			myServer.clientMessagesReceived.set(0);
		}

		private void syncResource() {
			// TODO Auto-generated method stub
			try {
				myServer.lm.requestCS();
				Object lib = myServer.lm.getLibrary();
				if (lib != null) {
					myServer.lib.clients = (int[]) lib;
				}
				myServer.lm.releaseCS();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}