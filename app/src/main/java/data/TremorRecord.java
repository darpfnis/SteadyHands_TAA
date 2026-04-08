package data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Сутність для збереження даних про стабільність рук (тремор).
 * Використовується Room для запису в локальну базу даних.
 */
@Entity(tableName = "tremor_stats")
public class TremorRecord {

    @PrimaryKey(autoGenerate = true)
    public int id;

    // Точна мілісекунда запису (System.currentTimeMillis())
    public long timestamp;

    // Значення балу стабільності (від 0.0 до 10.0) або амплітуда
    public float amplitude;

    /**
     * Тип запису для правильної фільтрації в аналітиці:
     * 0 - ФОНОВИЙ (автоматична фіксація під час користування)
     * 1 - ТЕСТ (активне тестування, наприклад "Склянка")
     * 2 - ВПРАВА (запис результату після дихальної гімнастики)
     */
    public int recordType;

    // Порожній конструктор необхідний для Room
    public TremorRecord() {
    }

    // Конструктор для зручного створення об'єкта в коді
    public TremorRecord(long timestamp, float amplitude, int recordType) {
        this.timestamp = timestamp;
        this.amplitude = amplitude;
        this.recordType = recordType;
    }

    // Геттери та сеттери (необов'язково для Room, якщо поля public,
    // але корисно для гарного стилю коду)
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public float getAmplitude() { return amplitude; }
    public void setAmplitude(float amplitude) { this.amplitude = amplitude; }

    public int getRecordType() { return recordType; }
    public void setRecordType(int recordType) { this.recordType = recordType; }
}