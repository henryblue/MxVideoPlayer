package hb.xvideoplayer;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

public class MxDragLayout extends FrameLayout {
    private ViewDragHelper mDragHelper;

    private int finalLeft = -1;
    private int finalTop = -1;

    private ViewDragHelper.Callback mDragHelperCallback = new ViewDragHelper.Callback() {

        @Override
        public boolean tryCaptureView(View child, int pointerId) {
            return child instanceof MxVideoPlayer;
        }

        @Override
        public int getViewHorizontalDragRange(View child) {
            return  getWidth() - child.getWidth();
        }

        @Override
        public int getViewVerticalDragRange(View child) {
            return getHeight() - child.getHeight();
        }

        @Override
        public int clampViewPositionHorizontal(View child, int left, int dx) {
            final int leftBound = getPaddingLeft();
            final int rightBound = getWidth() - child.getWidth() - leftBound;
            return Math.min(Math.max(left, leftBound), rightBound);
        }

        @Override
        public int clampViewPositionVertical(View child, int top, int dy) {
            final int topBound = getPaddingTop();
            final int bottomBound = getHeight() - child.getHeight() - topBound;
            return Math.min(Math.max(top, topBound), bottomBound);
        }

        @Override
        public void onViewReleased(View releasedChild, float xvel, float yvel) {
            super.onViewReleased(releasedChild, xvel, yvel);
            int viewWidth = releasedChild.getWidth();
            int viewHeight = releasedChild.getHeight();
            int curLeft = releasedChild.getLeft();
            int curTop = releasedChild.getTop();

            finalTop = curTop;
            finalLeft = curLeft;
            if (finalTop + viewHeight > getHeight()) {
                finalTop = getHeight() - viewHeight;
            }

            if (finalLeft + viewWidth > getWidth()) {
                finalLeft = getWidth() - viewWidth;
            }

            mDragHelper.settleCapturedViewAt(finalLeft, finalTop);
            invalidate();
        }
    };


    public MxDragLayout(@NonNull Context context) {
        super(context);
        init();
    }

    public MxDragLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MxDragLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mDragHelper = ViewDragHelper.create(this, mDragHelperCallback);
        setBackgroundColor(Color.TRANSPARENT);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (finalLeft == -1 && finalTop == -1) {
            super.onLayout(changed, left, top, right, bottom);
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        final int action = MotionEventCompat.getActionMasked(ev);
        if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
            mDragHelper.cancel();
            return false;
        }
        return mDragHelper.shouldInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mDragHelper.processTouchEvent(event);
        return false;
    }
}
