package com.pulse.fitness.ui.widget;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;
import com.pulse.fitness.R;

/** Decorative vector illustration. Written technique, not this drawing, guides the movement. */
public final class ExerciseArtView extends View {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

    public ExerciseArtView(Context c, AttributeSet a) {
        super(c, a);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.save();
        float scale = Math.min(getWidth() / 320f, getHeight() / 200f);
        canvas.translate(getWidth() / 2f - 160 * scale, getHeight() / 2f - 100 * scale);
        canvas.scale(scale, scale);
        p.setColor(getContext().getColor(R.color.art_backdrop));
        canvas.drawCircle(160, 100, 78, p);
        p.setColor(getContext().getColor(R.color.outline));
        p.setStrokeWidth(1);
        canvas.drawLine(55, 174, 265, 174, p);
        p.setColor(getContext().getColor(R.color.art_body));
        canvas.drawCircle(168, 39, 14, p);
        p.setStrokeWidth(19);
        p.setStrokeCap(Paint.Cap.ROUND);
        canvas.drawLine(160, 67, 145, 111, p);
        p.setStrokeWidth(13);
        canvas.drawLine(157, 71, 119, 91, p);
        canvas.drawLine(119, 91, 93, 71, p);
        canvas.drawLine(163, 73, 199, 91, p);
        canvas.drawLine(199, 91, 226, 71, p);
        p.setColor(getContext().getColor(R.color.art_secondary));
        p.setStrokeWidth(19);
        canvas.drawLine(145, 111, 114, 139, p);
        canvas.drawLine(114, 139, 98, 170, p);
        canvas.drawLine(151, 113, 188, 132, p);
        canvas.drawLine(188, 132, 210, 170, p);
        p.setColor(getContext().getColor(R.color.accent));
        p.setStrokeWidth(8);
        canvas.drawLine(80, 69, 105, 69, p);
        canvas.drawLine(80, 59, 80, 79, p);
        canvas.drawLine(105, 59, 105, 79, p);
        canvas.drawLine(214, 69, 239, 69, p);
        canvas.drawLine(214, 59, 214, 79, p);
        canvas.drawLine(239, 59, 239, 79, p);
        canvas.restore();
    }
}
