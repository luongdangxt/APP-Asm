package com.example.personalfinancialmanagement;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple donut chart for category shares. No external libs.
 */
public class CategoryDonutView extends View {
    public static class Slice { public final float value; public final int color; public Slice(float v,int c){value=v;color=c;} }

    private final List<Slice> slices = new ArrayList<>();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint valueTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint titleTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF oval = new RectF();
    private float strokeWidthPx;

    public CategoryDonutView(Context c){ super(c); init(); }
    public CategoryDonutView(Context c, AttributeSet a){ super(c,a); init(); }
    public CategoryDonutView(Context c, AttributeSet a, int s){ super(c,a,s); init(); }

    private void init(){
        setLayerType(LAYER_TYPE_SOFTWARE, null);
        strokeWidthPx = dp(28);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(strokeWidthPx);

        bgPaint.setStyle(Paint.Style.STROKE);
        bgPaint.setStrokeCap(Paint.Cap.BUTT);
        bgPaint.setStrokeWidth(strokeWidthPx);
        try {
            bgPaint.setColor(androidx.core.content.ContextCompat.getColor(getContext(), R.color.reportDonutTrack));
        } catch (Throwable t) {
            bgPaint.setColor(0xFFE0E7FF);
        }

        valueTextPaint.setTextAlign(Paint.Align.CENTER);
        valueTextPaint.setTextSize(dp(22));
        valueTextPaint.setFakeBoldText(true);
        try {
            valueTextPaint.setColor(androidx.core.content.ContextCompat.getColor(getContext(), R.color.textPrimary));
        } catch (Throwable t) { valueTextPaint.setColor(0xFF111111); }

        titleTextPaint.setTextAlign(Paint.Align.CENTER);
        titleTextPaint.setTextSize(dp(13));
        try {
            titleTextPaint.setColor(androidx.core.content.ContextCompat.getColor(getContext(), R.color.reportChipTint));
        } catch (Throwable t) { titleTextPaint.setColor(0xFF6B7280); }
    }

    private float dp(float d){ return d * getResources().getDisplayMetrics().density; }

    public void setStrokeWidthDp(float dp){
        strokeWidthPx = dp(dp);
        paint.setStrokeWidth(strokeWidthPx);
        bgPaint.setStrokeWidth(strokeWidthPx);
        invalidate();
    }

    public void setData(List<Slice> data){
        slices.clear();
        if (data != null) slices.addAll(data);
        invalidate();
    }

    private CharSequence centerTitle = "Total";
    private CharSequence centerValue = "";
    public void setCenterTexts(CharSequence title, CharSequence value){
        centerTitle = title != null ? title : "";
        centerValue = value != null ? value : "";
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);
        float pad = strokeWidthPx/2f + dp(8);
        float w = getWidth();
        float h = getHeight();
        float size = Math.min(w, h) - pad*2f;
        float left = (w - size)/2f;
        float top = (h - size)/2f;
        oval.set(left, top, left + size, top + size);
        // background ring
        canvas.drawArc(oval, -90, 360, false, bgPaint);

        float total = 0f; for (Slice s : slices) total += Math.max(0f, s.value);
        if (total <= 0f) return;
        float start = -90f;
        final float gap = 3f; // degrees gap between slices
        for (Slice s : slices){
            if (s.value <= 0) continue;
            float sweep = 360f * (s.value / total) - gap;
            if (sweep < 0) sweep = 0;
            paint.setColor(s.color);
            canvas.drawArc(oval, start, sweep, false, paint);
            start += sweep + gap;
        }

        // center labels
        float cx = getWidth()/2f; float cy = getHeight()/2f;
        if (centerValue != null) {
            Paint.FontMetrics fmVal = valueTextPaint.getFontMetrics();
            float valBaseline = cy - (fmVal.ascent + fmVal.descent)/2f - dp(6);
            canvas.drawText(centerValue.toString(), cx, valBaseline, valueTextPaint);
        }
        if (centerTitle != null) {
            Paint.FontMetrics fm = titleTextPaint.getFontMetrics();
            float titleBaseline = cy - (fm.ascent + fm.descent)/2f + dp(14);
            canvas.drawText(centerTitle.toString(), cx, titleBaseline, titleTextPaint);
        }
    }
}
