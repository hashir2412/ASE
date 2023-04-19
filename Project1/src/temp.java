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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;


public class Client {

    private static final Map<String, Long> fileLastModifiedMap = new HashMap<>();

    public static void main(String[] args) {

        String fileName = "text_large.txt";
        try {
            // create a watch service to monitor the file for changes
            Path directory = Paths.get(Constants.CLIENT_FILE_ROOT);
            WatchService watchService = directory.getFileSystem().newWatchService();
            directory.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
            System.out.println("Watching for changes in " + directory);

            // start the client
            InetAddress serverIp = InetAddress.getByName("localhost");
            Socket tcpSocket = new Socket(serverIp, Constants.SERVER_TCP_PORT);

            // get the port number from the server that will receive data through UDP datagrams
            String action = "SEND REQUEST";
            int serverPort = getPortFromServer(tcpSocket, action, fileName, Constants.CLIENT_UDP_PORT);
            if (serverPort == 0) {
                return;
            }

            // start sending the file
            PacketBoundedBufferMonitor bufferMonitor = new PacketBoundedBufferMonitor(Constants.MONITOR_BUFFER_SIZE);
            InetAddress senderIp = InetAddress.getByName("localhost");

            PacketSender packetSender = new PacketSender(bufferMonitor, senderIp, Constants.CLIENT_UDP_PORT, serverIp, serverPort);
            packetSender.start();

            FileReader fileReader = new FileReader(bufferMonitor, fileName);
            fileReader.start();

            // loop indefinitely and wait for changes to the file
            while (true) {
                WatchKey watchKey = watchService.take();
                for (WatchEvent<?> event : watchKey.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();
                    if (kind == StandardWatchEventKinds.OVERFLOW) {
                        continue;
                    }

                    WatchEvent<Path> ev = (WatchEvent<Path>) event;
                    Path filePath = ev.context();
                    if (filePath.toString().equals(fileName)) {
                        long lastModified = fileLastModifiedMap.getOrDefault(filePath.toString(), 0L);
                        File file = filePath.toFile();
                        if (file.lastModified() > lastModified) {
                            System.out.println("File modified: " + fileName);
                            fileLastModifiedMap.put(filePath.toString(), file.lastModified());
                            FileReader fileReaderThread = new FileReader(bufferMonitor, fileName);
                            fileReaderThread.start();
                        }
                    }
                }

                // reset the watch key
                boolean valid = watchKey.reset();
                if (!valid) {
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * get the port number, through which the server will receive data
     *
     * @param tcpSocket
     * @param action
     * @param fileName
     * @param udpPort,  the port number that the client will send data
     * @return
     */
    public static int getPortFromServer(Socket tcpSocket, String action, String fileName, int udpPort) {
        int server
