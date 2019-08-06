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
    private int av_angle = 0;

    public avinfoView(Context context) {
        super(context);
    }

    public avinfoView(Context context, AttributeSet attrs) {
        super(context, attrs);
            }


    public void setAngle(int angle) {
        this.angle = angle;
    }

    public int getAngle() {
        return angle;
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


        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setColor(Color.GREEN);
        mPaint.setStrokeWidth(30);


        if (av_angle < angle)
        {
            av_angle = av_angle +3;

            canvas.drawArc(30,30,getWidth()-30,getHeight()-30,0,av_angle,false,mPaint);

            invalidate();

        }else {
            canvas.drawArc(30,30,getWidth()-30,getHeight()-30,0,angle,false,mPaint);
        }

    }
}
