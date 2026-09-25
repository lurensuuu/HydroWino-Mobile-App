package com.example.hydrowino;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;

import com.github.mikephil.charting.charts.CombinedChart;
import com.github.mikephil.charting.components.YAxis;

public class TooltipCombinedChart extends CombinedChart {

    private int     tooltipDataIndex = -1;
    private float   tooltipValue     = 0f;
    private String  tooltipLabel     = "";
    private float[] allValues        = null;  // stores all day values

    private final Paint bgPaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public TooltipCombinedChart(Context c) { super(c); initTooltip(); }
    public TooltipCombinedChart(Context c, AttributeSet a) { super(c, a); initTooltip(); }
    public TooltipCombinedChart(Context c, AttributeSet a, int d) { super(c, a, d); initTooltip(); }

    private void initTooltip() {
        int color = 0xFF22C47A;
        try {
            color = androidx.core.content.ContextCompat.getColor(getContext(), R.color.status_improved_icon);
        } catch (Exception e) {}
        bgPaint.setColor(color);
        bgPaint.setStyle(Paint.Style.FILL);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(28f);
        textPaint.setTypeface(Typeface.DEFAULT_BOLD);
    }

    public void setAllValues(float[] values) {
        this.allValues = values;
    }

    public void setTooltipIndex(int index, float value) {
        this.tooltipDataIndex = index;
        this.tooltipValue     = value;
        this.tooltipLabel     = String.format(java.util.Locale.getDefault(), "%.2f cm²", value);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (tooltipDataIndex < 0 || getData() == null || tooltipLabel.isEmpty()) return;

        float[] pts = { tooltipDataIndex, tooltipValue };
        getTransformer(YAxis.AxisDependency.LEFT).pointValuesToPixel(pts);
        float cx = pts[0];
        float cy = pts[1];

        float textW  = textPaint.measureText(tooltipLabel);
        float pH     = 18f, pV = 12f, r = 16f, arrowH = 16f;
        float bw     = textW + pH * 2;
        float bh     = textPaint.getTextSize() + pV * 2;
        float left   = cx - bw / 2f;
        float top    = cy - bh - arrowH - 20f;
        float right  = left + bw;
        float bottom = top + bh;

        // Bubble
        canvas.drawRoundRect(new RectF(left, top, right, bottom), r, r, bgPaint);

        // Arrow
        Path arrow = new Path();
        arrow.moveTo(cx - 14f, bottom);
        arrow.lineTo(cx,       bottom + arrowH);
        arrow.lineTo(cx + 14f, bottom);
        arrow.close();
        canvas.drawPath(arrow, bgPaint);

        // Text
        canvas.drawText(tooltipLabel, cx - textW / 2f, bottom - pV - 2f, textPaint);
    }
}