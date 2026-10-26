package com.pulse.fitness.ui.widget;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;
import java.util.Locale;
import com.pulse.fitness.R;

/** Native chart uses actual records only; zero values remain zero. */
public final class WeeklyChartView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float[] values = new float[7];

    public WeeklyChartView(Context c, AttributeSet a) {
        super(c, a);
    }

    public void setMinutes(float[] minutes) {
        values = minutes.clone();
        StringBuilder b = new StringBuilder("本周训练分钟数：");
        for (int i = 0; i < 7; i++)
            b.append("周")
                    .append("一二三四五六日".charAt(i))
                    .append(String.format(Locale.CHINA, " %.1f 分钟，", values[i]));
        setContentDescription(b.toString());
        invalidate();
    }

    @Override
    protected void onDraw(Canvas c) {
        super.onDraw(c);
        float density = getResources().getDisplayMetrics().density,
                padding = 22 * density,
                width = getWidth() - padding * 2,
                height = getHeight() - 62 * density;
        float max = 15;
        for (float v : values) max = Math.max(max, v);
        paint.setTextSize(11 * density);
        paint.setTextAlign(Paint.Align.CENTER);
        for (int i = 0; i < 7; i++) {
            float x = padding + width * (i + 0.5f) / 7,
                    bar = width / 7 * 0.42f,
                    bottom = height + 20 * density;
            paint.setColor(getContext().getColor(R.color.surface_raised));
            c.drawRoundRect(
                    x - bar / 2,
                    22 * density,
                    x + bar / 2,
                    bottom,
                    6 * density,
                    6 * density,
                    paint);
            if (values[i] > 0) {
                float h = height * Math.min(1, values[i] / max);
                paint.setColor(getContext().getColor(R.color.primary));
                c.drawRoundRect(
                        x - bar / 2, bottom - h, x + bar / 2, bottom, 6 * density, 6 * density,
                        paint);
            }
            paint.setColor(getContext().getColor(R.color.muted));
            c.drawText(String.valueOf("一二三四五六日".charAt(i)), x, getHeight() - 14 * density, paint);
            if (values[i] > 0)
                c.drawText(
                        String.valueOf(Math.round(values[i])),
                        x,
                        Math.max(15 * density, bottom - height * values[i] / max - 5 * density),
                        paint);
        }
    }
}
