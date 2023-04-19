import java.io.File;
import java.io.FileInputStream;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Semaphore;




public class Client {
	
	public Client() {	
	}

	public static void main(String[] args) {
		
		// String fileName="video_small.wmv";
		String fileName="text_large.txt";
		//String fileName="text_small.txt";
	
		try {
			File file=new File(Constants.CLIENT_FILE_ROOT+fileName);
			if (!file.exists()) return;		
			
			InetAddress serverIp=InetAddress.getByName("localhost");			
			Socket tcpSocket = new Socket(serverIp, Constants.SERVER_TCP_PORT);
			
			// get the port number from the server that will receive data through UDP datagrams
			String action="SEND REQUEST";
		    int serverPort=getPortFromServer(tcpSocket,action,fileName,Constants.CLIENT_UDP_PORT);
			if (serverPort==0) {return;}			
			
			
			// start sending the file
			PacketBoundedBufferMonitor bufferMonitor=new PacketBoundedBufferMonitor(Constants.MONITOR_BUFFER_SIZE);			
			InetAddress senderIp=InetAddress.getByName("localhost");			
			
			PacketSender packetSender=new PacketSender(bufferMonitor,senderIp,Constants.CLIENT_UDP_PORT,serverIp,serverPort);
			packetSender.start();
			
			FileReader fileReader=new FileReader(bufferMonitor,fileName);
			fileReader.start();
			
			try {
				packetSender.join();
				fileReader.join();				
			} 
	 		catch (InterruptedException e) {}
			
		}catch(Exception e) {e.printStackTrace();}
		
	}// end of main
	

	/**
	 * get the port number, through which the server will receive data 
	 * @param tcpSocket
	 * @param action
	 * @param fileName
	 * @param udpPort, the port number that the client will send data
	 * @return
	 */
	public static int getPortFromServer(Socket tcpSocket,String action,String fileName,int udpPort) {
		int serverPort=0;
		try {
			Scanner inputSocket =  new Scanner(tcpSocket.getInputStream());
			PrintWriter outputSocket = new PrintWriter(tcpSocket.getOutputStream(), true);
			
			// send the HTTP packet	
			String request=action+" # "+fileName+" # "+udpPort;
		    outputSocket.println(request+Constants.CRLF+"STOP");
			System.out.println(Constants.CRLF+">> Request:"+request);
		    
			// receive the response	
		    String line=inputSocket.nextLine();
		    
		  // get the port number of the server that will receive data for the file		    
		    while(!line.equals("STOP")) {
		    	if (line.isEmpty()) {line=inputSocket.nextLine();continue;}
		    	if(line.startsWith(action)){
					// get the new port that is assigned by the server to receive data
		    		System.out.println(">> Response:"+line+Constants.CRLF);
					String [] items=line.split(":");					
					serverPort=Integer.parseInt(items[items.length-1]);
					break;
				}
		    	line=inputSocket.nextLine();
		    }
			 inputSocket.close();
		     outputSocket.close();
		}catch(Exception e) {e.printStackTrace();}
		return serverPort;
	}

	
	
	
}
