package com.example.sauairinapp.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class HalfCircleProgressView extends View {

    private float progress = 0f;
    private Paint paint;

    public HalfCircleProgressView(Context context) {
        super(context);
        init();
    }

    public HalfCircleProgressView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public HalfCircleProgressView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setColor(getResources().getColor(android.R.color.holo_orange_light, null));
        paint.setStrokeWidth(20f);
        paint.setStyle(Paint.Style.STROKE);
        paint.setAntiAlias(true);
    }

    public void setProgress(float progress) {
        this.progress = Math.min(progress, 100); // cap the progress at 100
        invalidate(); // Redraw the view
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float left = 20f;
        float top = 20f;
        float right = getWidth() - 20f;
        float bottom = getHeight() * 2 - 20f;

        // Draw the half-circle arc
        canvas.drawArc(left, top, right, bottom, 180f, progress * 1.8f, false, paint);
    }
}
