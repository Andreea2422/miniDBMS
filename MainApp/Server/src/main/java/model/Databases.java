package model;

import javax.xml.bind.annotation.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD )
@XmlRootElement(name = "databases")
@XmlType(propOrder = { "databases" })
public class Databases implements Serializable {
    @XmlElement(name = "database")
    private List<DataBase> databases;

    public Databases(){
        this.databases = new ArrayList<>();
    }

    public List<DataBase> getDatabasesFunction() {
        return databases;
    }

    public void setDatabases(List<DataBase> databases) {
        this.databases = databases;
    }

    public void createDatabase(String name) {
        DataBase newDatabase = new DataBase(name);
        databases.add(newDatabase);
    }

    public void addDatabase(DataBase newDatabase) {
        databases.add(newDatabase);
    }

    public void dropDatabase(String name) {
        DataBase databaseToRemove = null;
        for (DataBase database : databases) {
            if (database.getDatabaseName().equalsIgnoreCase(name)) {
                databaseToRemove = database;
                break;
            }
        }
        if (databaseToRemove != null) {
            databases.remove(databaseToRemove);
        }
    }

    public List<DataBase> listDatabases() {
        return databases;
    }

    public DataBase getDatabaseByName(String name){
        for (DataBase database: this.databases) {
            if (database.getDatabaseName().equalsIgnoreCase(name)) {
                return database;
            }
        }
        return null;
    }
}

