package controller;


import com.mongodb.MongoException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.InsertOneResult;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.ContextMenuEvent;
import javafx.stage.Stage;
import model.*;
import org.bson.Document;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.mongodb.client.model.Filters.eq;
import static utils.Utils.saveDBMSToXML;

public class MainController {
    @FXML
    private TreeView<String> mainTreeView;
    @FXML
    private TextArea sqlField;
    @FXML
    private TextArea resultTextArea;

    private ContextMenu rootContextMenu;
    private MenuItem addDatabaseItem;
    private ContextMenu tablesContextMenu;
    private MenuItem addTableItem;
    private ContextMenu dbContextMenu;
    private MenuItem dropDatabase;
    private ContextMenu tbContextMenu;
    private MenuItem dropTable;
    private ContextMenu indexContextMenu;
    private MenuItem createIndex;
    private ContextMenu dropIndexContextMenu;
    private MenuItem dropIndex;
    private ContextMenu createFKContextMenu;
    private MenuItem createFK;

    private Databases myDBMS;
    private DataBase crtDatabase;
    private MongoClient mongoClient;

    public void setDatabases(Databases myDBMS) {
        this.myDBMS = myDBMS;
        init();
    }

    public void setMongo(MongoClient mongoClient){
        this.mongoClient = mongoClient;
    }

    public void refreshTree(ActionEvent event){
        init();
        resultTextArea.setText("");
        resultTextArea.setStyle("-fx-text-fill: black;");
    }

