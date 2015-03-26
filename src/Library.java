
public class Library {
	final int numBooks;
	int[] clients;
	final int FREE = 0;
	
	public Library(int numBooks) {
		this.numBooks = numBooks;
		clients = new int[numBooks];
	}
	
	public Library(int[] clients) {
		numBooks = clients.length;
		this.clients = clients;
	}
	
	public synchronized boolean reserveBook(int ci, int num) { 
		if (num >= numBooks || num < 0 || clients[num] != FREE) {
			return false;
		}
		clients[num] = ci;
		return true;
	}
	
	public synchronized boolean returnBook(int ci, int num) {
		if (num < numBooks && num >= 0 && clients[num] == ci) {
			clients[num] = FREE;
			return true;
		}
		return false;
	}
	
	public boolean reserveBook(String ci, String num) {
		return reserveBook(extractNumber(ci),extractNumber(num));
	}
	
	public boolean returnBook(String ci, String num) {
		return returnBook(extractNumber(ci),extractNumber(num));
	}
	
	private int extractNumber(String s) {
		return Character.getNumericValue(s.charAt(1));
	}
	
	public String printLibrary() {
		StringBuilder sb = new StringBuilder();
		sb.append("\nBookNo\tClientId\n");
		
		String id;
		int i = 0;
		for (int clientId : clients) {
			if (clientId == 0) 	id = "-";
			else 				id = Integer.toString(clientId);
			sb.append(i++ + "\t" + id + "\n");
		}
		return sb.toString();
	}
}
