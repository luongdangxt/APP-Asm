package com.example.personalfinancialmanagement;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

/**
 * Simple neumorphic-like circular dial that renders a base ring, a blue progress arc
 * and a small knob at the end of the arc. Used in TotalExpenses categories header.
 */
public class DialProgressView extends View {
    private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint basePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint knobPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint innerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF arcRect = new RectF();
    private float ringWidthPx;
    private float progress = 0f; // 0..1
    private int primary = 0xFF00B4FF; // primaryBlue
    private int base = 0xFF2B3440;    // dark ring for night, replaced by theme color in XML if needed

    public DialProgressView(Context c) { super(c); init(); }
    public DialProgressView(Context c, AttributeSet a) { super(c,a); init(); }
    public DialProgressView(Context c, AttributeSet a, int s) { super(c,a,s); init(); }

    private void init() {
        float dp = getResources().getDisplayMetrics().density;
        ringWidthPx = 18f * dp;

        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setStrokeCap(Paint.Cap.ROUND);
        ringPaint.setStrokeWidth(ringWidthPx);
        ringPaint.setColor(primary);

        basePaint.setStyle(Paint.Style.STROKE);
        basePaint.setStrokeCap(Paint.Cap.ROUND);
        basePaint.setStrokeWidth(ringWidthPx);
        basePaint.setColor(0x33212121);

        knobPaint.setStyle(Paint.Style.FILL);
        knobPaint.setColor(primary);

        innerPaint.setStyle(Paint.Style.FILL);
        innerPaint.setColor(0xFF1E232B);
    }

    public void setProgress(float p) {
        if (p < 0) p = 0; if (p > 1) p = 1;
        if (this.progress != p) { this.progress = p; invalidate(); }
    }

    @Override protected void onMeasure(int wSpec, int hSpec) {
        int min = (int) (180 * getResources().getDisplayMetrics().density);
        int w = resolveSize(min, wSpec);
        int h = resolveSize(min, hSpec);
        int s = Math.min(w, h);
        setMeasuredDimension(s, s);
    }

    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        int w = getWidth(); int h = getHeight();
        float cx = w / 2f; float cy = h / 2f;
        float radius = Math.min(cx, cy) - ringWidthPx * 0.8f;

        // Inner circle with subtle radial (gives depth)
        float innerR = radius - ringWidthPx * 0.9f;
        Shader grad = new RadialGradient(cx, cy, innerR * 1.2f,
                new int[]{0xFF20262E, 0xFF151A20},
                new float[]{0.2f, 1f}, Shader.TileMode.CLAMP);
        innerPaint.setShader(grad);
        c.drawCircle(cx, cy, innerR, innerPaint);

        // Base ring
        arcRect.set(cx - radius, cy - radius, cx + radius, cy + radius);
        basePaint.setColor(0x1A000000); // transparent dark
        c.drawArc(arcRect, -90, 360, false, basePaint);

        // Progress arc
        ringPaint.setColor(primary);
        float sweep = 360f * progress;
        c.drawArc(arcRect, -90, sweep, false, ringPaint);

        // Knob at end of arc
        double ang = Math.toRadians(-90 + sweep);
        float kx = (float) (cx + Math.cos(ang) * radius);
        float ky = (float) (cy + Math.sin(ang) * radius);
        float knobR = ringWidthPx * 0.55f;
        // outer glow
        Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
        glow.setStyle(Paint.Style.STROKE);
        glow.setStrokeWidth(3f);
        glow.setColor(0x55FFFFFF);
        c.drawCircle(kx, ky, knobR + 3, glow);
        c.drawCircle(kx, ky, knobR, knobPaint);

        // Optional ticks (every 45deg)
        Paint tick = new Paint(Paint.ANTI_ALIAS_FLAG);
        tick.setStrokeWidth(3f); tick.setColor(0x33FFFFFF);
        float r1 = radius + ringWidthPx * 0.25f; float r2 = radius + ringWidthPx * 0.05f;
        for (int i=0;i<8;i++) {
            float a = (float) Math.toRadians(i * 45f - 90f);
            float x1 = (float) (cx + Math.cos(a) * r1);
            float y1 = (float) (cy + Math.sin(a) * r1);
            float x2 = (float) (cx + Math.cos(a) * r2);
            float y2 = (float) (cy + Math.sin(a) * r2);
            c.drawLine(x1, y1, x2, y2, tick);
        }
    }
}