    public void init() {
        TreeItem<String> rootItem = new TreeItem<>("Databases");

        List<DataBase> databaseList = myDBMS.listDatabases();

        for (DataBase db : databaseList) {
            TreeItem<String> dbItem = new TreeItem<>(db.getDatabaseName());
            TreeItem<String> tables = new TreeItem<>("Tables");
            List<Table> tablesList = db.getTables();
            for (Table tb : tablesList) {
                TreeItem<String> tbItem = new TreeItem<>(tb.getTableName());
                TreeItem<String> columns = new TreeItem<>("Columns");
                TreeItem<String> keys = new TreeItem<>("Keys");
                TreeItem<String> indexes = new TreeItem<>("Indexes");

                List<Column> columnList = tb.getColumns();
                for (Column cl : columnList) {
                    String key;
                    String value;
                    String keys_value;

                    if (cl.isPrimaryKey())
                    {
                        key = "PK";
                        value = cl.getColumnName() + " (" + key + ", " + cl.getType() + ")";

                        keys_value = "PK_" + tb.getTableName();
                        TreeItem<String> keyItem = new TreeItem<>(keys_value);
                        keys.getChildren().add(keyItem);
                    }

                    else if (tb.getForeignKeys().stream().map(fk ->
                            fk.getFkAttribute().toLowerCase()).toList().contains(cl.getColumnName().toLowerCase())) {
                        key = "FK";
                        value = cl.getColumnName() + " (" + key + ", " + cl.getType() + ")";
                    }
                    else value = cl.getColumnName() + " (" + cl.getType() + ")";

                    TreeItem<String> clItem = new TreeItem<>(value);
                    columns.getChildren().add(clItem);
                }

                List<ForeignKey> fkList = tb.getForeignKeys();
                for (ForeignKey fk : fkList) {
                    String keys_value;

                    keys_value = "FK_" + tb.getTableName() + "_" + fk.getRefTable();
                    TreeItem<String> keyItem = new TreeItem<>(keys_value);
                    keys.getChildren().add(keyItem);
                }

                List<Index> indexesList = tb.getIndexes();
                for (Index ix : indexesList) {
                    TreeItem<String> ixItem = new TreeItem<>(ix.getIndexName());
                    indexes.getChildren().add(ixItem);
                }

                tbItem.getChildren().addAll(columns, keys, indexes);
                tables.getChildren().add(tbItem);
            }
            dbItem.getChildren().add(tables);

            rootItem.getChildren().add(dbItem);
        }

        mainTreeView.setRoot(rootItem);

        // Create a ContextMenu and MenuItem for adding a new database
        rootContextMenu = new ContextMenu();
        addDatabaseItem = new MenuItem("New Database");
        addDatabaseItem.setOnAction(this::addNewDatabase);
        rootContextMenu.getItems().add(addDatabaseItem);

        // Create a ContextMenu and MenuItem for dropping a database
        dbContextMenu = new ContextMenu();
        dropDatabase = new MenuItem("Drop Database");
        dropDatabase.setOnAction(this::dropDatabaseFromContext);
        dbContextMenu.getItems().add(dropDatabase);

        // Create a ContextMenu and MenuItem for adding a new table
        tablesContextMenu = new ContextMenu();
        addTableItem = new MenuItem("New Table");
        addTableItem.setOnAction(this::addNewTable);
        tablesContextMenu.getItems().add(addTableItem);

        // Create a ContextMenu and MenuItem for dropping a table
        tbContextMenu = new ContextMenu();
        dropTable = new MenuItem("Drop Table");
        dropTable.setOnAction(this::dropTableFromContext);
        tbContextMenu.getItems().add(dropTable);

        // Create a ContextMenu and MenuItem for creating an index
        indexContextMenu = new ContextMenu();
        createIndex = new MenuItem("New Index");
        createIndex.setOnAction(this::addNewIndex);
        indexContextMenu.getItems().add(createIndex);

        // Create a ContextMenu and MenuItem for dropping an index
        dropIndexContextMenu = new ContextMenu();
        dropIndex = new MenuItem("Drop Index");
        dropIndex.setOnAction(this::dropIndexFromContext);
        dropIndexContextMenu.getItems().add(dropIndex);

        // Create a ContextMenu and MenuItem for creating a foreign key
        createFKContextMenu = new ContextMenu();
        createFK = new MenuItem("Create Foreign Key");
        createFK.setOnAction(this::addFK);
        createFKContextMenu.getItems().add(createFK);


        mainTreeView.addEventHandler(ContextMenuEvent.CONTEXT_MENU_REQUESTED, event -> {
            // Set up the context menu to display when the root item is right-clicked
            if (mainTreeView.getRoot() == mainTreeView.getTreeItem(mainTreeView.getSelectionModel().getSelectedIndex())) {
                rootContextMenu.show(mainTreeView, event.getScreenX(), event.getScreenY());
                event.consume();
            }

            // Set up the context menu to display when the "Tables" item is right-clicked
            TreeItem<String> selectedItem = mainTreeView.getSelectionModel().getSelectedItem();
            if (selectedItem.getValue().equals("Tables")) {
                tablesContextMenu.show(mainTreeView, event.getScreenX(), event.getScreenY());
                event.consume();
            }

            // Set up the context menu to display when a database is right-clicked
            List<DataBase> dbList = myDBMS.getDatabasesFunction();
            for (DataBase db : dbList) {
                if (selectedItem.getValue().equals(db.getDatabaseName())) {
                    dbContextMenu.show(mainTreeView, event.getScreenX(), event.getScreenY());
                    event.consume();
                }

                // Set up the context menu to display when a table is right-clicked
                List<Table> tableList = db.getTables();
                for (Table tb : tableList) {
                    if (selectedItem.getValue().equals(tb.getTableName())) {
                        tbContextMenu.show(mainTreeView, event.getScreenX(), event.getScreenY());
                        event.consume();
                    }

                    // Set up the context menu to display when the "Indexes" item is right-clicked
                    if (selectedItem.getValue().equals("Indexes")) {
                        indexContextMenu.show(mainTreeView, event.getScreenX(), event.getScreenY());
                        event.consume();
                    }

                    // Set up the context menu to display when the "Keys" item is right-clicked
                    if (selectedItem.getValue().equals("Keys")) {
                        createFKContextMenu.show(mainTreeView, event.getScreenX(), event.getScreenY());
                        event.consume();
                    }

                    List<Index> indices = tb.getIndexes();
                    for (Index index : indices) {
                        // Set up the context menu to display when an index item is right-clicked
                        if (selectedItem.getValue().equals(index.getIndexName())) {
                            dropIndexContextMenu.show(mainTreeView, event.getScreenX(), event.getScreenY());
                            event.consume();
                        }
                    }

                }
            }

        });
    }


    //////////////////////////////////////////////////////////
    // Methods for Context Menus /////////////////////////////

