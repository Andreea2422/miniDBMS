package controller;


import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import model.*;

import java.io.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private Databases myDBMS;
    private DataBase crtDatabase;

    public void setDatabases(Databases myDBMS) {
        this.myDBMS = myDBMS;
        init();
    }

    public void refreshTree(ActionEvent event){
        init();
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

            dialogStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void dropDatabaseFromContext(ActionEvent event){
        TreeItem<String> selectedItem = mainTreeView.getSelectionModel().getSelectedItem();
        String databaseName = selectedItem.getValue();

        List<DataBase> databaseList = myDBMS.listDatabases();
        if (!databaseList.stream().map(database -> database.getDatabaseName().toLowerCase()).toList().contains(databaseName.toLowerCase())) {
            resultTextArea.setText("This database name does not exist. Try again!");
            return;
        }

        DropDatabase(databaseName);
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
        if (!tableList.stream().map(table -> table.getTableName().toLowerCase()).toList().contains(tableName.toLowerCase())) {
            resultTextArea.setText("Table name " + tableName +" do not exist in " + crtDatabase.getDatabaseName() +" database. Try again!");
            return;
        }

        DropTable(tableName);
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
            resultTextArea.setText("This database name do not exist. Try again!");
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
            resultTextArea.setText("This database name already exist. Try again!");
            return true;
        }

        CreateDatabase(databaseName);
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
            resultTextArea.setText("This database name do not exist. Try again!");
            return true;
        }

        DropDatabase(databaseName);
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
            resultTextArea.setText("Table name " + tableName +" do not exist in " + crtDatabase.getDatabaseName() +" database. Try again!");
            return true;
        }

        DropTable(tableName);
        resultTextArea.setText("Table " + tableName + " was dropped!");
        return true;
    }

    public void CreateDatabase(String databaseName) {
        myDBMS.createDatabase(databaseName);
        saveDBMSToXML(myDBMS);
    }

    public void DropDatabase(String databaseName) {
        myDBMS.dropDatabase(databaseName);
        saveDBMSToXML(myDBMS);
    }

    public void DropTable(String tableName) {
        crtDatabase.dropTable(tableName);
        saveDBMSToXML(myDBMS);
    }
}
