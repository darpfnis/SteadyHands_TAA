package data;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {TremorEntity.class}, version = 3, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    public abstract TremorDao tremorDao();

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "tremor_database")
                            .fallbackToDestructiveMigration() // Видаляє стару БД при зміні версії
                            .allowMainThreadQueries() // Тільки для тестів/лаб, краще використовувати потоки
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}