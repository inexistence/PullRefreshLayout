package com.jb.pullrefreshlayout.customview;


import android.animation.Animator;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.jb.pullrefreshlayout.customview.handler.PullRefreshDataHandler;
import com.jb.pullrefreshlayout.customview.handler.PullRefreshUIHandler;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * 可自定义的下拉刷新布局
 * Created by Jianbin on 2015/12/12.
 */
public class PullRefreshLayout extends RelativeLayout {

    View header;
    View content;

    List<PullRefreshUIHandler> refreshUIHandlerList = new ArrayList<>(10);
    PullRefreshDataHandler refreshDataHandler;
    WeakHandler handler = new WeakHandler(this);
    OnHideHeaderWhenRefreshingListener onHideHeaderWhenRefreshingListener;
    OnTouchListener mOnTouchListener;

    private static final int DIR_UP = 1; // 手势方向 向上
    private static final int DIR_DOWN = -1; // 手势方向 向下
    private static final int DIR_NONE = 0;
    private static final int DEFAULT_HEADER_HEIGHT = 100;

    private int headerOffset = 0;
    private float mOffsetTop = 0;
    private int scrollDir = DIR_NONE;
    private long completeDelay = 0;
    private float resistance = 3f;// 下拉阻力,1为无阻力

    private boolean isPullDown = false; // 正在下拉
    private boolean isRefreshing = false;
    private boolean canHideHeaderWhenRefreshing = false;
    private boolean headerOnShow = false;
    private boolean isAnimation = false;

    private float mLastY = 0;
    private float mStartPullDownY = 0;

    public PullRefreshLayout(Context context) {
        super(context);
    }

