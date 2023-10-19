package model;

import java.io.Serializable;

public class PrimaryKey implements Serializable {
    private String pkAttribute;

    public PrimaryKey() {
    }

    public PrimaryKey(String pkAttribute) {
        this.pkAttribute = pkAttribute;
    }

    // Getter and Setter methods for the field

    public String getPkAttribute() {
        return pkAttribute;
    }

    public void setPkAttribute(String pkAttribute) {
        this.pkAttribute = pkAttribute;
    }
}
