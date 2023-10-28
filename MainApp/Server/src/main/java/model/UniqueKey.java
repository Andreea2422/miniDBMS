package model;

import java.io.Serializable;

public class UniqueKey implements Serializable {
    private String UniqueAttribute;

    public UniqueKey() {
    }

    public UniqueKey(String UniqueAttribute) {
        this.UniqueAttribute = UniqueAttribute;
    }

    // Getter and Setter methods for the field

    public String getUkAttribute() {
        return UniqueAttribute;
    }

    public void setUkAttribute(String UniqueAttribute) {
        this.UniqueAttribute = UniqueAttribute;
    }
}
