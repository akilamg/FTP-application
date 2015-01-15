import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;
import java.lang.Thread;

public class Listener implements Runnable{
	
	// Socket variable
	Socket socket;

	// Implement the socket
	public Listener (Socket socket){
		this.socket = socket;
	}
	
	// Rin the thread
	public void run() {		
		try
		{
			// Variables to keep track of the acks
			int ack = 0;
			String ack_s;
			//Make a reader to read information from the Server
			BufferedReader socket_reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

			// While the ack number is less than the number of packets, keep reading
			while (ack < Client.numberPackets)
			{
				// Read the ack number from the server in string format
				ack_s = socket_reader.readLine();
				// Change the string to an integer and print it out
				ack = Integer.parseInt(ack_s);
				System.out.println("Recieved ack for packet: " + ack);
				// Change the value of the lastAck variable
				Client.setAckNum(ack);
			}

			//Close the socket and reader
			socket_reader.close();
			socket.close();

		}catch(Exception e)
		{
			System.out.println(e.getMessage());
			e.printStackTrace();
		}	
	}
}