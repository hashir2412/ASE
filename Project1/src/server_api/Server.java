package server_api;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class Server{
    public static void main(String[] args) {
        try {
            Communicate obj = new Communicate();
            Registry reg = LocateRegistry.createRegistry(1099);
            ICommunicate stub = (ICommunicate) UnicastRemoteObject.exportObject(obj, 1099);
            // Bind the remote object's stub in the registry
            //Registry registry = LocateRegistry.getRegistry();
            reg.bind("batman", stub);

            System.err.println("Server ready");
        } catch (Exception e) {
            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
        }
    }    
}