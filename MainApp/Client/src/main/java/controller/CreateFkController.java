package controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import model.*;

import java.util.List;

import static utils.Utils.saveDBMSToXML;

public class CreateFkController {
    @FXML
    private TextField dbNameField;
    @FXML
    private TextField tbNameField;
    @FXML
    private ComboBox<String> currentColumnName;
    @FXML
    private ComboBox<String> tableComboBox;
    @FXML
    private ComboBox<String> columnComboBox;

    private TreeView<String> mainTreeView;
    private Databases myDBMS;
    private DataBase crtDatabase;
    private TextArea resultTextArea;
    private String tbName;

    public void setAttr(TreeView<String> mainTreeView, Databases myDBMS, DataBase crtDatabase, String tbName, TextArea resultTextArea) {
        this.mainTreeView = mainTreeView;
        this.myDBMS = myDBMS;
        this.crtDatabase = crtDatabase;
        this.tbName = tbName;
        this.resultTextArea = resultTextArea;
        init();
    }

    private ObservableList<String> tableComboBoxItems;
    private ObservableList<String> columnComboBoxItems;
    private ObservableList<String> currentComboBoxItems;

    public void init() {
        tableComboBoxItems = FXCollections.observableArrayList();

        List<Table> tables = crtDatabase.getTables();
        for (Table tb: tables) {
            if (tb.getTableName().equals(tbName)){
                List<Column> columns = tb.getColumns();

                // Create a new observable list for each table
                currentComboBoxItems = FXCollections.observableArrayList();

                for (Column cl: columns) {
                    currentComboBoxItems.add(cl.getColumnName());
                }
            } else {
                tableComboBoxItems.add(tb.getTableName());
            }
        }

        // Set the items in the ComboBox
        currentColumnName.setItems(currentComboBoxItems);
        tableComboBox.setItems(tableComboBoxItems);

        tbNameField.setText(tbName);
        dbNameField.setText(crtDatabase.getDatabaseName());
    }

    public void isSelected(ActionEvent event){
        columnComboBoxItems = FXCollections.observableArrayList();

        List<Table> tables = crtDatabase.getTables();
        for (Table tb: tables) {
            if (tb.getTableName().equals(tableComboBox.getValue())){
                List<Column> columns = tb.getColumns();

                for (Column cl: columns) {
                    columnComboBoxItems.add(cl.getColumnName());
                }
            }
        }

        columnComboBox.setItems(columnComboBoxItems);
    }

    public void addForeignKey(ActionEvent event) {
        List<Table> tables = crtDatabase.getTables();

        String fkAttribute = currentColumnName.getValue();
        String refTable = tableComboBox.getValue();
        String refAttribute = columnComboBox.getValue();

        ForeignKey newFK = new ForeignKey(fkAttribute, refTable, refAttribute);

        for (Table tb: tables) {
            if (tb.getTableName().equals(tbName)){
                tb.createForeignKey(newFK);
            }
        }

        saveDBMSToXML(myDBMS);
        resultTextArea.setText("FK between " + tbName + " and " + refTable + " created successfully!");

        // Close the dialog
        ((Stage) dbNameField.getScene().getWindow()).close();
    }
}
