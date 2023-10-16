package model;

import javax.xml.bind.annotation.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD )
@XmlRootElement(name = "tables")
@XmlType(name = "tables", propOrder = { "tableName", "fileName", "columns", "primaryKeys", "foreignKeys", "indexes" })
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
        this.foreignKeys = new ArrayList<>();
        this.indexes = new ArrayList<>();
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

