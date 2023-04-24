import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;




public class ServerNew {
	
	public ServerNew() {	
	}

	public static void main(String[] args) {
		
		


		try {
			
			ServerSocket serverSocket=null;			
			try {
				serverSocket = new ServerSocket(Constants.SERVER_TCP_PORT);				
			} catch (IOException ioEx) {
				System.out.println("\n>> Unable to set up port!");
				System.exit(1);
			}
			
			System.out.println("\r\n>> Ready to accept requests");
			// handle multiple client connections
			do {
				try {
					// Wait for clients...
					Socket client = serverSocket.accept();					
					System.out.println("\n>> New request is accepted."+Constants.CRLF);
					
					Scanner inputSocket = new Scanner(client.getInputStream());							
					PrintWriter outputSocket = new PrintWriter(client.getOutputStream(), true);
					
					// get action type from the received data
					String line=inputSocket.nextLine();	
					String actionType = "";	
					int clientUDPPort=0;
				    while(!line.equals("STOP")) {
				    	if (line.isEmpty()) {line=inputSocket.nextLine();continue;}
				    	if(line.startsWith("SEND REQUEST")){
				    		System.out.println(">> Request: "+line+Constants.CRLF);
				    		actionType="SEND REQUEST";				    		
				    		clientUDPPort=Integer.parseInt(line.split("#")[2].strip());
							break;
						}
				    	line=inputSocket.nextLine();
				    }
				    
				    if (actionType.equals("SEND REQUEST")) {
				    	receiveHandle(client,outputSocket,clientUDPPort);
				    }
				    
				    
				}catch(IOException io) {
					System.out.println(">> Fail to listen to requests!");
					System.exit(1);
				}
				
			} while (true);// end of while loop
			
	
		}catch(Exception e) {
			e.printStackTrace();
		}

	}// end of main
	
	
public static void receiveHandle(Socket socket,PrintWriter outputSocket,int senderPort) {
	try {
		// create the response with the port number which will receive data from clients through UDP
		String response="SEND REQUEST OK: receive data with the port:"+Constants.SERVER_UDP_PORT;
		System.out.println(">> Response: "+response+Constants.CRLF);
		
		// send the response
		outputSocket.println(response+Constants.CRLF+"STOP");
		outputSocket.close();

		
		PacketBoundedBufferMonitor bm=new PacketBoundedBufferMonitor(Constants.MONITOR_BUFFER_SIZE);
		InetAddress senderIp=socket.getInetAddress();// get the IP of the sender		
		InetAddress receiverIp=InetAddress.getByName("localhost");
		
		receiveFile(bm,receiverIp,Constants.SERVER_UDP_PORT,senderIp, senderPort);// receive the file		

	}catch(Exception e) {e.printStackTrace();}
	
}
	
public static void receiveFile(PacketBoundedBufferMonitor bm, InetAddress receiverIp,int receiverPort,InetAddress senderIp,int senderPort) {
	
	PacketReceiver packetReceiver=new PacketReceiver(bm,receiverIp,receiverPort,senderIp,senderPort);
	packetReceiver.start();
	
	FileWriter fileWriter=new FileWriter(bm);
	fileWriter.start();	
	try {
		packetReceiver.join();
		fileWriter.join();
	} 
		catch (InterruptedException e) {e.printStackTrace();}
	
}
	
	
	
	
}

	