package controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import model.*;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import static utils.Utils.saveDBMSToXML;

public class CreateTbController implements Initializable {
    @FXML
    private TextField columnName;
    @FXML
    private TextField tbNameField;
    @FXML
    private VBox columnDetailsVBox;
    @FXML
    private AnchorPane anchorColumnDetails;
    @FXML
    private ComboBox<String> dataType;
    @FXML
    private CheckBox primaryKeyCheckbox;
    @FXML
    private Button addColumnButton;
    @FXML
    private ScrollPane columnDetailsScrollPane;

    private TreeView<String> mainTreeView;
    private Databases myDBMS;
    private DataBase crtDatabase;
    private TextArea resultTextArea;
    private Button lastAddedButton;

    public void setMainTreeView(TreeView<String> mainTreeView) {
        this.mainTreeView = mainTreeView;
    }

    public void setDBandField(Databases myDBMS, DataBase crtDatabase, TextArea resultTextArea) {
        this.myDBMS = myDBMS;
        this.crtDatabase = crtDatabase;
        this.resultTextArea = resultTextArea;
    }

    // Declare an ObservableList for the ComboBox items
    private ObservableList<String> dataTypeItems;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize the ObservableList with initial items
        dataTypeItems = FXCollections.observableArrayList(
                "bigint", "char", "datetime", "date",
                "float", "int", "nvarchar(50)", "real",
                "smalldatetime", "smallint", "text", "timestamp",
                "tinyint", "varchar(50)", "varbinary(50)", "xml");

        // Set the items in the ComboBox
        dataType.setItems(dataTypeItems);

//        applyEventTo(dataType);
    }

//    private void applyEventTo(ComboBox<String> comboBox) {
//        comboBox.getEditor().addEventFilter(KeyEvent.KEY_RELEASED, event -> {
//            if (event.getCode().isLetterKey() || event.getCode().isDigitKey() || event.getCode().isWhitespaceKey()) {
//                String filterText = comboBox.getEditor().getText().toLowerCase();
//                ObservableList<String> filteredOptions = FXCollections.observableArrayList();
//
//                for (String option : dataTypeItems) {
//                    if (option.toLowerCase().contains(filterText)) {
//                        filteredOptions.add(option);
//                    }
//                }
//
//                comboBox.setItems(filteredOptions);
//            }
//        });
//    }

    public void addColumn(ActionEvent event) {
        addColumnButton.setVisible(false);

        // Create a new AnchorPane to hold the details for a new column
        AnchorPane newColumnAnchorPane = new AnchorPane();
        newColumnAnchorPane.setPrefHeight(44.0);
        newColumnAnchorPane.setPrefWidth(615.0);

        // Create the elements for the new column
        TextField columnName = new TextField();
        columnName.setId("columnName");
        columnName.setLayoutX(14.0);
        columnName.setLayoutY(14.0);
        columnName.setPrefHeight(26.0);
        columnName.setPrefWidth(207.0);

        ComboBox<String> dataType = new ComboBox<>();
        dataType.setId("dataType");
        dataType.setEditable(true);
        dataType.setLayoutX(279.0);
        dataType.setLayoutY(14.0);
        dataType.setPrefWidth(150.0);
        dataType.setItems(dataTypeItems);
//        applyEventTo(dataType);

        CheckBox primaryKeyCheckbox = new CheckBox();
        primaryKeyCheckbox.setId("primaryKeyCheckbox");
        primaryKeyCheckbox.setLayoutX(485.0);
        primaryKeyCheckbox.setLayoutY(17.0);
        primaryKeyCheckbox.setPrefHeight(18.0);
        primaryKeyCheckbox.setPrefWidth(18.0);

        Button addNewColumnButton = new Button();
        addNewColumnButton.setOnAction(this::addColumn);
        addNewColumnButton.setLayoutX(576.0);
        addNewColumnButton.setLayoutY(14.0);
        addNewColumnButton.setText("+");

        newColumnAnchorPane.getChildren().addAll(columnName, dataType, primaryKeyCheckbox, addNewColumnButton);
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

    public void addTable(ActionEvent actionEvent){
        List<Column> columns = new ArrayList<>();
        List<PrimaryKey> primaryKeys = new ArrayList<>();
        List<ForeignKey> foreignKeys = new ArrayList<>();
        List<Index> indexes = new ArrayList<>();

        // Get the new table name from the TextField
        String tableName = tbNameField.getText();
        if (tableName.isEmpty()) {
            resultTextArea.setText("Please input table name.");
            return;
        }

        if (crtDatabase == null) {
            resultTextArea.setText("Please select a database to use first!");
            return;
        }

        List<Table> tableList =  crtDatabase.getTables();
        if (tableList.stream().map(table -> table.getTableName().toLowerCase()).toList().contains(tableName.toLowerCase())) {
            resultTextArea.setText("Table name " + tableName +" already exist in " + crtDatabase.getDatabaseName() +" database. Try again!");
            return;
        }

        // Access the column details
        for (Node node : columnDetailsVBox.getChildren()) {
            if (node instanceof AnchorPane) {
                AnchorPane columnDetailsAnchorPane = (AnchorPane) node;

                // Access the input fields within the AnchorPane
                TextField columnName = (TextField) columnDetailsAnchorPane.lookup("#columnName");
                ComboBox<String> dataType = (ComboBox<String>) columnDetailsAnchorPane.lookup("#dataType");
                CheckBox primaryKeyCheckbox = (CheckBox) columnDetailsAnchorPane.lookup("#primaryKeyCheckbox");

                String columnNameValue = columnName.getText();
                String dataTypeValue = dataType.getValue();
                boolean isPrimaryKey = primaryKeyCheckbox.isSelected();

                Column newColumn = new Column(columnNameValue, dataTypeValue, isPrimaryKey);
                columns.add(newColumn);

                if (isPrimaryKey) {
                    PrimaryKey newPrimaryKey = new PrimaryKey(columnNameValue);
                    primaryKeys.add(newPrimaryKey);
                    List<String> columnsName = new ArrayList<>();
                    columnsName.add(columnNameValue);
                    String pk_index_name = "PK_" + columnNameValue;
                    Index pk_index = new Index(pk_index_name, tableName, columnsName);
                    indexes.add(pk_index);
                }

                if (columnNameValue.isEmpty()) {
                    resultTextArea.setText("Invalid column name");
                    return;
                } else if (dataTypeValue == null) {
                    resultTextArea.setText("Please select a data type for column " + columnNameValue);
                    return;
                }
            }
        }

        Table table = new Table(tableName, columns, primaryKeys, foreignKeys);
        table.setIndexes(indexes);
        crtDatabase.createTable(table);
        saveDBMSToXML(myDBMS);
        resultTextArea.setText("Table " + tableName + " created successfully!");

        // Close the dialog
        ((Stage) tbNameField.getScene().getWindow()).close();
    }

}
