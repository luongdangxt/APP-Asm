package com.example.personalfinancialmanagement;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class CategoryArcView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private float[] portions = new float[0];
    private int[] colors = new int[0];
    private int baseColor = 0xFFE6EAF2; // light base
    private float strokePx;

    public CategoryArcView(Context c) { super(c); init(); }
    public CategoryArcView(Context c, AttributeSet a) { super(c,a); init(); }
    public CategoryArcView(Context c, AttributeSet a, int s) { super(c,a,s); init(); }
    private void init(){
        float dp = getResources().getDisplayMetrics().density;
        strokePx = 22f * dp;
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(strokePx);
    }

    public void setData(double[] values, int[] colors){
        if (values == null || values.length == 0) {
            this.portions = new float[0];
            this.colors = new int[0];
            invalidate();
            return;
        }
        double sum = 0; for (double v: values) sum += Math.max(0, v);
        if (sum <= 0) {
            this.portions = new float[0]; this.colors = new int[0]; invalidate(); return;
        }
        this.portions = new float[values.length];
        for (int i=0;i<values.length;i++) this.portions[i] = (float)(values[i] / sum);
        this.colors = colors != null ? colors.clone() : new int[values.length];
        invalidate();
    }

    @Override protected void onMeasure(int wSpec, int hSpec) {
        int min = (int)(160 * getResources().getDisplayMetrics().density);
        int w = resolveSize(min, wSpec);
        int h = resolveSize(min, hSpec);
        setMeasuredDimension(w, h);
    }

    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        int w = getWidth(); int h = getHeight();
        float pad = strokePx;
        rect.set(pad, pad, w - pad, h - pad);

        // draw base semi-circle (top half)
        paint.setColor(baseColor);
        c.drawArc(rect, 180, 180, false, paint);

        float start = 180f;
        for (int i=0;i<portions.length && i<colors.length; i++) {
            float sweep = 180f * portions[i];
            paint.setColor(colors[i]);
            c.drawArc(rect, start, sweep, false, paint);
            start += sweep;
        }
    }
}
