package ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import ui.helpers.UIHelper;

/**
 * Спеціальний View для відображення візуального маркера стабільності.
 * Малює коло, яке реагує на тремор.
 */
public class OverlayMarker extends View {

    private Paint paintCircle;
    private Paint paintTarget;
    private float currentRadius = 100f;
    private float targetRadius = 50f;
    private int currentColor = Color.GREEN;

    public OverlayMarker(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        // Налаштування пензля для динамічного кола (тремор)
        paintCircle = new Paint();
        paintCircle.setAntiAlias(true);
        paintCircle.setStyle(Paint.Style.STROKE);
        paintCircle.setStrokeWidth(8f);

        // Налаштування пензля для центральної мішені (ідеал)
        paintTarget = new Paint();
        paintTarget.setAntiAlias(true);
        paintTarget.setStyle(Paint.Style.STROKE);
        paintTarget.setStrokeWidth(3f);
        paintTarget.setColor(Color.LTGRAY);
    }

    /**
     * Оновлення стану маркера на основі поточного балу стабільності.
     * @param score Бал від 0.0 до 10.0
     */
    public void updateStatus(float score) {
        // Чим нижчий бал, тим більший радіус (коло "розлітається")
        this.currentRadius = 50f + (10f - score) * 25f;

        // Отримуємо колір через наш UIHelper
        this.currentColor = UIHelper.getColorForScore(score);

        paintCircle.setColor(currentColor);

        // Перемалювати View
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float centerX = getWidth() / 2f;
        float centerY = getHeight() / 2f;

        // 1. Малюємо статичну мішень (центр)
        canvas.drawCircle(centerX, centerY, targetRadius, paintTarget);
        canvas.drawLine(centerX - 20, centerY, centerX + 20, centerY, paintTarget);
        canvas.drawLine(centerX, centerY - 20, centerX, centerY + 20, paintTarget);

        // 2. Малюємо динамічне коло тремору
        canvas.drawCircle(centerX, centerY, currentRadius, paintCircle);
    }
}