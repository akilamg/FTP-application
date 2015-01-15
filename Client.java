import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.PriorityQueue;
import java.util.Collections;
import java.io.FileInputStream;
import java.io.File;
import java.nio.ByteBuffer;

// The network is represented by a graph, that contains nodes and edges
// The Node class for creating Node objects
class Node implements Comparable<Node>
{
	public final int name;
	public Edge[] neighbors;
	public double minDistance = Double.POSITIVE_INFINITY;
	public Node previous;     // to keep the path
	
	public Node(int argName)
	{
		name = argName;
	}

	public int compareTo(Node other)
	{
		return Double.compare(minDistance, other.minDistance);
	}
}

// The Edge class for creating the edges
class Edge
{
	public final Node target;
	public final double weight;
	public Edge(Node argTarget, double argWeight)
	{
		target = argTarget;
		weight = argWeight;
	}
}

public class Client {

	static String mode;	//Mode of client
	static String host;	//Host to connect to
	static int port;	//Port to connect to
	public static int lastAck = 0; 	//The last ack that was received
	public static int numberPackets = 0;		//The number of packets being transmitted

	// Create the edges from each node
	public static void adjacenyToEdges(double[][] matrix, List<Node> v)
	{
		for(int i = 0; i < matrix.length; i++)
		{
			v.get(i).neighbors = new Edge[matrix.length];
			for(int j = 0; j < matrix.length; j++)
			{
				v.get(i).neighbors[j] =  new Edge(v.get(j), matrix[i][j]);
			}
		}
	}
	
	// Use Dijkstra's algorithm to compute the shortest path from the source node to all other nodes
	public static void computePaths(Node source)
	{
		source.minDistance = 0;
		
		PriorityQueue<Node> NodeQueue = new PriorityQueue<Node>();
		NodeQueue.add(source);
		
		while(!NodeQueue.isEmpty()){
			Node sourceNode = NodeQueue.poll();
			
			for(Edge e : sourceNode.neighbors){
				Node targetNode = e.target;
				double distanceThroughSource = sourceNode.minDistance + e.weight;
				
				if(distanceThroughSource < targetNode.minDistance){
					NodeQueue.remove(targetNode);
					targetNode.minDistance = distanceThroughSource;
					targetNode.previous = sourceNode;
					NodeQueue.add(targetNode);
				}
			}
		}
	}

	// Find the path used to get to each node
	public static List<Integer> getShortestPathTo(Node target)
	{
		List<Integer> path = new ArrayList<Integer>();
        for (Node node = target; node != null; node = node.previous)
            path.add(node.name);
        Collections.reverse(path);
        return path;
	}

	/**
	 * @param args
	 */

	public static void main(String[] args) {
		
		// Set up the client-server connection
		if(args.length<=0)
		{
			mode="client";
			host="localhost";
			port=9876;
		}
		else if(args.length==1)
		{
			mode=args[0];
			host="localhost";
			port=9876;
		}
		else if(args.length==3)
		{
			mode=args[0];
			host=args[1];
			port=Integer.parseInt(args[2]);
		}
		else
		{
			System.out.println("improper number of arguments.");
			return;
		}

		try
		{
			Socket socket = null;
			if(mode.equalsIgnoreCase("client"))
			{
				socket=new Socket(host, port);
			}
			else if(mode.equalsIgnoreCase("server"))
			{
				ServerSocket ss = new ServerSocket(port);
				socket=ss.accept();
			}
			else
			{
				System.out.println("improper type.");
				return;
			}
			System.out.println("Connected to : "+ host+ ":"+socket.getPort());
			
			// The carriage return & line feed
			String CRLF="\r\n";

			// Socket reader
			BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream())); //for reading lines
			// Socket writer
			DataOutputStream writer = new DataOutputStream(socket.getOutputStream());	//for writing lines.
			// Client reader
			BufferedReader client_reader = new BufferedReader(new InputStreamReader(System.in));	//for reading from keyboard

