package model;

import javax.xml.bind.annotation.*;
import java.io.Serializable;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD )
@XmlRootElement(name = "IndexFiles")
@XmlType(propOrder = { "indexName", "fileName", "tableName", "columns" })
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


    public Index() {}

    public Index(String indexName, String tableName, List<String> columns) {
        this.indexName = indexName;
        this.tableName = tableName;
        this.columns = columns;
        this.fileName = indexName + ".ind";
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
}