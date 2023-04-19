import java.io.File;
import java.io.FileInputStream;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Semaphore;

public class Client {

  private static final String CLIENT_FILE_ROOT = "/path/to/client/files";

  public Client() {}

  public static void main(String[] args) {
    // String fileName="video_small.wmv";
    String fileName = "text_large.txt";
    // String fileName="text_small.txt";

    try {
      // create a watch service for the client file directory
      Path clientDir = Paths.get(CLIENT_FILE_ROOT);
      WatchService watchService = clientDir.getFileSystem().newWatchService();
      clientDir.register(
        watchService,
        StandardWatchEventKinds.ENTRY_CREATE,
        StandardWatchEventKinds.ENTRY_DELETE
      );

      InetAddress serverIp = InetAddress.getByName("localhost");
      Socket tcpSocket = new Socket(serverIp, Constants.SERVER_TCP_PORT);

      // get the port number from the server that will receive data through UDP datagrams
      String action = "SEND REQUEST";
      int serverPort = getPortFromServer(
        tcpSocket,
        action,
        fileName,
        Constants.CLIENT_UDP_PORT
      );
      if (serverPort == 0) {
        return;
      }

      // start sending the file
      PacketBoundedBufferMonitor bufferMonitor = new PacketBoundedBufferMonitor(
        Constants.MONITOR_BUFFER_SIZE
      );
      InetAddress senderIp = InetAddress.getByName("localhost");

      PacketSender packetSender = new PacketSender(
        bufferMonitor,
        senderIp,
        Constants.CLIENT_UDP_PORT,
        serverIp,
        serverPort
      );
      packetSender.start();

      FileReader fileReader = new FileReader(bufferMonitor, fileName);
      fileReader.start();

      // monitor the client file directory for changes
      while (true) {
        WatchKey key = watchService.take();
        for (WatchEvent<?> event : key.pollEvents()) {
          WatchEvent.Kind<?> kind = event.kind();
          if (kind == StandardWatchEventKinds.OVERFLOW) {
            continue;
          }

          WatchEvent<Path> ev = (WatchEvent<Path>) event;
          Path filename = ev.context();
          File file = new File(CLIENT_FILE_ROOT + filename.toString());

          if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
            System.out.println("New file added: " + filename);
            // send a request to the server to transfer the new file
            serverPort =
              getPortFromServer(
                tcpSocket,
                action,
                file.getName(),
                Constants.CLIENT_UDP_PORT
              );
            if (serverPort == 0) {
              continue;
            }
            // start sending the new file
            bufferMonitor =
              new PacketBoundedBufferMonitor(Constants.MONITOR_BUFFER_SIZE);
            packetSender =
              new PacketSender(
                bufferMonitor,
                senderIp,
                Constants.CLIENT_UDP_PORT,
                serverIp,
                serverPort
              );
            packetSender.start();
            fileReader = new FileReader(bufferMonitor, file.getName());
            fileReader.start();
          } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
            System.out.println("File deleted: " + filename);

            // delete the corresponding transfer process
            bufferMonitor.deleteProcess(file.getName());
          }
        }
        key.reset();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  } // end of main

  // get the port number from the server that will receive data through UDP datagrams
  private static int getPortFromServer(
    Socket tcpSocket,
    String action,
    String fileName,
    int clientUdpPort
  ) {
    try {
      PrintWriter out = new PrintWriter(tcpSocket.getOutputStream(), true);
      Scanner in = new Scanner(tcpSocket.getInputStream());

      out.println(action);
      out.println(fileName);
      out.println(clientUdpPort);

      int serverPort = Integer.parseInt(in.nextLine());
      System.out.println("Server UDP port: " + serverPort);

      return serverPort;
    } catch (Exception e) {
      e.printStackTrace();
      return 0;
    }
  }
}