			while(socket!=null && socket.isConnected() && !socket.isClosed()){
				// Get the number of nodes from the server then turn it into an int
				System.out.println("Waiting to recieve the number of nodes...");
				String noNodesStr = reader.readLine();
				System.out.println(noNodesStr);
				int noNodes = Integer.parseInt(noNodesStr);
				System.out.println(noNodes);
				
				// Get the distances for each node from the server
				String distances = reader.readLine(); 

				// Create an adjacency matrix after reading from server
				double[][] matrix = new double[noNodes][noNodes];

				// Use StringTokenizer to store the values read from the server in matrix
				StringTokenizer st = new StringTokenizer(distances, " ");
				
				System.out.println("Adjacency Matrix");
				
				//Put the distances into the matrix and print out the matrix
				for(int i = 0; i < noNodes; i++){
					for(int j = 0; j < noNodes; j++){
						String dist = st.nextToken();
						if(dist.equals("Infinity")){
							matrix[i][j] = Double.POSITIVE_INFINITY;
						}else{
							matrix[i][j] = Double.parseDouble(dist);
						}
						System.out.print(matrix[i][j] + " ");
					}
					System.out.println();
				}
				
				//The nodes are stored in a list, nodeList
				List<Node> nodeList = new ArrayList<Node>();
				for(int i = 0; i < noNodes; i++){
					nodeList.add(new Node(i));
				}

				// Create edges from adjacency matrix
				adjacenyToEdges(matrix, nodeList);
				
				// Finding shortest path for node 0 to all nodes
				Node n = nodeList.get(0);
				computePaths(n);
				System.out.println("\nNode " + n.name);
				List<Integer> path = new ArrayList<Integer>();
				
				// The timeOut variable used when sending the packets 
				int timeOut = 0;
				
				// Get the shortest path to each node and print them out
				for(Node l : nodeList){
					path = getShortestPathTo(l);
					System.out.println("Total time to reach node " + l.name + ": " + l.minDistance + " ms, Path: " + path);
					// Assign timeOut to be the minDistance time * 2 + 200 of the last node l
					timeOut = (int)l.minDistance * 2 + 200;
				}
				
				// Send the path taken to the server
				writer.writeBytes(String.valueOf(path) + CRLF);	
				
				// Read in the file name from the client and open the file and a FileInputStream
				System.out.print("\nEnter the name of the file: ");
				String filename = client_reader.readLine();
				File file_pointer = new File(filename);
				FileInputStream file_reader = new FileInputStream(file_pointer);
				
				// Send the file name to the server
				writer.writeBytes(filename + CRLF);
				
				// Get the size of the file then print it out
				int file_size = (int)file_pointer.length();
				System.out.println("File size: " + file_size);
				// Get the number of packets, then print it out
				numberPackets = file_size/1000 + 1; 
				System.out.println("Number of packets: " + numberPackets);
				
				// Write the number of packets to the server as a string value
				String noPackets = String.valueOf(numberPackets);
				writer.writeBytes(noPackets + CRLF);
				
				// Make a byte array and read in the entire file
				byte[] entire_file = new byte[file_size];
				file_reader.read(entire_file);
				
				// Create a buffer to get the file data, the header int, and then combine them
				byte[] buf = new byte[1004];
				byte[] file = new byte[1000];
				ByteBuffer header = ByteBuffer.allocate(4);
				
				// Create an array for the packet timeOut monitoring
				long [] timer = new long[numberPackets];
				
				// Start a new thread to handle the ack receiving
				Thread thread=new Thread(new Listener(socket));
				thread.start();
				
				// Variable to keep track of how many packets have been sent, what the cwnd is,
				// What the ssthresh is, how many packets have been previously sent,
				// What the of the data read from the file length is, and what the offset for the file is
				int sent = 1;
				int cwnd = 1;
				int ssthresh = numberPackets;
				int previouslySent = 0;
				int length = 0;
				int offset = 0;

				while (lastAck < numberPackets && lastAck >= 0){
					// If you are still able to send more packets, send them
					if((sent - previouslySent) <= cwnd && sent <= numberPackets){
						// Clear the header ByteBuffer to load a new int into
						header.clear();
						// Get the data from the file and put it into the file byte array
						for(int i = 0; (i < 1000) && ((i + offset) < file_size); i++){
							file[i] = entire_file[i + offset];
						}
						// Increment the offset
						offset += 1000;
						
						// Put the packet number into the ByteBuffer
						header.putInt(sent);
						
						// Concatenate the two arrays together into the buff array
						System.arraycopy(header.array(), 0, buf, 0, 4);
						System.arraycopy(file, 0, buf, 4, length);
						
						// Send the packet over the socket connection to the server
						writer.write(buf, 0, 1004);
						
						// Start the timer on the sent packet
						timer[sent-1] = System.currentTimeMillis();
						
						// Print out what packet has been sent and increment the sent variable
						System.out.println("Sent to server: " + sent);
						sent = sent + 1;
					}
					// Get the current time
					long currentTime = System.currentTimeMillis();
					
					// If there has been a timeOut
					if((lastAck < numberPackets) && (currentTime - timer[lastAck] > timeOut) && ((lastAck - previouslySent) != cwnd)){
						System.out.println("Timeout");
						// Reset the value of sent
						sent = lastAck + 1;
						// Make previouslySent the last acked packet
						previouslySent = lastAck;
						// Set the offset to previouslySent * 1000
						offset = previouslySent * 1000;
						// Make the threshold cwnd/2
						ssthresh = cwnd/2;
						System.out.println("ssthresh = " + ssthresh);
						// Set cwnd to 1, and print it out
						cwnd = 1;
						System.out.println("cwnd = " + cwnd);
						// Timeout, resend the packet
						System.out.println("Resending packet to server: " + (lastAck + 1));
					}
					
					// If the cwnd has been reached and cwnd is less than ssthresh, cwnd is in Slow Start, change cwnd's value
					if((lastAck - previouslySent) == cwnd && cwnd < ssthresh){
						// If cwnd * 2 is greater than ssthresh, make cwnd equal to ssthresh
						if((cwnd * 2) > ssthresh){
							// Make previouslySent the last acked packet
							previouslySent = lastAck;
							// Slow Start, make cwnd = ssthresh
							System.out.println("Required number of acks recieved for cwnd = " + cwnd);
							cwnd = ssthresh;
							System.out.println("cwnd = " + cwnd);
							// If cwnd * 2 is less than ssthresh, double cwnd
						}else{
							// Make previouslySent the last acked packet
							previouslySent = lastAck;
							// Slow Start, make cwnd = cwnd * 2
							System.out.println("Required number of acks recieved for cwnd = " + cwnd);
							cwnd = cwnd * 2;
							System.out.println("cwnd = " + cwnd);
						}
						// If the cwnd has been reached and cwnd is greater than ssthresh, cwnd is in Collision Avoidance, change cwnd's value
					}else if((lastAck - previouslySent) == cwnd && cwnd >= ssthresh){
						previouslySent += cwnd;
						// Collision Avoidance, increment cwnd by 1
						System.out.println("Required number of acks recieved for cwnd = " + cwnd);
						cwnd = cwnd + 1;
						System.out.println("cwnd = " + cwnd);
					}
				}
				
				// Close all the sockets, readers and writers
				file_reader.close();
				reader.close();
				writer.close();
				client_reader.close();
				socket.close();
			}
			System.out.println("\nQuit");


		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	
	// Change the value of lastAck
	public static void setAckNum(int ackNum){
		if(ackNum > lastAck)
			lastAck = ackNum;
	}

}