    public PullRefreshLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PullRefreshLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public PullRefreshLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(true, l, t, r, b);
        scrollTopTo(mOffsetTop);
    }

    public void setHeaderOffset(int offset) {
        headerOffset = offset;
    }

    public View getContent() {
        return content;
    }

    public void setContent(int layoutId) {
        View v = LayoutInflater.from(getContext()).inflate(layoutId, this, false);
        setContent(v);
    }

    public void setContent(View content) {
        if (this.content != null) {
            removeView(this.content);
        }
        this.content = content;
        addView(content);
        scrollTopTo(0);
        invalidate();
    }

    /**
     * 设置当下拉刷新时content不随之移动
     */
    public void stickContentWhenPullDown() {
        // TODO
    }

    /**
     * 获取下拉阻力
     * @return 下拉阻力 1为无阻力
     */
    public float getResistance() {
        return resistance;
    }

    /**
     * 设置下拉阻力
     * @param resistance 阻力倍数 1为无阻力
     */
    public void setResistance(float resistance) {
        this.resistance = resistance;
    }

    public boolean isHeaderOnShow() {
        return headerOnShow;
    }

    public boolean canHideHeaderWhenRefreshing() {
        return canHideHeaderWhenRefreshing;
    }

    public void setOnHideHeaderWhenRefreshingListener(OnHideHeaderWhenRefreshingListener onHideHeaderWhenRefreshingListener) {
        this.onHideHeaderWhenRefreshingListener = onHideHeaderWhenRefreshingListener;
    }

    public void setCanHideHeaderWhenRefreshing(boolean canHideHeaderWhenRefreshing) {
        this.canHideHeaderWhenRefreshing = canHideHeaderWhenRefreshing;
    }

    public void addRefreshUIHandler(PullRefreshUIHandler handler) {
        refreshUIHandlerList.add(handler);
    }

    public void removeRefreshUIHandler(PullRefreshUIHandler handler) {
        refreshUIHandlerList.remove(handler);
    }

    public void setRefreshDataHandler(PullRefreshDataHandler handler) {
        refreshDataHandler = handler;
    }

    private float mTouchStartY = 0;
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        Log.d("test", "onInterceptTouchEvent");
        int action = ev.getAction();
        if (mOnTouchListener != null) {
            mOnTouchListener.onTouch(this, ev);
        }
        if (canHideHeaderWhenRefreshing && isRefreshing && headerOnShow && !isAnimation) {
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    mTouchStartY = ev.getY();
                    break;
                case MotionEvent.ACTION_MOVE:
                    float curY = ev.getY();
                    if (mTouchStartY - curY > ViewConfiguration.get(getContext()).getScaledTouchSlop()) {
                        if (onHideHeaderWhenRefreshingListener != null) {
                            onHideHeaderWhenRefreshingListener.onHideHeaderWhenRefreshing(this);
                        }
                        animateToTop(0);
                    }
                    break;
            }
        }
        if(isAnimation) {
            isPullDown = false;
            return true;
        }
        if (isRefreshing && headerOnShow) {
            isPullDown = false;
            return false;
        }

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                scrollDir = DIR_NONE;
                mTouchStartY = ev.getY();
                mLastY = ev.getY();
                isPullDown = false;
                break;
            case MotionEvent.ACTION_MOVE:
                float currentY = ev.getY();
                if(currentY - mLastY > 0) {
                    scrollDir = DIR_DOWN;
                } else if (currentY - mLastY < 0) {
                    scrollDir = DIR_UP;
                } else {
                    scrollDir = DIR_NONE;
                }
                mLastY = currentY;

                if(!isPullDown) {
                    if (content != null && !canChildScrollUp(content) && scrollDir == DIR_DOWN) {
                        isPullDown = true;
                        mStartPullDownY = currentY;
                    }
//                    if(content!=null && canChildScrollDown(content) && scrollDir == DIR_UP) {
//
//                    }
                }


                if (isPullDown) {
                    // listView不处理该事件
                    return true;
                }
                break;
            case MotionEvent.ACTION_UP:
                scrollDir = DIR_NONE;
                isPullDown = false;
                break;
        }

        return false;
    }

    @Override
    public void setOnTouchListener(OnTouchListener l) {
        this.mOnTouchListener = l;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isPullDown) {
            return super.onTouchEvent(event);
        }
        int action = event.getAction();
        switch (action) {
            case MotionEvent.ACTION_MOVE:
                float currentY = event.getY();
                float top = currentY - mStartPullDownY;
                if (getMeasuredHeight() != 0) {
                    // 添加拖动阻力
                    float percent = Math.min(1.0f, 1.0f / getResistance());
                    top = top * percent;
                }
                if ((int) top > getHeaderHeight()) {
                    top = getHeaderHeight();
                }
                if (top <= 0) {
                    top = 0;
                }
                scrollTopTo(top);
                // 这里不绘制的话，第一次下拉显示不了header
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                isPullDown = false;
                if (content.getTop() >= getHeaderHeight() - headerOffset) {
                    for (int i = 0; i < refreshUIHandlerList.size(); i++) {
                        refreshUIHandlerList.get(i).onUIRefreshPrepare(this);
                    }
                    animateToTop(getHeaderHeight() - headerOffset, new Animator.AnimatorListener() {
                        @Override
                        public void onAnimationStart(Animator animator) {
                        }

                        @Override
                        public void onAnimationEnd(Animator animator) {
                            if (refreshDataHandler != null) {
                                if (refreshDataHandler.checkCanDoRefresh(PullRefreshLayout.this, header, content)) {
                                    if (!isRefreshing) {
                                        isRefreshing = true;
                                        refreshDataHandler.onRefreshBegin(PullRefreshLayout.this);
                                    }
                                    for (int i = 0; i < refreshUIHandlerList.size(); i++) {
                                        refreshUIHandlerList.get(i).onUIRefreshBegin(PullRefreshLayout.this);
                                    }
                                } else {
                                    refreshComplete();
                                }
                            }
                        }

                        @Override
                        public void onAnimationCancel(Animator animator) {

                        }

                        @Override
                        public void onAnimationRepeat(Animator animator) {

                        }
                    });
                } else {
                    animateToTop(0);
                }
                break;
        }
        return super.onTouchEvent(event);
    }

    public void refreshComplete() {
        isRefreshing = false;
        for(int i = 0; i < refreshUIHandlerList.size(); i++) {
            refreshUIHandlerList.get(i).onUIRefreshComplete(this);
        }
        handler.sendEmptyMessageDelayed(WeakHandler.MSG_ANIMATE_TO_TOP, completeDelay);
    }

    public void setHeader(View header) {
        if (this.header != null) {
            this.removeView(this.header);
        }
        this.header = header;
        this.addView(header, 0);
        scrollTopTo(0);
        invalidate();
    }

    public void setHeader(Drawable drawable, int width, int height) {
        ImageView imageView = new ImageView(getContext());
        LayoutParams params = new LayoutParams(width, height);
        imageView.setLayoutParams(params);
        imageView.setImageDrawable(drawable);
        setHeader(imageView);
    }

    public View getHeader() {
        return header;
    }

    public void setCompleteDelay(long completeDelay) {
        this.completeDelay = completeDelay;
    }

    public void setHeader(int layoutId) {
        View header = LayoutInflater.from(getContext()).inflate(layoutId, this, false);
        setHeader(header);
    }

    private void animateToTop(final float y) {
        animateToTop(y, null);
    }

    private void animateToTop(final float y, Animator.AnimatorListener listener) {
        if (isAnimation) {
            return;
        }
        ValueAnimator animator = ValueAnimator.ofFloat(content.getTop(), y);
        animator.setDuration(300);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float frameValue = (Float) animation.getAnimatedValue();
                scrollTopTo(frameValue);
                if (frameValue == y) {
                    isAnimation = false;
                    if (y == 0) {
                        for (int i = 0; i < refreshUIHandlerList.size(); i++) {
                            refreshUIHandlerList.get(i).onUIReset(PullRefreshLayout.this);
                        }
                    }
                }
            }
        });
        if (listener != null) {
            animator.addListener(listener);
        }
        isAnimation = true;
        animator.start();
    }

    private int getHeaderHeight() {
        return header == null ? DEFAULT_HEADER_HEIGHT : header.getMeasuredHeight();
    }

    private void scrollTopTo(float y) {
        headerOnShow = (y > 0);
        mOffsetTop = y;
        if (content != null) {
            content.offsetTopAndBottom((int) y - content.getTop());
        }
        if (header != null) {
            header.offsetTopAndBottom((int) y - header.getHeight() - header.getTop());
        }
        for (int i = 0; i < refreshUIHandlerList.size(); i++) {
            refreshUIHandlerList.get(i).onUIPositionChange(this, isPullDown, y, getHeaderHeight());
        }
    }

    private boolean canChildScrollUp(View mTarget) {
        if (Build.VERSION.SDK_INT < 14) {
            if (mTarget instanceof AbsListView) {
                final AbsListView absListView = (AbsListView) mTarget;
                return absListView.getChildCount() > 0
                        && (absListView.getFirstVisiblePosition() > 0 || absListView.getChildAt(0)
                        .getTop() < absListView.getPaddingTop());
            } else {
                return mTarget.getScrollY() > 0;
            }
        } else {
            return ViewCompat.canScrollVertically(mTarget, -1);
        }
    }

    static class WeakHandler extends Handler {
        public static final int MSG_ANIMATE_TO_TOP = 0x1212;
        WeakReference<PullRefreshLayout> reference;
        public WeakHandler(PullRefreshLayout layout) {
            reference = new WeakReference<>(layout);
        }
        @Override
        public void handleMessage(Message msg) {
            PullRefreshLayout refreshLayout = reference.get();
            if (refreshLayout == null) return;
            if(msg.what == MSG_ANIMATE_TO_TOP) {
                refreshLayout.animateToTop(0);
            }
        }
    }

    public interface OnHideHeaderWhenRefreshingListener {
        void onHideHeaderWhenRefreshing(PullRefreshLayout parent);
    }
}
