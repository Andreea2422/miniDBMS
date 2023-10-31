package controller;

import com.mongodb.client.MongoClient;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.stage.Stage;
import model.DataBase;
import model.Databases;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import java.io.File;
import java.util.List;

import static utils.Utils.saveDBMSToXML;

public class CreateDbController {
    @FXML
    private TextField dbNameField;

    private TreeView<String> mainTreeView;
    private Databases myDBMS;
    private DataBase crtDatabase;
    private TextArea resultTextArea;
    private MongoClient mongoClient;

    public void setMainTreeView(TreeView<String> mainTreeView) {
        this.mainTreeView = mainTreeView;
    }

    public void setMongo(MongoClient mongo){
        this.mongoClient = mongo;
    }

    public void setDBandField(Databases myDBMS, DataBase crtDatabase, TextArea resultTextArea) {
        this.myDBMS = myDBMS;
        this.crtDatabase = crtDatabase;
        this.resultTextArea = resultTextArea;
    }

    public void addDatabase(ActionEvent actionEvent){
        // Get the new database name from the TextField
        String newDatabaseName = dbNameField.getText();
        if (newDatabaseName.isEmpty()) {
            resultTextArea.setText("Please input database name");
            return;
        }

        processCreateDbFromContext(newDatabaseName);

        // Close the dialog
        ((Stage) dbNameField.getScene().getWindow()).close();
    }


    public void processCreateDbFromContext(String databaseName){
        List<DataBase> databaseList = myDBMS.listDatabases();
        if (databaseList.stream().map(database -> database.getDatabaseName().toLowerCase()).toList().contains(databaseName.toLowerCase())) {
            resultTextArea.setText("This database name already exist. Try again!");
            return;
        }

        myDBMS.createDatabase(databaseName);
        saveDBMSToXML(myDBMS);

        resultTextArea.setText("Database " + databaseName + " was created!");
        crtDatabase = myDBMS.getDatabaseByName(databaseName);
        mongoClient.getDatabase(databaseName);
    }


}
