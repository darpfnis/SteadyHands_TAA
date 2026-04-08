package data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "tremor_table")
public class TremorEntity {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private float tremorValue;
    private long timestamp;

    // Нове поле для розрізнення фонового моніторингу та активних тестів
    private boolean isTest;

    // Конструктор
    public TremorEntity(float tremorValue, long timestamp, boolean isTest) {
        this.tremorValue = tremorValue;
        this.timestamp = timestamp;
        this.isTest = isTest;
    }

    // Геттери та сеттери
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public float getTremorValue() {
        return tremorValue;
    }

    public void setTremorValue(float tremorValue) {
        this.tremorValue = tremorValue;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isTest() {
        return isTest;
    }

    public void setTest(boolean test) {
        isTest = test;
    }
}