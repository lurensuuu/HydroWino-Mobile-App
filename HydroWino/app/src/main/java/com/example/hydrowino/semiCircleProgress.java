package com.example.hydrowino;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class semiCircleProgress extends View {

    private Paint backgroundPaint;
    private Paint progressPaint;
    private RectF rectF;

    private float progress = 0;
    private float max = 50;

    public semiCircleProgress(Context context, AttributeSet attrs) {
        super(context, attrs);

        int bgColor = 0xFFE6E6E6;
        try {
            bgColor = androidx.core.content.ContextCompat.getColor(context, R.color.progress_background);
        } catch (Exception e) {}

        backgroundPaint = new Paint();
        backgroundPaint.setColor(bgColor);
        backgroundPaint.setStyle(Paint.Style.STROKE);
        backgroundPaint.setStrokeWidth(30);
        backgroundPaint.setStrokeCap(Paint.Cap.ROUND);
        backgroundPaint.setAntiAlias(true);

        progressPaint = new Paint();
        progressPaint.setColor(Color.parseColor("#F5B041"));
        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeWidth(30);
        progressPaint.setStrokeCap(Paint.Cap.ROUND);
        progressPaint.setAntiAlias(true);

        rectF = new RectF();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();

        if (width == 0 || height == 0) return;

        // Draw within bounds
        float stroke = 25;
        float padding = stroke / 2 + 5;

        // Make it a full circle rect, centered
        float diameter = Math.min(width, height) - padding * 2;
        float left = (width - diameter) / 2;
        float top = (height - diameter) / 2;
        rectF.set(left, top, left + diameter, top + diameter);

        // Arc from 135 degrees for 270 degrees (leaving a gap at the bottom)
        float startAngle = 135;
        float totalSweep = 270;

        backgroundPaint.setStrokeWidth(stroke);
        progressPaint.setStrokeWidth(stroke);

        // Background arc
        canvas.drawArc(rectF, startAngle, totalSweep, false, backgroundPaint);

        // Progress arc
        if (max > 0) {
            float sweepAngle = (progress / max) * totalSweep;
            if (sweepAngle > totalSweep) sweepAngle = totalSweep;
            canvas.drawArc(rectF, startAngle, sweepAngle, false, progressPaint);
        }
    }

    public void setProgress(float progress) {
        this.progress = progress;
        invalidate();
    }

    public void setMax(float max) {
        this.max = max;
        invalidate();
    }

    public void setProgressColor(int color) {
        progressPaint.setColor(color);
        invalidate();
    }
}