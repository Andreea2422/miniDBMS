package controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import model.*;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import static utils.Utils.saveDBMSToXML;

public class CreateIndexController {
    @FXML
    private TextField indexNameField;
    @FXML
    private TextField indexTypeField;
    @FXML
    private TextField tbNameField;
    @FXML
    private VBox columnDetailsVBox;
    @FXML
    private AnchorPane anchorColumnDetails;
    @FXML
    private ComboBox<String> columnsBox;
    @FXML
    private CheckBox uniqueCheckbox;
    @FXML
    private Button addColumnButton;
    @FXML
    private ScrollPane columnDetailsScrollPane;

    private TreeView<String> mainTreeView;
    private Databases myDBMS;
    private DataBase crtDatabase;
    private TextArea resultTextArea;
    private Button lastAddedButton;
    private String tbName;

    public void setAttr(TreeView<String> mainTreeView, Databases myDBMS, DataBase crtDatabase, String tbName, TextArea resultTextArea) {
        this.mainTreeView = mainTreeView;
        this.myDBMS = myDBMS;
        this.crtDatabase = crtDatabase;
        this.tbName = tbName;
        this.resultTextArea = resultTextArea;
        init();
    }

    private ObservableList<String> columnsBoxItems;

    public void init() {
        List<Table> tables = crtDatabase.getTables();
        for (Table tb: tables) {
            if (tb.getTableName().equals(tbName)){
                List<Column> columns = tb.getColumns();

                // Create a new observable list for each table
                columnsBoxItems = FXCollections.observableArrayList();

                for (Column cl: columns) {
                    columnsBoxItems.add(cl.getColumnName());
                }
            }
        }

        // Set the items in the ComboBox
        columnsBox.setItems(columnsBoxItems);

        tbNameField.setText(tbName);
    }

    public void addColumn(ActionEvent event) {
        addColumnButton.setVisible(false);

        AnchorPane newColumnAnchorPane = new AnchorPane();
        newColumnAnchorPane.setPrefHeight(41.0);
        newColumnAnchorPane.setPrefWidth(615.0);

        ComboBox<String> columnsBox = new ComboBox<>();
        columnsBox.setId("columnsBox");
        columnsBox.setEditable(true);
        columnsBox.setLayoutX(23.0);
        columnsBox.setLayoutY(9.0);
        columnsBox.setPrefHeight(26.0);
        columnsBox.setPrefWidth(302.0);
        columnsBox.setItems(columnsBoxItems);

        Button addNewColumnButton = new Button();
        addNewColumnButton.setOnAction(this::addColumn);
        addNewColumnButton.setLayoutX(478.0);
        addNewColumnButton.setLayoutY(9.0);
        addNewColumnButton.setText("+");

        newColumnAnchorPane.getChildren().addAll(columnsBox, addNewColumnButton);
        columnDetailsVBox.getChildren().add(newColumnAnchorPane);

        // Set the ScrollPane to scroll if the content exceeds its height
        columnDetailsScrollPane.setVvalue(1.0);

        // Hide the previous button if it exists
        if (lastAddedButton != null) {
            lastAddedButton.setVisible(false);
        }
        // Update the last added button
        lastAddedButton = addNewColumnButton;
    }

    public void addIndex(ActionEvent event) {
        List<String> indexColumns = new ArrayList<>();
        List<Table> tables = crtDatabase.getTables();

        String indexName = indexNameField.getText();
//        String indexType = indexTypeField.getText();

        if (indexName.isEmpty()) {
            resultTextArea.setText("Please input index name");
            return;
        }
//        else if (indexType.isEmpty()) {
//            resultTextArea.setText("Please input index type");
//            return;
//        }

        // Access the column details
        for (Node node : columnDetailsVBox.getChildren()) {
            if (node instanceof AnchorPane) {
                AnchorPane columnDetailsAnchorPane = (AnchorPane) node;

                ComboBox<String> columnsBox = (ComboBox<String>) columnDetailsAnchorPane.lookup("#columnsBox");
                String columnsNameValue = columnsBox.getValue();

                for (Table tb: tables) {
                    if (tb.getTableName().equals(tbName)){
                        List<Column> columns = tb.getColumns();
                        for (Column cl: columns) {
                            if (cl.getColumnName().equals(columnsNameValue)){
                                indexColumns.add(columnsNameValue);
                            }
                        }
                    }
                }

                if (columnsNameValue == null) {
                    resultTextArea.setText("Please select a column for index");
                    return;
                }
            }
        }

        Index index = new Index(indexName, tbName, indexColumns);
        for (Table tb: tables) {
            if (tb.getTableName().equals(tbName)){
                tb.createIndex(index);
            }
        }
        saveDBMSToXML(myDBMS);
        resultTextArea.setText("Index " + indexName + " created successfully!");

        // Close the dialog
        ((Stage) indexNameField.getScene().getWindow()).close();
    }
}
