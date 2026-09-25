package com.example.hydrowino;

import android.content.Context;
import android.widget.TextView;

import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.utils.MPPointF;

public class GrowthMarkerView extends MarkerView {

    public GrowthMarkerView(Context context, int layoutResource) {
        super(context, layoutResource);
    }

    @Override
    public void refreshContent(Entry e, Highlight highlight) {
        super.refreshContent(e, highlight);
    }

    @Override
    public MPPointF getOffset() {
        // Centers the bubble above the point
        return new MPPointF(-(getWidth() / 2f), -getHeight() - 10f);
    }
}