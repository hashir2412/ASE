package server_api;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.util.Arrays;

public class Communicate implements ICommunicate {
  
  @Override
  public void createFile(String fileName, byte[] data) throws RemoteException {
    // TODO Auto-generated method stub
    final String dir =
    Paths.get("").toAbsolutePath().toString() +
    "\\src\\server_api\\files\\";
    String path = dir + fileName;
    File f = new File(path);
    try {
      if (f.createNewFile()) {
        BufferedOutputStream out = new BufferedOutputStream(
          new FileOutputStream(f.getName())
        );
        out.write(data, 0, data.length);
        out.flush();
        out.close();
        System.out.println("File successfully created on server");
      } else {
        System.out.println("Error: File not created or already exists.");
      }
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  @Override
  public void modifyFile(String fileName, byte[] data) throws RemoteException {
    // TODO Auto-generated method stub
    final String dir =
    Paths.get("").toAbsolutePath().toString() +
    "\\src\\server_api\\files\\";
    String path = dir + fileName;
    try {
      BufferedOutputStream out = new BufferedOutputStream(
        new FileOutputStream(path)
      );
      out.write(data, 0, data.length);
      out.flush();
      out.close();
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    System.out.println("File successfully modified on server");
  }

  @Override
  public void deleteFile(String fileName) throws RemoteException {
    // TODO Auto-generated method stub
    final String dir =
    Paths.get("").toAbsolutePath().toString() +
    "\\src\\server_api\\files\\";
    File delFile = new File(dir + fileName);
    if (delFile.delete()) {
      System.out.println("File deleted from server");
    } else {
      System.out.println(
        "Error: File doesn't exist or not deleted successfully."
      );
    }
  }
}