    private void addNewDatabase(ActionEvent event) {
        // Load the FXML file and create a new stage
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/mainapp/events/createdb-view.fxml"));
        Stage dialogStage = new Stage();
        // Set the owner of the dialog (main stage)
        dialogStage.initOwner(mainTreeView.getScene().getWindow());
        try {
            // Load the scene from the FXML file
            Scene scene = new Scene(loader.load());

            // Set the scene and show the dialog
            dialogStage.setScene(scene);

            // Set the controller for the dialog
            CreateDbController controller = loader.getController();
            controller.setMainTreeView(mainTreeView);
            controller.setDBandField(myDBMS, crtDatabase, resultTextArea);
            controller.setMongo(mongoClient);

            dialogStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void dropDatabaseFromContext(ActionEvent event){
        TreeItem<String> selectedItem = mainTreeView.getSelectionModel().getSelectedItem();
        String databaseName = selectedItem.getValue();

        myDBMS.dropDatabase(databaseName);
        saveDBMSToXML(myDBMS);

        MongoDatabase database = mongoClient.getDatabase(databaseName);
        database.drop();
        resultTextArea.setText("Database " + databaseName + " was dropped!");
    }

    private void addNewTable(ActionEvent event) {
        // Load the FXML file and create a new stage
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/mainapp/events/createtb-view.fxml"));
        Stage dialogStage = new Stage();
        // Set the owner of the dialog (main stage)
        dialogStage.initOwner(mainTreeView.getScene().getWindow());
        try {
            // Load the scene from the FXML file
            Scene scene = new Scene(loader.load());

            // Set the scene and show the dialog
            dialogStage.setScene(scene);

            TreeItem<String> selectedItem = mainTreeView.getSelectionModel().getSelectedItem();
            String dbName = selectedItem.getParent().getValue();
            crtDatabase = myDBMS.getDatabaseByName(dbName);

            // Set the controller for the dialog
            CreateTbController controller = loader.getController();
            controller.setMainTreeView(mainTreeView);
            controller.setDBandField(myDBMS, crtDatabase, resultTextArea);
            controller.setMongo(mongoClient);

            dialogStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
        crtDatabase = null;
    }


    private void dropTableFromContext(ActionEvent event) {
        TreeItem<String> selectedItem = mainTreeView.getSelectionModel().getSelectedItem();
        String tableName = selectedItem.getValue();

        String dbName = selectedItem.getParent().getParent().getValue();
        crtDatabase = myDBMS.getDatabaseByName(dbName);

        List<Table> tableList =  crtDatabase.getTables();
        for (Table table: tableList){
            List<ForeignKey> foreignKeyList = table.getForeignKeys();
            for (ForeignKey fk: foreignKeyList) {
                if (tableName.equals(fk.getRefTable())){
                    resultTextArea.setStyle("-fx-text-fill: red;");
                    resultTextArea.setText("Could not drop object " + tableName + " because it is referenced by a FOREIGN KEY constraint.");
                    return;
                }
            }
        }

        crtDatabase.dropTable(tableName);
        saveDBMSToXML(myDBMS);

        //Connecting to the database
        MongoDatabase database = mongoClient.getDatabase(crtDatabase.getDatabaseName());
        // drop table
        database.getCollection(tableName).drop();

        resultTextArea.setText("Table " + tableName + " was dropped!");
        crtDatabase = null;
    }

    private void addNewIndex(ActionEvent event) {
        // Load the FXML file and create a new stage
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/mainapp/events/createindex-view.fxml"));
        Stage dialogStage = new Stage();
        // Set the owner of the dialog (main stage)
        dialogStage.initOwner(mainTreeView.getScene().getWindow());
        try {
            // Load the scene from the FXML file
            Scene scene = new Scene(loader.load());

            // Set the scene and show the dialog
            dialogStage.setScene(scene);

            TreeItem<String> selectedItem = mainTreeView.getSelectionModel().getSelectedItem();
            String dbName = selectedItem.getParent().getParent().getParent().getValue();
            String tbName = selectedItem.getParent().getValue();
            crtDatabase = myDBMS.getDatabaseByName(dbName);

            // Set the controller for the dialog
            CreateIndexController controller = loader.getController();
            controller.setAttr(mainTreeView, myDBMS, crtDatabase, tbName, resultTextArea);

            dialogStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
        crtDatabase = null;
    }

    private void dropIndexFromContext(ActionEvent event) {
        TreeItem<String> selectedItem = mainTreeView.getSelectionModel().getSelectedItem();
        String indexName = selectedItem.getValue();
        String tableName = selectedItem.getParent().getParent().getValue();

        String dbName = selectedItem.getParent().getParent().getParent().getParent().getValue();
        crtDatabase = myDBMS.getDatabaseByName(dbName);

        List<Table> tableList =  crtDatabase.getTables();
        for (Table tb: tableList) {
            if (tb.getTableName().equals(tableName)){
                List<Index> indices = tb.getIndexes();
                for (Index ix: indices) {
                    if (ix.getIndexName().equals(indexName)){
                        tb.dropIndex(ix);
                    }
                }
            }
        }

        saveDBMSToXML(myDBMS);
        resultTextArea.setText("Index " + indexName + " was dropped!");
        crtDatabase = null;
    }

    private void addFK(ActionEvent event) {
        // Load the FXML file and create a new stage
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/mainapp/events/createfk-view.fxml"));
        Stage dialogStage = new Stage();
        // Set the owner of the dialog (main stage)
        dialogStage.initOwner(mainTreeView.getScene().getWindow());
        try {
            // Load the scene from the FXML file
            Scene scene = new Scene(loader.load());

            // Set the scene and show the dialog
            dialogStage.setScene(scene);

            TreeItem<String> selectedItem = mainTreeView.getSelectionModel().getSelectedItem();
            String dbName = selectedItem.getParent().getParent().getParent().getValue();
            String tbName = selectedItem.getParent().getValue();
            crtDatabase = myDBMS.getDatabaseByName(dbName);

            // Set the controller for the dialog
            CreateFkController controller = loader.getController();
            controller.setAttr(mainTreeView, myDBMS, crtDatabase, tbName, resultTextArea);

            dialogStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
        crtDatabase = null;
    }

    ///////////////////////////////////////////////////////////
    // Methods for SQL Statements /////////////////////////////

    public void runSqlStm(ActionEvent event) {
        ProcessSqlStatement();
    }

    public void ProcessSqlStatement() {
        if (ProcessUseDatabase()) {
            return;
        }
        if (ProcessCreateDatabase()) {
            return;
        }
        if (ProcessDropDatabase()) {
            return;
        }
        if (ProcessDropTable()) {
            return;
        }
        if (ProcessInsertIntoTable()){
            return;
        }
        if (ProcessDeleteFromTable()){
            return;
        }
        else {
            resultTextArea.setText("SQL Statement unknown!");
        }
    }

    public boolean ProcessUseDatabase() {
        String useDatabasePatter = "(use) [a-zA-Z_$][a-zA-Z_$0-9]*;";
        String databaseName = "";

        Pattern pattern = Pattern.compile(useDatabasePatter);
        Matcher matcher = pattern.matcher(sqlField.getText().toLowerCase());

        if (!matcher.matches()) {
            return false;
        }

        databaseName = sqlField.getText().substring(4, sqlField.getText().length() -1);

        List<DataBase> databaseList = myDBMS.listDatabases();
        if (!databaseList.stream().map(database -> database.getDatabaseName().toLowerCase()).toList().contains(databaseName.toLowerCase())) {
            resultTextArea.setText("This database name does not exist. Try again!");
            return true;
        }

        resultTextArea.setText("Using " + databaseName + " database!");
        crtDatabase = myDBMS.getDatabaseByName(databaseName);
        return true;
    }

    public boolean ProcessCreateDatabase() {
        String createDatabasePatter = "(create database) [a-zA-Z_$][a-zA-Z_$0-9]*;";
        String databaseName = "";

        Pattern pattern = Pattern.compile(createDatabasePatter);
        Matcher matcher = pattern.matcher(sqlField.getText().toLowerCase());

        if (!matcher.matches()) {
            return false;
        }

        databaseName = sqlField.getText().substring(16, sqlField.getText().length() -1);

        List<DataBase> databaseList = myDBMS.listDatabases();
        if (databaseList.stream().map(database -> database.getDatabaseName().toLowerCase()).toList().contains(databaseName.toLowerCase())) {
            resultTextArea.setText("This database name already exists. Try again!");
            return true;
        }

        myDBMS.createDatabase(databaseName);
        saveDBMSToXML(myDBMS);
        mongoClient.getDatabase(databaseName);
        resultTextArea.setText("Database " + databaseName + " was created!");
        crtDatabase = myDBMS.getDatabaseByName(databaseName);
        return true;
    }

    public boolean ProcessDropDatabase() {
        String dropDatabasePatter = "(drop database) [a-zA-Z_$][a-zA-Z_$0-9]*;";
        String databaseName = "";

        Pattern pattern = Pattern.compile(dropDatabasePatter);
        Matcher matcher = pattern.matcher(sqlField.getText().toLowerCase());

        if (!matcher.matches()) {
            return false;
        }

        databaseName = sqlField.getText().substring(14, sqlField.getText().length() -1);

        List<DataBase> databaseList = myDBMS.listDatabases();
        if (!databaseList.stream().map(database -> database.getDatabaseName().toLowerCase()).toList().contains(databaseName.toLowerCase())) {
            resultTextArea.setText("This database name does not exist. Try again!");
            return true;
        }

        myDBMS.dropDatabase(databaseName);
        saveDBMSToXML(myDBMS);

        MongoDatabase database = mongoClient.getDatabase(databaseName);
        database.drop();

        resultTextArea.setText("Database " + databaseName + " was dropped!");
        crtDatabase = null;
        return true;
    }

    public boolean ProcessDropTable() {
        String dropTablePatter = "(drop table) [a-zA-Z_$][a-zA-Z_$0-9]*;";
        String tableName = "";

        Pattern pattern = Pattern.compile(dropTablePatter);
        Matcher matcher = pattern.matcher(sqlField.getText().toLowerCase());

        if (!matcher.matches()) {
            return false;
        }

        if (crtDatabase == null) {
            resultTextArea.setText("Please select a database to use first!");
            return true;
        }

        tableName = sqlField.getText().substring(11, sqlField.getText().length() -1);

        List<Table> tableList =  crtDatabase.getTables();
        if (!tableList.stream().map(table -> table.getTableName().toLowerCase()).toList().contains(tableName.toLowerCase())) {
            resultTextArea.setText("Table name " + tableName +" does not exist in " + crtDatabase.getDatabaseName() +" database. Try again!");
            return true;
        }

        crtDatabase.dropTable(tableName);
        saveDBMSToXML(myDBMS);

        //Connecting to the database
        MongoDatabase database = mongoClient.getDatabase(crtDatabase.getDatabaseName());
        // drop table
        database.getCollection(tableName).drop();

        resultTextArea.setText("Table " + tableName + " was dropped!");
        return true;
    }


    public boolean ProcessInsertIntoTable() {
        // Define the regex pattern to match the INSERT INTO statement
        String insertPattern = "(insert into) (\\S+).*\\((.*?)\\).*(values).*\\((.*?)\\)(.*\\;?);";

        String tableName ;
        List<String> columnNames;
        List<String> columnValues;

        // Compile the regex pattern and create a matcher
        Pattern pattern = Pattern.compile(insertPattern);
        Matcher matcher = pattern.matcher(sqlField.getText());

        // Check if the provided SQL command matches the expected pattern
        if (!matcher.matches()) {
            resultTextArea.setText("Invalid SQL command. Please provide a valid INSERT INTO statement.");
            return false;
        }

        // Check if a database is selected
        if (crtDatabase == null) {
            resultTextArea.setText("Please select a database to use first!");
            return false;
        }

        // Extract information from the matched SQL command
        tableName = matcher.group(2).trim();
        columnNames = Arrays.stream(matcher.group(3).split(",")).map(String::trim).toList();
        columnValues = Arrays.stream(matcher.group(5).split(",")).map(String::trim).toList();

        // Check if the specified table exists in the current database
        List<Table> tableList = crtDatabase.getTables();
        if (tableList.stream().noneMatch(table -> table.getTableName().equalsIgnoreCase(tableName))) {
            resultTextArea.setText("Table " + tableName + " does not exist in the " + crtDatabase.getDatabaseName() + " database. Please try again.");
            return false;
        }

        Table crtTable = new Table();
        for (Table tb: tableList) {
            if (tb.getTableName().equals(tableName)){
                crtTable = tb;
            }
        }

        for (String column : columnNames) {
            if ( crtTable.getColumnByName(column) == null ){
                resultTextArea.setText("Column " + column + " does not exist in the " + crtTable.getTableName() + " table. Please try again.");
                return false;
            }
        }

        // Verify the primary key constraint
        String primaryKeys = null;
        String values = null;

        for (int i=0; i<columnNames.size();i++){
            if ( crtTable.getPrimaryKeys().stream().map(primaryKey -> primaryKey.getPkAttribute().toLowerCase(Locale.ROOT)).toList().contains(columnNames.get(i).toLowerCase()) ){
                if (primaryKeys == null){
                    primaryKeys = columnValues.get(i);
                }else{
                    primaryKeys = primaryKeys + "#" + columnValues.get(i);
                }
            }else{
                if (values == null){
                    values = columnValues.get(i);
                }else{
                    values = values + "#" + columnValues.get(i);
                }
            }
        }

        //Connecting to the database
        MongoDatabase database = mongoClient.getDatabase(crtDatabase.getDatabaseName());
        MongoCollection<Document> collection = database.getCollection(tableName);

        if (!isPrimaryKeyValid(crtTable, columnNames, primaryKeys, collection)) {
            return true;
        }

        try{
            InsertOneResult result = collection.insertOne(new Document()
                    .append("_id", primaryKeys.toString())
                    .append("values", values));
        } catch (MongoException me) {
            resultTextArea.setText("Unable to insert due to an error: " + me);
        }

        resultTextArea.setText("Data was successfully inserted into the table " + tableName);
        return true;
    }

    private boolean isPrimaryKeyValid(Table table, List <String> columnNames, String primaryKeyString , MongoCollection<Document> collection) {
        // Validation logic to check if the primary key constraint is valid
        // If the primary key constraint is valid, return true; otherwise, return false
        List <PrimaryKey> primaryKeys = table.getPrimaryKeys();
        for (PrimaryKey primaryKey: primaryKeys) {
            if ( !columnNames.stream().map(String::toLowerCase).toList().contains(primaryKey.getPkAttribute().toLowerCase()) ){
                resultTextArea.setText("Invalid list of columns. List of columns must contains all primary key fields.");
                return false;
            }
        }
        Document doc = collection.find(eq("_id", primaryKeyString)).first();
        if (doc != null){
            resultTextArea.setText("Primary key violation: A record with the same primary key already exists.");
            return false;
        }

        return true;
    }

    public boolean ProcessDeleteFromTable() {
        String deletePattern = "(delete(\\s+)from)(\\s+)(\\S+)(\\s+)(where)(\\s+)(\\S+)(\\s+)(=)(\\s+)(\\S+).*;";

        String tableName;

        // Compile the regex pattern and create a matcher
        Pattern pattern = Pattern.compile(deletePattern);
        Matcher matcher = pattern.matcher(sqlField.getText());

        // Check if the provided SQL command matches the expected pattern
        if (!matcher.matches()) {
            resultTextArea.setText("Invalid SQL command. Please provide a valid DELETE statement.");
            return false;
        }

        // Check if a database is selected
        if (crtDatabase == null) {
            resultTextArea.setText("Please select a database to use first!");
            return false;
        }

        // Extract information from the matched SQL command
        tableName = matcher.group(4).trim();
        String columnPK = matcher.group(8).trim();
        String columnValue = matcher.group(12).trim();

        List<Table> tableList = crtDatabase.getTables();
        if (tableList.stream().noneMatch(table -> table.getTableName().equalsIgnoreCase(tableName))) {
            resultTextArea.setText("Table " + tableName + " does not exist in the " + crtDatabase.getDatabaseName() + " database. Please try again.");
            return false;
        }

        Table crtTable = new Table();
        for (Table tb: tableList) {
            if (tb.getTableName().equals(tableName)){
                crtTable = tb;
            }
        }

        if (crtTable.getColumns().stream().noneMatch(column -> column.getColumnName().equalsIgnoreCase(columnPK))) {
            resultTextArea.setText("Column " + columnPK + " does not exist in the " + crtTable.getTableName() + " table. Please try again.");
            return false;
        }

        //Connecting to the database
        MongoDatabase database = mongoClient.getDatabase(crtDatabase.getDatabaseName());
        //Creating a collection
        MongoCollection<Document> collection = database.getCollection(tableName);
        //Deleting a document
        collection.deleteOne(Filters.eq("_id", columnValue));

        resultTextArea.setText("Data was successfully deleted from the table " + tableName);
        return true;
    }

}
