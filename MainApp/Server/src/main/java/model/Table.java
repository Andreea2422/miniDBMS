package model;

import javax.xml.bind.annotation.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD )
@XmlRootElement(name = "tables")
@XmlType(name = "tables", propOrder = { "tableName", "fileName", "columns", "primaryKeys", "uniqueKeys", "foreignKeys", "indexes" })
public class Table implements Serializable {
    @XmlAttribute
    private String tableName;
    @XmlAttribute
    private String fileName;
    @XmlElement(name = "attribute")
    @XmlElementWrapper(name = "structure")
    private List<Column> columns;
    @XmlElement(name = "pkAttribute")
    @XmlElementWrapper(name = "primaryKey")
    private List<PrimaryKey> primaryKeys;
    @XmlElement(name = "UniqueAttribute")
    @XmlElementWrapper(name = "uniqueKeys")
    private List<UniqueKey> uniqueKeys;
    @XmlElement(name = "foreignKey")
    @XmlElementWrapper(name = "foreignKeys")
    private List<ForeignKey> foreignKeys;
    @XmlElement(name = "IndexFile")
    @XmlElementWrapper(name = "IndexFiles")
    private List<Index> indexes;

    public Table() {
    }

    public Table(String name) {
        this.tableName = name;
        this.columns = new ArrayList<>();
        this.primaryKeys = new ArrayList<>();
        this.uniqueKeys = new ArrayList<>();
        this.foreignKeys = new ArrayList<>();
        this.indexes = new ArrayList<>();
    }

    public Table(String tableName, List<Column> columns, List<PrimaryKey> primaryKeys, List<UniqueKey> uniqueKeys, List<ForeignKey> foreignKeys) {
        this.tableName = tableName;
        this.fileName =  tableName + ".bin" ;
        this.columns = columns;
        this.primaryKeys = primaryKeys;
        this.uniqueKeys = uniqueKeys;
        this.foreignKeys = foreignKeys;
        this.indexes = new ArrayList<>();
    }

    public void createIndex(Index newIndex) {
        indexes.add(newIndex);
    }

    public void createForeignKey(ForeignKey newfk) {
        foreignKeys.add(newfk);
    }

    public void dropIndex(Index index) { indexes.remove(index); }

    public Column getColumnByName(String name) {
        for (Column column: this.columns) {
            if (column.getColumnName().equalsIgnoreCase(name)) {
                return column;
            }
        }
        return null;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public List<Column> getColumns() {
        return columns;
    }

    public void setColumns(List<Column> columns) {
        this.columns = columns;
    }

    public List<PrimaryKey> getPrimaryKeys() {
        return primaryKeys;
    }

    public void setPrimaryKeys(List<PrimaryKey> primaryKeys) {
        this.primaryKeys = primaryKeys;
    }

    public List<UniqueKey> getUniqueKeys() {
        return uniqueKeys;
    }

    public void setUniqueKeys(List<UniqueKey> uniqueKeys) {
        this.uniqueKeys = uniqueKeys;
    }

    public List<ForeignKey> getForeignKeys() {
        return foreignKeys;
    }

    public void setForeignKeys(List<ForeignKey> foreignKeys) {
        this.foreignKeys = foreignKeys;
    }

    public List<Index> getIndexes() {
        return indexes;
    }

    public void setIndexes(List<Index> indexes) {
        this.indexes = indexes;
    }
}

