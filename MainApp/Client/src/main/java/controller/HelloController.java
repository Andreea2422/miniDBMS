package controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import model.Databases;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

import static utils.Utils.loadDBMSFromXML;

public class HelloController {
    @FXML
    private Label welcomeText;

    private Databases myDBMS;


    public void setDatabases(Databases myDBMS) {
        this.myDBMS = myDBMS;
    }

    public void switchToMain(ActionEvent actionEvent) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/mainapp/events/main-view.fxml"));
        Parent root = fxmlLoader.load();

        Stage stage = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();

        myDBMS = loadDBMSFromXML();
        MainController mainController = fxmlLoader.getController();
        mainController.setDatabases(myDBMS);

        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }
}