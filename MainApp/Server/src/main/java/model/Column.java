package model;

import javax.xml.bind.annotation.*;
import java.io.Serializable;

@XmlAccessorType(XmlAccessType.FIELD )
@XmlRootElement(name = "column")
@XmlType(propOrder = { "columnName", "type", "isPrimaryKey", "isnull" })
public class Column implements Serializable {
    @XmlAttribute
    private String columnName;
    @XmlAttribute
    private String type;
    private boolean isPrimaryKey;
    @XmlAttribute
    private String isnull;

    public Column() {
    }

    public Column(String name, String type, boolean isPrimaryKey, String isnull) {
        this.columnName = name;
        this.type = type;
        this.isPrimaryKey = isPrimaryKey;
        this.isnull = isnull;
    }

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getNull() {
        return isnull;
    }

    public void setNull(String isnull) {
        this.isnull = isnull;
    }

    public boolean isPrimaryKey() {
        return isPrimaryKey;
    }

    public void setPrimaryKey(boolean primaryKey) {
        isPrimaryKey = primaryKey;
    }
}
