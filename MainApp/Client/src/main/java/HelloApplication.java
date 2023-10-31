import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import controller.HelloController;
import controller.MainController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import model.Databases;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        String serverAddress = "127.0.0.1"; // Server IP address
        int serverPort = 12345; // Server port

        try (Socket socket = new Socket(serverAddress, serverPort);
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            // Read and deserialize the list of custom objects
            Databases myDBMS = (Databases) in.readObject();

            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/mainapp/events/hello-view.fxml"));
            Parent root = fxmlLoader.load();

            // mongodb
            String uri = "mongodb+srv://andreea:crytina31@isgbd.tovblw2.mongodb.net/?retryWrites=true&w=majority";
            MongoClient mongoClient = null;
            try {
                mongoClient = MongoClients.create(uri);
            }
            catch(Exception e) {
                System.out.println(e.getMessage());
            }

            HelloController helloController = fxmlLoader.getController();
            helloController.setDatabases(myDBMS);
            helloController.setMongo(mongoClient);

            Scene scene = new Scene(root, 320, 240);
            stage.setScene(scene);

            stage.show();

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }

    }

    public static void main(String[] args) {
        launch();
    }
}