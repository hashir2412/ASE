package client_side;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import server_api.ICommunicate;

public class Client {

  public static void main(String[] args) {
    String path =
      Paths.get("").toAbsolutePath().toString() +
      "\\src\\client_side\\client\\";
      System.out.println("Synchronising part2/client folder with part2Server folder. Kindly create,modify or delete the file in the above folder.");
    File directory = new File(path);
    try {
      Registry registry = LocateRegistry.getRegistry();
      ICommunicate stub = (ICommunicate) registry.lookup("batman");
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
          // TODO Auto-generated method stub
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
                stub.createFile(fileName, data);
                bip.close();
                System.out.println("File created on client");
              } else if (event.kind() == StandardWatchEventKinds.ENTRY_DELETE) {
                System.out.println(
                  "A file has been deleted: " + p.toAbsolutePath()
                );
                stub.deleteFile(p.getFileName().toString());
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
                stub.modifyFile(p.getFileName().toString(), data);
                System.out.println("File copied to server");
              }
            }
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
      };
      ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
      executor.scheduleAtFixedRate(runnable, 0, 3, TimeUnit.SECONDS);
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }
}
