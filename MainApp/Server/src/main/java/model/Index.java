package model;

import javax.xml.bind.annotation.*;
import java.io.Serializable;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD )
@XmlRootElement(name = "IndexFiles")
@XmlType(propOrder = { "indexName", "fileName", "tableName", "columns", "isUnique" })
public class Index implements Serializable {
    @XmlAttribute
    private String indexName;
    @XmlAttribute
    private String fileName;
    @XmlAttribute
    private String tableName;
    @XmlElement(name = "IAttribute")
    @XmlElementWrapper(name = "IndexAttributes")
    private List<String> columns;
    @XmlAttribute
    private boolean isUnique;


    public Index() {}

    public Index(String indexName, String tableName, List<String> columns, boolean isUnique) {
        this.indexName = indexName;
        this.tableName = tableName;
        this.columns = columns;
        this.fileName = indexName + ".ind";
        this.isUnique = isUnique;
    }


    public String getIndexName() {
        return indexName;
    }

    public void setIndexName(String indexName) {
        this.indexName = indexName;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public List<String> getColumns() {
        return columns;
    }

    public void setColumns(List<String> columns) {
        this.columns = columns;
    }

    public boolean isUnique() {
        return isUnique;
    }

    public void setUnique(boolean unique) {
        isUnique = unique;
    }
}
