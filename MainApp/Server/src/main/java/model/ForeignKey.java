package model;

import java.io.Serializable;

public class ForeignKey implements Serializable {
    private String fkAttribute;
    private String refTable;
    private String refAttribute;

    public ForeignKey() {
    }

    public ForeignKey(String fkAttribute, String refTable, String refAttribute) {
        this.fkAttribute = fkAttribute;
        this.refTable = refTable;
        this.refAttribute = refAttribute;
    }

    public String getFkAttribute() {
        return fkAttribute;
    }

    public void setFkAttribute(String fkAttribute) {
        this.fkAttribute = fkAttribute;
    }

    public String getRefTable() {
        return refTable;
    }

    public void setRefTable(String refTable) {
        this.refTable = refTable;
    }

    public String getRefAttribute() {
        return refAttribute;
    }

    public void setRefAttribute(String refAttribute) {
        this.refAttribute = refAttribute;
    }
}
