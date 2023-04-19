package server_api;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ICommunicate extends Remote {
    
    // Part 2
    public void createFile(String fileName, byte[] data) throws RemoteException;
    public void deleteFile(String fileName) throws RemoteException;
    public void modifyFile(String fileName, byte[] data) throws RemoteException;
}
