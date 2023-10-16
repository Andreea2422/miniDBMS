import model.Databases;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

import static utils.Utils.loadDBMSFromXML;

public class StartServer {
    public static void main(String[] args) {
        int port = 12345;


        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server is listening on port " + port);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected: " + clientSocket.getInetAddress().getHostAddress());

                // Create an output stream to send data to the client
                ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());

                // Serialize and send the list of custom objects
                Databases myDBMS = loadDBMSFromXML();
                out.writeObject(myDBMS);

                clientSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
