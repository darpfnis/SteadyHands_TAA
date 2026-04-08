package ui.helpers;

/**
 * Фільтр високих частот (High-Pass Filter).
 * Використовується для видалення гравітаційної складової та низькочастотних рухів,
 * залишаючи лише швидкі коливання (власне тремор).
 */
public class HighPassFilter {

    private float[] gravity = new float[3];
    private final float alpha;

    /**
     * Конструктор фільтра.
     * @param alpha Коефіцієнт фільтрації (зазвичай від 0.1 до 0.8).
     * Чим вище alpha, тим більше низьких частот відсікається.
     */
    public HighPassFilter(float alpha) {
        this.alpha = alpha;
        // Ініціалізація початкових значень гравітації
        gravity[0] = 0;
        gravity[1] = 0;
        gravity[2] = 0;
    }

    /**
     * Обробка нових значень сенсора (x, y, z).
     * @param values масив значень з SensorEvent.values
     * @return масив відфільтрованих значень (тільки високі частоти)
     */
    public float[] filter(float[] values) {
        float[] filteredValues = new float[3];

        // 1. Виділяємо низькочастотну складову (гравітацію/плавний рух)
        // Формула: gravity = alpha * gravity + (1 - alpha) * event.values
        gravity[0] = alpha * gravity[0] + (1 - alpha) * values[0];
        gravity[1] = alpha * gravity[1] + (1 - alpha) * values[1];
        gravity[2] = alpha * gravity[2] + (1 - alpha) * values[2];

        // 2. Віднімаємо низьку частоту від оригінального сигналу
        // Результат: тільки швидкі коливання (висока частота)
        filteredValues[0] = values[0] - gravity[0];
        filteredValues[1] = values[1] - gravity[1];
        filteredValues[2] = values[2] - gravity[2];

        return filteredValues;
    }

    /**
     * Скидання фільтра (корисно при зміні положення тіла або початку нового тесту)
     */
    public void reset() {
        gravity[0] = 0;
        gravity[1] = 0;
        gravity[2] = 0;
    }
}