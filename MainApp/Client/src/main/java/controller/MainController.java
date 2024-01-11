package controller;


import com.google.gson.Gson;
import com.mongodb.MongoException;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.InsertOneResult;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.ContextMenuEvent;
import javafx.stage.Stage;
import javafx.util.Pair;
import model.*;
import org.bson.Document;
import org.bson.conversions.Bson;

import javax.print.Doc;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.invoke.StringConcatException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.regex;
import static com.mongodb.client.model.Sorts.ascending;
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
    private Table controllerTable;
    private MongoClient mongoClient;

    public void setDatabases(Databases myDBMS) {
        this.myDBMS = myDBMS;
        init();
    }

    public void setMongo(MongoClient mongoClient) {
        this.mongoClient = mongoClient;
    }

    public void refreshTree(ActionEvent event) {
        init();
        sqlField.setText("");
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

                    if (cl.isPrimaryKey()) {
                        key = "PK";
                        if (cl.getLength() != null) {
                            value = cl.getColumnName() + " (" + key + ", " + cl.getType() + "(" + cl.getLength() + ")" + ")";
                        } else value = cl.getColumnName() + " (" + key + ", " + cl.getType() + ")";
                    } else if (tb.getForeignKeys().stream().map(fk ->
                            fk.getFkAttribute().toLowerCase()).toList().contains(cl.getColumnName().toLowerCase())) {
                        key = "FK";
                        if (cl.getLength() != null) {
                            value = cl.getColumnName() + " (" + key + ", " + cl.getType() + "(" + cl.getLength() + ")" + ")";
                        } else value = cl.getColumnName() + " (" + key + ", " + cl.getType() + ")";
                    } else if (cl.getLength() != null) {
                        value = cl.getColumnName() + " (" + cl.getType() + "(" + cl.getLength() + ")" + ")";
                    } else value = cl.getColumnName() + " (" + cl.getType() + ")";

                    TreeItem<String> clItem = new TreeItem<>(value);
                    columns.getChildren().add(clItem);
                }

                String keys_value;
                List<PrimaryKey> pkList = tb.getPrimaryKeys();
                if (!pkList.isEmpty()) {
                    keys_value = "PK_" + tb.getTableName();
                    TreeItem<String> keyItem = new TreeItem<>(keys_value);
                    keys.getChildren().add(keyItem);
                }

                List<ForeignKey> fkList = tb.getForeignKeys();
                for (ForeignKey fk : fkList) {
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

    private void dropDatabaseFromContext(ActionEvent event) {
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
        controllerTable = crtDatabase.getTableByName(tableName);

        List<Table> tableList = crtDatabase.getTables();
        for (Table table : tableList) {
            List<ForeignKey> foreignKeyList = table.getForeignKeys();
            for (ForeignKey fk : foreignKeyList) {
                if (tableName.equals(fk.getRefTable())) {
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
        // drop index
        for (Index index : controllerTable.getIndexes()) {
            database.getCollection(index.getIndexName()).drop();
        }

        resultTextArea.setText("Table " + tableName + " was dropped!");
        crtDatabase = null;
        controllerTable = null;
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
            controller.setMongo(mongoClient);

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
        controllerTable = crtDatabase.getTableByName(tableName);

        List<Index> indices = controllerTable.getIndexes();
        indices.removeIf(index -> index.getIndexName().equals(indexName));

        saveDBMSToXML(myDBMS);
        resultTextArea.setText("Index " + indexName + " was dropped!");

        String mongoIndex = indexName + "_" + tableName + "_index";
        //Connecting to the database
        MongoDatabase database = mongoClient.getDatabase(crtDatabase.getDatabaseName());
        // drop index (collection)
        database.getCollection(mongoIndex).drop();

        crtDatabase = null;
        controllerTable = null;
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
        if (ProcessUseDatabase(sqlField.getText())) {
            return;
        }
        if (ProcessInsertIntoTable(sqlField.getText())) {
            return;
        }
        if (ProcessDeleteFromTable()) {
            return;
        }
        if (ProcessSelectFromTable()) {
            return;
        } else {
            resultTextArea.setText("SQL Statement unknown!");
        }
    }


    public boolean ProcessUseDatabase(String sqlStmt) {
        String useDatabasePattern = "(use) [a-zA-Z_$][a-zA-Z_$0-9]*;";
        String databaseName = "";

        Pattern pattern = Pattern.compile(useDatabasePattern);
        Matcher matcher = pattern.matcher(sqlStmt);

        if (!matcher.matches()) {
            return false;
        }

        databaseName = sqlStmt.substring(4, sqlStmt.length() - 1);

        List<DataBase> databaseList = myDBMS.listDatabases();
        if (!databaseList.stream().map(database -> database.getDatabaseName().toLowerCase()).toList().contains(databaseName.toLowerCase())) {
            resultTextArea.setText("This database name does not exist. Try again!");
            return true;
        }

        resultTextArea.setText("Using " + databaseName + " database!");
        crtDatabase = myDBMS.getDatabaseByName(databaseName);
        return true;
    }

    public boolean ProcessInsertIntoTable(String sqlStmt) {
        // Define the regex pattern to match the INSERT INTO statement
        String insertPattern = "(insert into) (\\S+).*\\((.*?)\\).*(values).*\\((.*?)\\)(.*\\;?);";

        String tableName;
        List<String> columnNames;
        List<String> columnValues;

        // Compile the regex pattern and create a matcher
        Pattern pattern = Pattern.compile(insertPattern);
        Matcher matcher = pattern.matcher(sqlStmt);

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
        for (Table tb : tableList) {
            if (tb.getTableName().equals(tableName)) {
                crtTable = tb;
            }
        }

        for (String column : columnNames) {
            if (crtTable.getColumnByName(column) == null) {
                resultTextArea.setText("Column " + column + " does not exist in the " + crtTable.getTableName() + " table. Please try again.");
                return false;
            }
        }

        // Verify the primary key constraint
        String primaryKeys = null;
        String values = null;

        //Connecting to the database
        MongoDatabase database = mongoClient.getDatabase(crtDatabase.getDatabaseName());
        MongoCollection<Document> collection = database.getCollection(tableName);

        if (!isPrimaryKeyValid(crtTable, columnNames, primaryKeys, collection)) {
            return true;
        }

        for (int i = 0; i < columnNames.size(); i++) {
            if (crtTable.getPrimaryKeys().stream().map(primaryKey -> primaryKey.getPkAttribute().toLowerCase(Locale.ROOT)).toList()
                    .contains(columnNames.get(i).toLowerCase())) {
                if (primaryKeys == null) {
                    primaryKeys = columnValues.get(i);
                } else {
                    primaryKeys = primaryKeys + "#" + columnValues.get(i);
                }
            }
        }

        List<Column> columnList = crtTable.getColumns();
        int i = 0;
        boolean hasNulls = false;
        for (Column column : columnList) {
            if (!column.isPrimaryKey()) {
                if (!columnNames.stream().map(String::toLowerCase).toList()
                        .contains(column.getColumnName().toLowerCase()) && column.isNull()) {
                    //daca column nu este in columnNames si column permite nulls
                    hasNulls = true;
                    if (values == null) {
                        values = "null";
                    } else {
                        values = values + "#" + "null";
                    }
                } else if (!columnNames.stream().map(String::toLowerCase).toList()
                        .contains(column.getColumnName().toLowerCase()) && !column.isNull()) {
                    hasNulls = false;
                    break;
                } else {
                    if (values == null) {
                        values = columnValues.get(i);
                    } else {
                        values = values + "#" + columnValues.get(i);
                    }
                }
            }
            i++;
        }

        if (!isFieldValid(crtTable, columnNames, columnValues, hasNulls)) {
            return true;
        }
        if (!checkFKonInsert(crtTable, columnNames, columnValues, database)) {
            return true;
        }
        // insert indexes
        if (!insertInIndexes(crtTable, columnNames, columnValues, primaryKeys, database)) {
            return true;
        }

        try {
            collection.insertOne(new Document().append("_id", primaryKeys).append("values", values));

        } catch (MongoException me) {
            resultTextArea.setText("Unable to insert due to an error: " + me);
        }

        resultTextArea.setText("Data was successfully inserted into the table " + tableName);
        return true;
    }

    private boolean isPrimaryKeyValid(Table table, List<String> columnNames, String primaryKeyString, MongoCollection<Document> collection) {
        // Validation logic to check if the primary key constraint is valid
        // If the primary key constraint is valid, return true; otherwise, return false
        List<PrimaryKey> primaryKeys = table.getPrimaryKeys();
        for (PrimaryKey primaryKey : primaryKeys) {
            if (!columnNames.stream().map(String::toLowerCase).toList().contains(primaryKey.getPkAttribute().toLowerCase())) {
                resultTextArea.setText("Invalid list of columns. List of columns must contain all primary key fields.");
                return false;
            }
        }
        Document doc = collection.find(eq("_id", primaryKeyString)).first();
        if (doc != null) {
            resultTextArea.setText("Primary key violation: A record with the same primary key already exists.");
            return false;
        }

        return true;
    }

    private boolean isFieldValid(Table table, List<String> columnNames, List<String> columnValues, boolean hasNulls) {
        // Validation logic to check if the fields are valid and if they allow nulls
        // If the fields are valid, return true; otherwise, return false
        List<Column> columnList = table.getColumns();
        if ((columnList.size() != columnNames.size() || columnValues.size() != columnNames.size()) && !hasNulls) {
            resultTextArea.setText("Invalid list of columns. List of columns must contain all fields.");
            return false;
        } else {
            return true;
        }
    }

    private boolean checkFKonInsert(Table crtTable, List<String> columnNames, List<String> columnValues, MongoDatabase database) {
        if (crtTable.getForeignKeys().isEmpty()) {
            return true;
        }

        for (ForeignKey foreignKey : crtTable.getForeignKeys()) {
            for (int i = 0; i < columnNames.size(); i++) {
                if (columnNames.get(i).equalsIgnoreCase(foreignKey.getFkAttribute())) {
                    Table refTable = crtDatabase.getTableByName(foreignKey.getRefTable());
                    MongoCollection<Document> collection = database.getCollection(refTable.getTableName());
                    Document doc = collection.find(eq("_id", columnValues.get(i))).first();
                    if (doc != null) {
                        return true;
                    }
                    for (Index index : refTable.getIndexes()) {
                        collection = database.getCollection(index.getIndexName());
                        doc = collection.find(eq("_id", columnValues.get(i))).first();
                        if (doc != null) {
                            return true;
                        }
                    }
                }
            }
        }

        resultTextArea.setText("Foreign Key constraint failure.");
        return false;
    }

    private boolean insertInIndexes(Table crtTable, List<String> columnNames, List<String> columnValues, String primaryKeyString, MongoDatabase database) {
        for (Index index : crtTable.getIndexes()) {
            String indexID = null;
            for (String column : index.getColumns()) {
                for (int i = 0; i < columnNames.size(); i++) {
                    if (columnNames.get(i).equalsIgnoreCase(column)) {
                        if (indexID != null) {
                            indexID = indexID + "$" + columnValues.get(i);
                        } else {
                            indexID = columnValues.get(i);
                        }
                    }
                }
            }

            String mongoIndex = index.getIndexName() + "_" + crtTable.getTableName() + "_index";
            MongoCollection<Document> collection = database.getCollection(mongoIndex);
            Document doc = collection.find(eq("_id", indexID)).first();
            if (doc != null) {
                if (index.isUnique()) {
                    resultTextArea.setText("Unique key violation: A record with the same value already exists.");
                    return false;
                }
                String doc_values = doc.getString("values");
                if (crtTable.getPrimaryKeys().size() > 1) {
                    if (!doc_values.contains("$")) {
                        doc_values = doc_values.replace("#", "$");
                    }
                }
                String pkstring_index = primaryKeyString.replace("#", "$");
                Bson updates = Updates.combine(Updates.set("values", doc_values + "#" + pkstring_index));
                UpdateOptions options = new UpdateOptions().upsert(true);
                collection.updateOne(doc, updates, options);
            } else {
                collection.insertOne(new Document()
                        .append("_id", indexID)
                        .append("values", primaryKeyString));
            }
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
        for (Table tb : tableList) {
            if (tb.getTableName().equals(tableName)) {
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

        if (!checkFKonDelete(crtTable, columnPK, columnValue, database)) {
            return true;
        }
        deleteFromIndexes(crtTable, columnPK, columnValue, database);

        //Deleting a document
        collection.deleteOne(Filters.eq("_id", columnValue));

        resultTextArea.setText("Data was successfully deleted from the table " + tableName);
        return true;
    }

    private void deleteFromIndexes(Table crtTable, String columnName, String columnValue, MongoDatabase database) {
        for (Index index : crtTable.getIndexes()) {
            String mongoIndex = index.getIndexName() + "_" + crtTable.getTableName() + "_index";
            MongoCollection<Document> collection = database.getCollection(mongoIndex);
            Document doc = collection.find(eq("values", columnValue)).first();
            if (doc != null) {
                collection.deleteOne(Filters.eq("values", columnValue));
//                return;
            } else {
                String patternStr = "#" + columnValue;
                Pattern pattern = Pattern.compile(patternStr, Pattern.CASE_INSENSITIVE);
                Bson filter = Filters.regex("values", pattern);
                Document nonuniqueDoc = collection.find(filter).first();
                if (nonuniqueDoc != null) {
                    // Update the document to remove "#columnValue" from the "values" field
                    // Retrieve the existing values from the document and replace "#3" from it
                    String existingValue = nonuniqueDoc.getString("values");
                    String updatedValue = existingValue.replace("#" + columnValue, "");

                    collection.updateOne(filter, Updates.set("values", updatedValue));
                }
            }
        }
    }

    private boolean checkFKonDelete(Table crtTable, String columnName, String columnValue, MongoDatabase database) {
        for (Table table : crtDatabase.getTables()) {
            for (ForeignKey foreignKey : table.getForeignKeys()) {
                if (foreignKey.getRefTable().equalsIgnoreCase(crtTable.getTableName())) {
                    if (foreignKey.getRefAttribute().equalsIgnoreCase(columnName)) {
                        MongoCollection<Document> collection = database.getCollection(foreignKey.getRefTable());
                        Document doc = collection.find(eq("_id", columnValue)).first();
                        if (doc != null) {
                            resultTextArea.setText("Foreign Key constraint failure.");
                            return false;
                        }

//                        for (Index index : table.getIndexes()) {
//                            MongoCollection<Document> collection = database.getCollection(index.getIndexName());
//                            Document doc = collection.find(eq("_id", columnValue)).first();
//                            if (doc != null) {
//                                resultTextArea.setText("Foreign Key constraint failure.");
//                                return false;
//                            }
                    }
                }
            }
        }

        return true;
    }

    public void insertData(ActionEvent event) throws IOException {
        int n = 1000000;

        FileWriter file = new FileWriter("D:\\UNI\\MASTER AN 1\\ISGBD\\Produse.json");

        Gson gson = new Gson();

        // collection Produse
        for (int i = 1; i <= n; i++) {
            Random random = new Random();
            int tip = random.nextInt(2) + 1;
            double price = Math.round(Math.random() * 10000d) / 100d;
            String values = "nume_" + i + "#" + tip + "#" + price;
            Document doc = new Document().append("_id", String.valueOf(i)).append("values", values);

            gson.toJson(doc, file);

        }
        file.close();

        // collection nume_idx
        for (int i = 1; i <= n; i++) {
            String id = "nume_" + i;
            Document doc = new Document().append("_id", id).append("values", String.valueOf(i));

            gson.toJson(doc, file);

        }
        file.close();

        // collection nume_tip_idx
        for (int i = 1; i <= n; i++) {
            Random random = new Random();
            int tip = random.nextInt(2) + 1;
            String id = "nume_" + i + "$" + tip;
            Document doc = new Document().append("_id", id).append("values", String.valueOf(i));

            gson.toJson(doc, file);

        }
        file.close();


        resultTextArea.setText("Data successfully inserted;");
    }


    private boolean ProcessSelectFromTable() {
//        String selectPattern = "^\\s*(select)(\\s+)(distinct)?(\\s+)?((?:\\w+\\.\\w+|[\\w\\*]+(?:\\s*,\\s*\\w+\\.\\w+|\\s*,\\s*[\\w\\*]+)*))(\\s+)(from)(\\s+)(\\w+\\s*(?:,\\s*\\w+\\s*)*)(\\s*)(?:(where)(\\s+)((.|\\n)*))?;";
                                                                      //group(5)                                                                        //group(9)
        String selectPattern = "^\\s*(select)(\\s+)(distinct)?(\\s+)?((?:\\w+\\.\\w+|\\w+|\\*)(?:\\s*,\\s*(?:\\w+\\.\\w+|\\w+|\\*))*)(\\s+)(from)(\\s+)((?:\\w+\\s*(?:,\\s*\\w+\\s*)*))(?:\\s*(?:(inner|left|right)?\\s+(join)\\s+(\\w+)\\s+(on)\\s+(\\w+\\.\\w+=\\w+\\.\\w+)))?(\\s*)(?:(where)(\\s+)((.|\\n)*))?;";


        Pattern pattern = Pattern.compile(selectPattern, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(sqlField.getText());

        if (!matcher.matches()) {
            return false;
        }

        if (crtDatabase == null) {
            resultTextArea.setText("Please select a database to use first!");
            return true;
        }

        // is distinct
        String isDistinctString = matcher.group(3);
        boolean isDistinct = false;
        if (isDistinctString != null) {
            isDistinct = true;
        }

        // tables in which to search
        String tablesInWhichToSearch = matcher.group(9);
        if (tablesInWhichToSearch != null) {
            tablesInWhichToSearch = tablesInWhichToSearch.trim();
        }
        List<String> tablesInWhichToSearchList = Arrays.stream(tablesInWhichToSearch.split(",")).map(s -> s.trim()).collect(Collectors.toList());

        // columns to show
        String columnsToShow = matcher.group(5);
        if (columnsToShow != null) {
            columnsToShow = columnsToShow.trim();
        }
        List<String> columnsToShowAux = Arrays.stream(columnsToShow.split(",")).map(s -> s.trim()).toList();
        List<Pair<String, String>> columnAndTableList = new ArrayList<>();
        for (String column : columnsToShowAux) {
            if (column.contains(".")) {
                List<String> columnAndTable = List.of(column.split("\\."));
                columnAndTableList.add(new Pair<>(columnAndTable.get(0), columnAndTable.get(1)));
            } else {
                columnAndTableList.add(new Pair<>(column.trim(), tablesInWhichToSearchList.get(0)));
            }
        }

//        List<String> copy = tablesInWhichToSearchList;

        // join tables
        String joinTable = matcher.group(12);
        if (joinTable != null) {
            joinTable = joinTable.trim();
            tablesInWhichToSearchList.add(joinTable);
        }
        // ON condition
        String onCond = matcher.group(14);
        if (onCond != null) {
            onCond = onCond.trim();
        }

        // where clauses
        String whereClause = matcher.group(18);
        if (whereClause != null) {
            whereClause = whereClause.trim();
        }

        for (String tableName : tablesInWhichToSearchList) {
            Table crtTable = crtDatabase.getTableByName(tableName);
            if (crtTable == null) {
                resultTextArea.setText("Table " + tableName + " does not exist in the " + crtDatabase.getDatabaseName() + " database.");
                return true;
            }
        }

        MongoDatabase database = mongoClient.getDatabase(crtDatabase.getDatabaseName());

        List<Document> resultDocuments = null;
        List<Document> resultDocumentsList = null;
        List<Document[]> resultDocumentsJoin = null;
        if (joinTable == null && onCond == null && whereClause != null && !whereClause.trim().isEmpty()) {
            resultDocumentsList = ExecuteMultipleWhereCondition(whereClause, tablesInWhichToSearchList, database);
            DisplayQueryResults(resultDocumentsList, columnAndTableList, isDistinct);
        } else if (joinTable != null && onCond != null) {
            //NOT FINAL
            resultDocumentsJoin = executeJoinOperation(joinTable, onCond, whereClause, tablesInWhichToSearchList, database);
            DisplayJoinResults(resultDocumentsJoin);
        } else {
            for (String tableName : tablesInWhichToSearchList) {
                MongoCollection<Document> collection = database.getCollection(tableName);
                resultDocuments = collection.find().into(new ArrayList<>());

                if (resultDocumentsList == null) {
                    resultDocumentsList = resultDocuments;
                } else {
                    resultDocumentsList.addAll(resultDocuments);
                }
            }
            DisplayQueryResults(resultDocumentsList, columnAndTableList, isDistinct);
        }

//        // Display query results
//        DisplayQueryResults(resultDocumentsList, columnAndTableList, isDistinct);

        return true;
    }

    //NOT FINAL
    private List<Document[]> executeJoinOperation(String joinTable, String onCond, String whereClause, List<String> tablesInWhichToSearchList, MongoDatabase database) {
        List<Document> documents;
        List<Document> finalDocuments = new ArrayList<>();

        List<List<Document>> records = new ArrayList<>();
        List<Document> docs1 = new ArrayList<>();
        List<Document> docs2 = new ArrayList<>();

        // if oncond has index use index-nested-loop-join else hash-join
        // process ONCOND
        String[] parts = onCond.split("=");
        List<Document> resultDocuments = new ArrayList<>();
        int i = 1; //starting after the id column

        if (parts.length == 2) {
            String columnNameAndTable0 = parts[0].trim();
            String columnNameAndTable1 = parts[1].trim();
            String columnName0 = null, columnName1 = null;
            String tableName0 = null, tableName1 = null;

            if (columnNameAndTable0.contains(".")) {
                tableName0 = columnNameAndTable0.split("\\.")[0];
                columnName0 = columnNameAndTable0.split("\\.")[1];
            }
            if (columnNameAndTable1.contains(".")) {
                tableName1 = columnNameAndTable1.split("\\.")[0];
                columnName1 = columnNameAndTable1.split("\\.")[1];
            }

            Table crtTable = crtDatabase.getTableByName(tableName0);
//            boolean thereIsIndex = false;
//            for (Index index : crtTable.getIndexes()) {
//                if (index.getColumns().get(0).equalsIgnoreCase(columnName0)) {
                    List<String> collectionNames = database.listCollectionNames().into(new ArrayList<>());
                    // Filter collections based on a regex pattern
                    String regexPattern = "^\\w*" + Pattern.quote(columnName0.toLowerCase(Locale.ROOT)) + "\\w*(_index)?";
                    Pattern pattern = Pattern.compile(regexPattern);
                    List<String> filteredCollections = collectionNames.stream()
                            .filter(collectionName -> pattern.matcher(collectionName).matches())
                            .collect(Collectors.toList());
                    MongoCollection<Document> firstCol = database.getCollection(filteredCollections.get(0));

                    // Fetch all documents from the Produse collection
                    List<Document> firstDocuments = firstCol.find().into(new ArrayList<>());
                    List<Column> allColumns = crtTable.getColumns();

                    while (!allColumns.isEmpty()){
                        if (allColumns.get(i).getColumnName().equals(columnName0)){
                           break;
                        }
                        i++;
                    }

                    MongoCollection<Document> secondCol = database.getCollection(tableName1);
                    // For each Produse document, retrieve the related document from Tipuri collection
                    for (Document firstDoc : firstDocuments) {
                        String value = null;
                        Object tipValue = firstDoc.get("_id");
                        if (tipValue.toString().contains("$")){
                            String[] tipValues = tipValue.toString().split("\\$");
                            value = tipValues[i-1];
                        }

                        if (value != null) {
                            // Query the Tipuri collection to find the related document based on the custom ID field
                            Document secondDoc = secondCol.find(Filters.eq("_id", value)).first();

                            // Handle the retrieved Tipuri document according to your needs
                            if (secondDoc != null) {
                                // Handle the found Tipuri document
//                                System.out.println("Found related document in Tipuri collection: " + secondDoc.toJson());
                                docs1.add(firstDoc);

                            }
                        }
                    }
                docs2.addAll(secondCol.find().into(new ArrayList<>()));
                }
//            }
//        }



//        for (String table : tablesInWhichToSearchList) {
//            MongoCollection<Document> collection = database.getCollection(table);
//            List<Document> records1 = collection.find().into(new ArrayList<>());
//            records.add(records1);
//        }


        records.add(docs1);
        records.add(docs2);

        //hashJoin with 2 tables(TO DO for more than 2)
        List<Document[]> finaly = hashJoin(records.get(0), records.get(1), i);
//        System.out.println(finaly);


        if (whereClause != null && !whereClause.trim().isEmpty()) {
            finalDocuments = ExecuteMultipleWhereCondition(whereClause, tablesInWhichToSearchList, database);
        }

//        return finalDocuments;
        return finaly;
    }

    private List<Document[]> hashJoin(List<Document> records1, List<Document> records2, int idx) {
        List<Document[]> result = new ArrayList<>();
        Map<String, List<Document>> map = new HashMap<>();

        for (Document record : records2) {
            List<Document> v = map.getOrDefault(record.getString("_id"), new ArrayList<>());
            v.add(record);
//            String value = null;
//            if (record.getString("_id").contains("$")){
//                String[] recordValues = record.getString("_id").split("\\$");
//                value = recordValues[idx-1];
//            }
            map.put(record.getString("_id"), v);
        }

        Set<String> keys = map.keySet();
        // Converting HashSet to Array
//        String[] arrayKeys = keys.toArray(new String[keys.size()]);
//        int i=0;
        for (Document record : records1) {
            String value = null;
            if (record.getString("_id").contains("$")){
                String[] recordValues = record.getString("_id").split("\\$");
                value = recordValues[idx-1];
            }
            List<Document> lst = map.get(value);
            if (lst != null) {
                lst.stream().forEach(r -> {
                    result.add(new Document[]{r, record});
                });
            }
        }

        return result;
    }

    private List<Document> ExecuteMultipleWhereCondition(String whereClause, List<String> tablesInWhichToSearchList, MongoDatabase database) {
        List<String> whereClauses = List.of(whereClause.split(" and "));
        List<Document> documents;
        List<Document> finalDocuments = new ArrayList<>();
        for (String clause : whereClauses) {
            documents = ExecuteWhereCondition(clause, tablesInWhichToSearchList, database);
            if (finalDocuments.isEmpty()) {
                finalDocuments = documents;
            } else {
                finalDocuments = finalDocuments.stream()
                        .distinct()
                        .filter(documents::contains)
                        .collect(Collectors.toSet()).stream().toList();
            }
        }

        return finalDocuments;
    }

    private List<Document> ExecuteWhereCondition(String whereClause, List<String> tablesInWhichToSearchList, MongoDatabase database) {
        // Implement logic to parse and execute WHERE conditions
        String condition = null;
        if (whereClause.contains("=")) {
            condition = "=";
        }
        if (whereClause.contains("like")) {
            condition = "like";
        }
        if (whereClause.contains("<")) {
            condition = "<";
        }
        if (whereClause.contains(">")) {
            condition = ">";
        }
        if (whereClause.contains("<=")) {
            condition = "<=";
        }
        if (whereClause.contains(">=")) {
            condition = ">=";
        }
        String[] parts = whereClause.split(condition);
        List<Document> resultDocuments = new ArrayList<>();

        if (parts.length == 2) {
            String columnNameAndTable = parts[0].trim();
            String value = parts[1].trim().replaceAll("'", "");
            String columnName;
            String tableName;

            if (columnNameAndTable.contains(".")) {
                tableName = columnNameAndTable.split(".")[0];
                columnName = columnNameAndTable.split(".")[1];
            } else {
                columnName = columnNameAndTable;
                tableName = tablesInWhichToSearchList.get(0);
            }

            Table crtTable = crtDatabase.getTableByName(tableName);
            List<Document> docs = new ArrayList<>();
//            FindIterable<Document> sortedDocs;
            boolean thereIsIndex = false;
            for (Index index : crtTable.getIndexes()) {
                if (index.getColumns().get(0).equalsIgnoreCase(columnName)) {
                    MongoCollection<Document> collection = database.getCollection(columnName + "_" + tableName + "_index");
//                    sortedDocs = collection.find().sort(ascending(columnName));
                    switch (condition) {
                        case "=":
                            docs.addAll(collection.find(Filters.eq("_id", value)).into(new ArrayList<>()));
//                            for (Document doc : sortedDocs) {
//                                if (doc.get("_id").equals(value)) {
//                                    docs.add(doc);
//                                }
//                            }
                            break;
                        case "like":
                            if (value.contains("%")) {
                                int percentIndex = value.indexOf('%');

                                String startValue;
                                String endValue;

                                if (percentIndex != -1) {
                                    startValue = value.substring(0, percentIndex);
                                    endValue = value.substring(percentIndex + 1);
                                } else {
                                    // If '%' is not found, the whole string is considered as startValue
                                    startValue = value;
                                    endValue = "";
                                }

                                Pattern startPattern = Pattern.compile("^" + Pattern.quote(startValue), Pattern.CASE_INSENSITIVE);
                                Pattern endPattern = Pattern.compile(Pattern.quote(endValue) + "$", Pattern.CASE_INSENSITIVE);

                                if (startValue.isEmpty() && !endValue.isEmpty()) {
                                    docs.addAll(collection.find(regex("_id", endPattern)).into(new ArrayList<>()));
                                } else if (!startValue.isEmpty() && endValue.isEmpty()) {
                                    docs.addAll(collection.find(regex("_id", startPattern)).into(new ArrayList<>()));
                                }

                            }
                            break;
                        case "<":
                            docs.addAll(collection.find(Filters.lt("_id", value)).into(new ArrayList<>()));
                            break;
                        case ">":
                            docs.addAll(collection.find(Filters.gt("_id", value)).into(new ArrayList<>()));
                            break;
                        case "<=":
                            docs.addAll(collection.find(Filters.lte("_id", value)).into(new ArrayList<>()));
                            break;
                        case ">=":
                            docs.addAll(collection.find(Filters.gte("_id", value)).into(new ArrayList<>()));
                            break;
                    }
                    thereIsIndex = true;
                }
            }
            Map<String, String> columnValueMap;
            if (!thereIsIndex) {
                MongoCollection<Document> collection = database.getCollection(tableName);
                for (Document document : collection.find()) {
                    columnValueMap = getColumnValueMap(document, crtTable);
                    if (columnValueMap.get(columnName) != null) {
                        switch (condition) {
                            case "=":
                                if (columnValueMap.get(columnName).equalsIgnoreCase(value)) {
                                    resultDocuments.add(document);
                                }
                                break;
                            case "like":
                                if (value.contains("%")) {
                                    int percentIndex = value.indexOf('%');

                                    String startValue;
                                    String endValue;

                                    if (percentIndex != -1) {
                                        startValue = value.substring(0, percentIndex);
                                        endValue = value.substring(percentIndex + 1);
                                    } else {
                                        startValue = value;
                                        endValue = "";
                                    }

                                    Pattern startPattern = Pattern.compile("^" + Pattern.quote(startValue), Pattern.CASE_INSENSITIVE);
                                    Pattern endPattern = Pattern.compile(Pattern.quote(endValue) + "$", Pattern.CASE_INSENSITIVE);

                                    if (startValue.isEmpty() && !endValue.isEmpty()) {
                                        if (columnValueMap.get(columnName).matches(String.valueOf(endPattern))) {
                                            resultDocuments.add(document);
                                        }
                                    } else if (!startValue.isEmpty() && endValue.isEmpty()) {
                                        if (columnValueMap.get(columnName).matches(String.valueOf(startPattern))) {
                                            resultDocuments.add(document);
                                        }
                                    }

                                }
                                break;
                            case "<":
                                if (columnValueMap.get(columnName).matches("[-+]?\\d+") && value.matches("[-+]?\\d+")) {
                                    int columnValue = Integer.parseInt(columnValueMap.get(columnName));
                                    int inputValue = Integer.parseInt(value);
                                    if (columnValue < inputValue) {
                                        resultDocuments.add(document);
                                    }
                                } else if (columnValueMap.get(columnName).matches("-?\\d+(\\.\\d+)?") && value.matches("-?\\d+(\\.\\d+)?")) {
                                    Double columnValue = Double.parseDouble(columnValueMap.get(columnName));
                                    Double inputValue = Double.parseDouble(value);
                                    if (columnValue < inputValue) {
                                        resultDocuments.add(document);
                                    }
                                }
                                break;
                            case ">":
                                if (columnValueMap.get(columnName).matches("[-+]?\\d+") && value.matches("[-+]?\\d+")) {
                                    int columnValue = Integer.parseInt(columnValueMap.get(columnName));
                                    int inputValue = Integer.parseInt(value);
                                    if (columnValue > inputValue)
                                        resultDocuments.add(document);
                                } else if (columnValueMap.get(columnName).matches("-?\\d+(\\.\\d+)?") && value.matches("-?\\d+(\\.\\d+)?")) {
                                    Double columnValue = Double.parseDouble(columnValueMap.get(columnName));
                                    Double inputValue = Double.parseDouble(value);
                                    if (columnValue > inputValue) {
                                        resultDocuments.add(document);
                                    }
                                }
                                break;
                            case "<=":
                                if (columnValueMap.get(columnName).matches("[-+]?\\d+") && value.matches("[-+]?\\d+")) {
                                    int columnValue = Integer.parseInt(columnValueMap.get(columnName));
                                    int inputValue = Integer.parseInt(value);
                                    if (columnValue <= inputValue)
                                        resultDocuments.add(document);
                                } else if (columnValueMap.get(columnName).matches("-?\\d+(\\.\\d+)?") && value.matches("-?\\d+(\\.\\d+)?")) {
                                    Double columnValue = Double.parseDouble(columnValueMap.get(columnName));
                                    Double inputValue = Double.parseDouble(value);
                                    if (columnValue <= inputValue) {
                                        resultDocuments.add(document);
                                    }
                                }
                                break;
                            case ">=":
                                if (columnValueMap.get(columnName).matches("[-+]?\\d+") && value.matches("[-+]?\\d+")) {
                                    int columnValue = Integer.parseInt(columnValueMap.get(columnName));
                                    int inputValue = Integer.parseInt(value);
                                    if (columnValue >= inputValue)
                                        resultDocuments.add(document);
                                } else if (columnValueMap.get(columnName).matches("-?\\d+(\\.\\d+)?") && value.matches("-?\\d+(\\.\\d+)?")) {
                                    Double columnValue = Double.parseDouble(columnValueMap.get(columnName));
                                    Double inputValue = Double.parseDouble(value);
                                    if (columnValue >= inputValue) {
                                        resultDocuments.add(document);
                                    }
                                }
                                break;
                        }
                    }
                }
            }

            if (thereIsIndex) {
                for (Document doc : docs) {
                    MongoCollection<Document> collection = database.getCollection(tableName);
                    resultDocuments.addAll(collection.find(eq("_id", doc.get("values"))).into(new ArrayList<>()));
                }
            }
        }

        return resultDocuments;
    }

    private Map<String, String> getColumnValueMap(Document document, Table crtTable) {
        Map<String, String> ColumnValueMap = new LinkedHashMap<>();
        int index = 0;
        for (PrimaryKey pk : crtTable.getPrimaryKeys()) {
            ColumnValueMap.put(pk.getPkAttribute(), document.get("_id").toString().split("#")[index]);
            index++;
        }
        index = 0;
        for (Column col : crtTable.getColumns()) {
            if (ColumnValueMap.get(col.getColumnName()) == null) {
                ColumnValueMap.put(col.getColumnName(), document.get("values").toString().split("#")[index]);
                index++;
            }
        }

        return ColumnValueMap;
    }


    private void DisplayQueryResults(List<Document> resultDocuments, List<Pair<String, String>> selectedColumns, boolean isDistinct) {
        // Implement logic to display query results
        StringBuilder resultStringBuilder = new StringBuilder();
        List<String> result = new ArrayList<>();

        if (resultDocuments == null || resultDocuments.isEmpty()) {
            resultTextArea.setText("No records found!");
            return;
        }

        for (Document document : resultDocuments) {
            // Display selected columns
            resultStringBuilder = new StringBuilder();
            for (Pair<String, String> column : selectedColumns) {
                Table crtTable = crtDatabase.getTableByName(column.getValue());

                Map<String, String> columnValueMap = getColumnValueMap(document, crtTable);
                if (!column.getKey().trim().equals("*")) {
                    for (Map.Entry<String, String> entry : columnValueMap.entrySet()) {
                        if (Objects.equals(entry.getKey(), column.getKey())) {
                            resultStringBuilder.append(entry.getKey()).append(": ").append(entry.getValue()).append("; ");
                        }
                    }
                } else {
                    for (Map.Entry<String, String> entry : columnValueMap.entrySet()) {
                        resultStringBuilder.append(entry.getKey()).append(": ").append(entry.getValue()).append("; ");
                    }
                }
            }
            resultStringBuilder.setLength(resultStringBuilder.length() - 2); // Remove trailing comma and space
            resultStringBuilder.append("\n");
            result.add(resultStringBuilder.toString());
        }

        if (isDistinct) {
            result = result.stream().distinct().toList();
        }

        resultTextArea.setText(result.toString()
                .replace("[", "")
                .replace("]", "")
                .replace(", ", ""));
    }

    //NOT FINAL
    private void DisplayJoinResults(List<Document[]> resultDocuments) {
        // Implement logic to display query results
        StringBuilder resultStringBuilder = new StringBuilder();
        List<String> result = new ArrayList<>();

        if (resultDocuments == null || resultDocuments.isEmpty()) {
            resultTextArea.setText("No records found!");
            return;
        }

        for (Document[] documents : resultDocuments) {
                Document document = documents[1];
                String id = document.get("_id").toString();
                String values = document.get("values").toString();
                String formattedIDResult = null;
                String formattedValueResult = null;
                String formattedResult = null;

                if (id.contains("$")){
                    String[] idSplit = id.split("\\$");
                    formattedIDResult = " name:" + idSplit[0] +
                            " tip:" + idSplit[1];
                } else formattedIDResult = "id:" + id;
                if (values.contains("#")) {
                    String[] valuesSplit = values.split("#");
                    formattedValueResult = " name:" + valuesSplit[0] +
                            " tip:" + valuesSplit[1];
                } else formattedValueResult = "id:" + values;

                formattedResult = formattedValueResult + formattedIDResult + "\n";
                resultStringBuilder.append(formattedResult);
            }
            resultStringBuilder.append("\n");
//        }

        resultStringBuilder.setLength(resultStringBuilder.length() - 2);
        result.add(resultStringBuilder.toString());

        String formattedResultString = resultStringBuilder.toString().trim();
        resultTextArea.setText(formattedResultString);
    }


}
