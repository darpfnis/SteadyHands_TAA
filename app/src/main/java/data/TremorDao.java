package data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface TremorDao {

    @Insert
    void insert(TremorEntity tremor);

    // 1. Похвилинно за останню годину
    @Query("SELECT * FROM tremor_table WHERE timestamp > :fromTime ORDER BY timestamp ASC")
    List<TremorEntity> getLastHourData(long fromTime);

    // 2. Кожні 10 хвилин (середнє значення)
    // ДОДАНО: isTest у вибірку (беремо 0 як значення для згрупованих даних, бо це не індивідуальний тест)
    @Query("SELECT id, AVG(tremorValue) as tremorValue, (timestamp / 600000) * 600000 as timestamp, 0 as isTest " +
            "FROM tremor_table WHERE timestamp > :fromTime " +
            "GROUP BY (timestamp / 600000) ORDER BY timestamp ASC")
    List<TremorEntity> getTenMinuteAverages(long fromTime);

    // 3. Щогодини (середнє за кожну годину дня)
    // ДОДАНО: 0 as isTest (або false, залежно від версії SQLite, 0 відповідає false)
    @Query("SELECT id, AVG(tremorValue) as tremorValue, (timestamp / 3600000) * 3600000 as timestamp, 0 as isTest " +
            "FROM tremor_table WHERE timestamp > :fromTime " +
            "GROUP BY (timestamp / 3600000) ORDER BY timestamp ASC")
    List<TremorEntity> getHourlyAverages(long fromTime);

    @Query("DELETE FROM tremor_table")
    void deleteAll();

    @Query("SELECT * FROM tremor_table WHERE timestamp >= :start AND timestamp < :end ORDER BY timestamp ASC")
    List<TremorEntity> getMinuteData(long start, long end);

    @Query("SELECT * FROM tremor_table WHERE timestamp >= :start AND timestamp < :end ORDER BY timestamp ASC")
    List<TremorEntity> getAllDataForDay(long start, long end);
}