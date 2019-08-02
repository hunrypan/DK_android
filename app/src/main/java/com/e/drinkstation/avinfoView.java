package com.e.drinkstation;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;

public class avinfoView extends View {

    private Paint mPaint;
    private TextPaint textPaint;
    public int angle = 300;
    public int av_angle = 0;

    public avinfoView(Context context) {
        super(context);
    }

    public avinfoView(Context context, AttributeSet attrs) {
        super(context, attrs);
            }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();
        int paddingRight = getPaddingRight();
        int paddingBottom = getPaddingBottom();

        int contentWidth = getWidth() - paddingLeft - paddingRight;
        int contentHeight = getHeight() - paddingTop - paddingBottom;

        mPaint = new Paint();
        mPaint.setAntiAlias(true);

        textPaint = new TextPaint();
        textPaint.setTextAlign(Paint.Align.CENTER);

        String info = String.format("%.2f",angle/360.0);

        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setColor(Color.GREEN);
        mPaint.setStrokeWidth(30);

        textPaint.setTextSize(60);
        textPaint.setColor(Color.BLACK);

        if (av_angle < angle)
        {
            av_angle = av_angle +2;

            canvas.drawArc(30,30,getWidth()-30,getHeight()-30,0,av_angle,false,mPaint);

            invalidate();

        }else {
            canvas.drawArc(30,30,getWidth()-30,getHeight()-30,0,angle,false,mPaint);

            canvas.drawText(info,
                    getWidth()/2 -20,
                    getHeight()/2,
                    textPaint);
        }

    }
}
