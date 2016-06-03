package com.jl.pathmesuretest;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

/**
 * Created by Administrator on 2016/6/3.
 */
public class SearchStatusIndicator extends View implements ValueAnimator.AnimatorUpdateListener {
    private static final String TAG = "SearchStatusIndicator";
    private int mCenterX;  //画布中心点
    private int mCenterY;
    private Paint mPaint;
    private Path mPath1;    //放大镜路径Path，init（）初始化
    private Path mPath2;    //外圈的路径Path
    public static final int STATUS_UNDO = 0;  //状态码
    public static final int STATUS_PREPARING = 1;
    public static final int STATUS_SEARCHING = 2;
    public static final int STATUS_OVER = 3;
    private int status = STATUS_UNDO;  //默认Undo
    private int mInnerRadius; //放大镜 圆半径
    private int mOuterRadius;   //外圈半径
    private long mDuration = 3000; //持续时间，所有动画采用一个时间
    private int mCurrentTime;  //动画当前时间（此时间非真实走过的时间，当前采用了加速减速Animation插补器）
    private Path mDrawPath;     //当前需要绘制的Path
    private PathMeasure mMeasure1; //放大镜测量（计算起始长度，结束长度，截取动画需要显示的一部分放大镜）
    private float mTotalLen;  //放大镜总长度  圆+线
    private float mInnerCircleLen; //放大镜圆长度，周长
    private float mOuterCircleLen;//外圈周长
    private PathMeasure mMeasure2;//外圈 测量（计算起始长度，结束长度，截取动画需要显示的一部外圈）
    private double mMaxLen;  //外圈转动时最大长度 （旋转的同时，长度 0--》mMaxLen--》0
    private double a;  //运算是的中间数，避免在onDraw重复运算 （用于计算外圈旋转时显示的长度）
    private ValueAnimator mAnimation; //valueAnimation，int，(0-mDuration)
    private boolean isAnimationRun;//记录是否处于动画中

    public SearchStatusIndicator(Context context) {
        this(context, null);

    }

    public SearchStatusIndicator(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SearchStatusIndicator(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mCenterX = w / 2;
        mCenterY = h / 2;
    }

    private void init() {
        //初始化Paint
        mPaint = new Paint();
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(15);
        mPaint.setColor(Color.RED);
        mPaint.setAntiAlias(true);

        mInnerRadius = 50;
        mOuterRadius = 150;
        mMaxLen = mOuterRadius;  //外圈转动是的最大长度暂时采用外圈的半径

        //初始化放大镜path，mMeasure1 将反复对此路径进行片段截取
        mPath1 = new Path();
        mPath1.addCircle(0, 0, mInnerRadius, Path.Direction.CCW);
        mPath1.lineTo(mOuterRadius, 0);

        //外圈Path，mMeasure2 将反复对此路径进行片段截取
        mPath2 = new Path();
        mPath2.addCircle(0, 0, mOuterRadius, Path.Direction.CW);

        mDrawPath = new Path();

        mMeasure1 = new PathMeasure(mPath1, false);
        mInnerCircleLen = mMeasure1.getLength();

        //获取放大镜 圆 + 柄 的总长
        mTotalLen = mMeasure1.getLength();
        while (mMeasure1.nextContour()) {
            mTotalLen += mMeasure1.getLength();
        }
        //上面while循环之后重新设置，返回到第一条曲线
        mMeasure1.setPath(mPath1, false);

        mMeasure2 = new PathMeasure(mPath2, false);
        mOuterCircleLen = mMeasure2.getLength();
        mMeasure2.setPath(mPath2, false);

        a = -(4 * mMaxLen / ((float) mDuration * mDuration));


    }

    @Override
    protected void onDraw(Canvas canvas) {

        //坐标原点移动到中心位置
        canvas.translate(mCenterX, mCenterY);
        //反转Y轴
        canvas.scale(1, -1);

        switch (status) {
            case STATUS_UNDO:
                model0(); //直接绘制，无动画
                break;
            case STATUS_PREPARING:
                model1();//根据动画回调重设的mCurrenttime 截取mPath1片段，加入mDrawPath（放大镜慢慢消失）
                break;
            case STATUS_SEARCHING:
                model2();//根据动画回调时间mCurrenttime 设置mDrawPath（转圈圈）
                break;
            case STATUS_OVER:
                model3();//放大镜慢慢重现
                break;
        }

        //设置的path都是水平方向，画布旋转后绘制
        canvas.rotate(-45);
        canvas.drawPath(mDrawPath, mPaint);


    }

    private void model3() {
        //清楚之前的路径信息
        mDrawPath.reset();

        //重新返回到测量第一条曲线的状态
        mMeasure1.setPath(mPath1, false);

        //截取放大镜圆圈部分的片段
        float startLen = (mTotalLen / mDuration) * (mCurrentTime - mDuration * ((mTotalLen - mInnerCircleLen) / mTotalLen));
        mMeasure1.getSegment(mMeasure1.getLength() - startLen, mMeasure1.getLength(), mDrawPath, true);
        mMeasure1.nextContour();
        //截取手柄的片段
        mMeasure1.getSegment(mMeasure1.getLength() - mCurrentTime * mTotalLen / mDuration, mMeasure1.getLength(), mDrawPath, true);


    }

    private void model2() {
        mDrawPath.reset();
        mMeasure2.setPath(mPath2, false);
        //本身变化的长度 （短--》长--》短）
        //与原点相交的倒抛物线,
        double len = a * Math.pow((mCurrentTime - mDuration / 2.0), 2) + mMaxLen;

        float startLen = mCurrentTime * mOuterCircleLen / mDuration;
        //根据起始位置和长度，截取外圈片段
        mMeasure2.getSegment(startLen, startLen + (float) len, mDrawPath, true);


    }

    private void model1() {
        mDrawPath.reset();
        mMeasure1.setPath(mPath1, false);

        mMeasure1.getSegment(mCurrentTime * mTotalLen / mDuration, mMeasure1.getLength(), mDrawPath, true);
        while (mMeasure1.nextContour()) {
            mMeasure1.getSegment((mTotalLen / mDuration) * (mCurrentTime - mDuration * (mInnerCircleLen / mTotalLen)), mMeasure1.getLength(), mDrawPath, true);
        }


    }

    private void model0() {
        mMeasure1.getSegment(0, mMeasure1.getLength(), mDrawPath, true);
        while (mMeasure1.nextContour()) {
            mMeasure1.getSegment(0, mMeasure1.getLength(), mDrawPath, true);

        }
    }

    public void setStatus(int status) {
        this.status = status;
        Log.d(TAG, "setStatus: " + status);
        if (status == STATUS_UNDO) {
            invalidate();
            return;
        }
        if (!isAnimationRun) {

            isAnimationRun = true;

            mAnimation = new ValueAnimator();
            mAnimation.setDuration(mDuration);
            mAnimation.setIntValues(0, (int) mDuration);
            AccelerateDecelerateInterpolator interpolator = new AccelerateDecelerateInterpolator();
            mAnimation.setInterpolator(interpolator);
            mAnimation.addUpdateListener(this);
            mAnimation.start();


        }


    }


    @Override
    public void onAnimationUpdate(ValueAnimator animation) {
        mCurrentTime = (int) animation.getAnimatedValue();
        invalidate();
        if (mCurrentTime == mDuration) {
            isAnimationRun = false;
            int next = (status + 1) % 4;
            if (next == 0)
                next += 1;
            setStatus(next);
        }
    }

}
