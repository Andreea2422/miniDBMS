package controller;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
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
import java.util.*;

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
    private CheckBox uniqueKeyCheckbox;
    @FXML
    private CheckBox notnullCheckbox;
    @FXML
    private Button addColumnButton;
    @FXML
    private ScrollPane columnDetailsScrollPane;

    private TreeView<String> mainTreeView;
    private Databases myDBMS;
    private DataBase crtDatabase;
    private TextArea resultTextArea;
    private Button lastAddedButton;
    private MongoClient mongoClient;

    public void setMainTreeView(TreeView<String> mainTreeView) {
        this.mainTreeView = mainTreeView;
    }

    public void setMongo(MongoClient mongo) {
        this.mongoClient = mongo;
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
    }

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
        columnName.setPrefWidth(165.0);

        ComboBox<String> dataType = new ComboBox<>();
        dataType.setId("dataType");
        dataType.setEditable(true);
        dataType.setLayoutX(204.0);
        dataType.setLayoutY(12.0);
        dataType.setPrefWidth(150.0);
        dataType.setItems(dataTypeItems);

        CheckBox primaryKeyCheckbox = new CheckBox();
        primaryKeyCheckbox.setId("primaryKeyCheckbox");
        primaryKeyCheckbox.setLayoutX(380.0);
        primaryKeyCheckbox.setLayoutY(16.0);
        primaryKeyCheckbox.setPrefHeight(18.0);
        primaryKeyCheckbox.setPrefWidth(18.0);

        CheckBox uniqueKeyCheckbox = new CheckBox();
        uniqueKeyCheckbox.setId("uniqueKeyCheckbox");
        uniqueKeyCheckbox.setLayoutX(453.0);
        uniqueKeyCheckbox.setLayoutY(16.0);
        uniqueKeyCheckbox.setPrefHeight(18.0);
        uniqueKeyCheckbox.setPrefWidth(18.0);

        CheckBox notnullCheckbox = new CheckBox();
        notnullCheckbox.setId("notnullCheckbox");
        notnullCheckbox.setLayoutX(521.0);
        notnullCheckbox.setLayoutY(18.0);
        notnullCheckbox.setPrefHeight(18.0);
        notnullCheckbox.setPrefWidth(18.0);

        Button addNewColumnButton = new Button();
        addNewColumnButton.setOnAction(this::addColumn);
        addNewColumnButton.setLayoutX(576.0);
        addNewColumnButton.setLayoutY(12.0);
        addNewColumnButton.setText("+");

        newColumnAnchorPane.getChildren().addAll(columnName, dataType, primaryKeyCheckbox, uniqueKeyCheckbox, notnullCheckbox, addNewColumnButton);
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

    public void addTable(ActionEvent actionEvent) {
        List<Column> columns = new ArrayList<>();
        List<PrimaryKey> primaryKeys = new ArrayList<>();
        List<UniqueKey> uniqueKeys = new ArrayList<>();
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

        List<Table> tableList = crtDatabase.getTables();
        if (tableList.stream().map(table -> table.getTableName().toLowerCase()).toList().contains(tableName.toLowerCase())) {
            resultTextArea.setText("Table name " + tableName + " already exist in " + crtDatabase.getDatabaseName() + " database. Try again!");
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
                CheckBox uniqueKeyCheckbox = (CheckBox) columnDetailsAnchorPane.lookup("#uniqueKeyCheckbox");
                CheckBox notnullCheckbox = (CheckBox) columnDetailsAnchorPane.lookup("#notnullCheckbox");

                String columnNameValue = columnName.getText();
                String dataTypeValue = dataType.getValue();

                Integer length = null;
                if (dataTypeValue.contains("(")) {
                    length = Integer.parseInt(dataTypeValue.split("\\(")[1].replaceAll("\\)", ""));
                    dataTypeValue = dataTypeValue.split("\\(")[0];
                }

                if (!ValidateDataType(dataTypeValue)) {
                    resultTextArea.setText("Invalid dataType: " + dataTypeValue);
                    return;
                }
                boolean isPrimaryKey = primaryKeyCheckbox.isSelected();
                boolean isUniqueKey = uniqueKeyCheckbox.isSelected();
                String isnull;
                if (notnullCheckbox.isSelected()) {
                    isnull = "0";
                } else isnull = "1";

                Column newColumn = new Column();
                newColumn.setColumnName(columnNameValue);
                newColumn.setType(dataTypeValue);
                if (length != null) {
                    newColumn.setLength(length);
                }
                newColumn.setPrimaryKey(isPrimaryKey);
                newColumn.setNull(isnull);

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

                if (isUniqueKey) {
                    UniqueKey newUniqueKey = new UniqueKey(columnNameValue);
                    uniqueKeys.add(newUniqueKey);
                    List<String> columnsName = new ArrayList<>();
                    columnsName.add(columnNameValue);
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

        Table table = new Table(tableName, columns, primaryKeys, uniqueKeys, foreignKeys);
        table.setIndexes(indexes);
        crtDatabase.createTable(table);
        saveDBMSToXML(myDBMS);


        //Connecting to the database
        MongoDatabase database = mongoClient.getDatabase(crtDatabase.getDatabaseName());
        //Creating a collection
        database.createCollection(tableName);


        resultTextArea.setText("Table " + tableName + " created successfully!");

        // Close the dialog
        ((Stage) tbNameField.getScene().getWindow()).close();
    }

    public boolean ValidateDataType(String attributeType){
        List<String> dataTypes = Arrays.asList(
                "bigint", "char", "datetime", "date",
                "float", "int", "nvarchar", "real",
                "smalldatetime", "smallint", "text", "timestamp",
                "tinyint", "varchar", "varbinary", "xml");
        if (dataTypes.contains(attributeType.toLowerCase(Locale.ROOT))){
            return true;
        }
        return false;
    }

}
