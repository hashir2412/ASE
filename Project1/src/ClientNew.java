import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.WatchEvent.Modifier;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class ClientNew {

  private static final String CLIENT_FILE_ROOT = "/path/to/client/files";

  public ClientNew() {}

  public static void main(String[] args) {
    // String fileName="video_small.wmv";
    String fileName = "text_large.txt";
    
    try {
      InetAddress serverIp = InetAddress.getByName("localhost");
      Socket tcpSocket = new Socket(serverIp, Constants.SERVER_TCP_PORT);
      String path = Constants.CLIENT_FILE_ROOT;
      File directory = new File(path);
      WatchService watchService = FileSystems.getDefault().newWatchService();
      WatchKey key = directory
        .toPath()
        .register(
          watchService,
          StandardWatchEventKinds.ENTRY_CREATE,
          StandardWatchEventKinds.ENTRY_DELETE,
          StandardWatchEventKinds.ENTRY_MODIFY
        );
      // while (true) {
      Runnable runnable = new Runnable() {
        @Override
        public void run() {
          System.out.println("In watcher runner thread ");
          while (tcpSocket.isConnected()) {
            try {
              
              for (WatchEvent event : key.pollEvents()) {
                Path p = (Path) event.context();
                if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
                  String fileName = p.getFileName().toString();
                  File file = new File(path + fileName);
                  BufferedInputStream bip = new BufferedInputStream(
                    new FileInputStream(path + fileName)
                  );
                  byte[] data = new byte[(int) file.length()];
                  bip.read(data, 0, data.length);
                  // stub.createFile(fileName, data);
                  bip.close();
                  System.out.println("File created on client");
                } else if (event.kind() == StandardWatchEventKinds.ENTRY_DELETE) {
                  System.out.println(
                    "A file has been deleted: " + p.toAbsolutePath()
                  );
                  // stub.deleteFile(p.getFileName().toString());
                } else if (event.kind() == StandardWatchEventKinds.ENTRY_MODIFY) {
                  String fileName = p.getFileName().toString();
                  File file = new File(path + fileName);
                  System.out.println("A file has been modified: " + fileName);
                  BufferedInputStream bip = new BufferedInputStream(
                    new FileInputStream(path + fileName)
                  );
                  byte[] data = new byte[(int) file.length()];
                  bip.read(data, 0, data.length);
                  bip.close();
                  // Handle file modification
                  // stub.modifyFile(p.getFileName().toString(), data);
                  //
                  System.out.println("File copied to server");
                }  else if (event.kind() == StandardWatchEventKinds.OVERFLOW) {
                  // Overflow occured -> 
                  // Reaxamine
                }
                key.reset();
              }
            } catch (Exception e) {
              e.printStackTrace();
            }
          }
        }
      };
      ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
      executor.scheduleAtFixedRate(runnable, 0, 3, TimeUnit.SECONDS);
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    //
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
