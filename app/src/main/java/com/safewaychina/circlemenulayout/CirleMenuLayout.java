package com.safewaychina.circlemenulayout;

import android.content.Context;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * @Description: TODO(这里用一句话描述这个类的作用)
 * @author: lanming
 * @date: 2016-04-05
 */
public class CirleMenuLayout extends ViewGroup {
    //半径
    private int mRadius;
    //child item 默认尺寸比例
    private float RADIO_DEFUALT_CHILD_DIMENSION = 1 / 4f;
    //中心item 默认尺寸比例
    private float RADIO_DEFUALT_CENTER_DIMENSION = 1 / 3f;
    //默认内边距比例
    private float RADIO_PADDING_LAYOUT = 1 / 25f;

    //大于300则认为是快速滑动
    private int FLINGABLE_VALUE = 300;
    //大于3着屏蔽点击事件
    private int NOCLICK_VALUE = 3;

    private int mFlingable = FLINGABLE_VALUE;
    private int mPadding;
    private int mStartAngle = 0;
    private String[] mItemsTexts;
    private int[] mItemsImages;
    private int mChildCount;
    //滑动角度
    private float mTmpAngle;
    //down to up time
    private long mDownTime;
    //滑动标记
    private boolean isFiling;

    public CirleMenuLayout(Context context) {
        this(context, null);
    }

    public CirleMenuLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CirleMenuLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setPadding(0, 0, 0, 0);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(widthMeasureSpec);
        int desireWidth = 0;
        int desireHeight = 0;
        if (widthMode != MeasureSpec.EXACTLY || heightMode != MeasureSpec.EXACTLY) {
            desireWidth = getSuggestedMinimumWidth();
            desireWidth = desireWidth == 0 ? getDefaultWidth() : desireWidth;
            desireHeight = getSuggestedMinimumHeight();
            desireHeight = desireHeight == 0 ? getDefaultHeight() : desireHeight;
        } else {
            desireWidth = desireHeight = Math.min(widthSize, heightSize);
        }
        setMeasuredDimension(desireWidth, desireHeight);
        mRadius = Math.max(getMeasuredWidth(), getMeasuredHeight());
        int childCount = getChildCount();
        int childSize = (int) (mRadius * RADIO_DEFUALT_CHILD_DIMENSION);
        int childMode = MeasureSpec.EXACTLY;
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == GONE) {
                continue;
            }
            int makeMeasureSpec = -1;
            if (child.getId() == R.id.id_circle_menu_item_center) {
                makeMeasureSpec = MeasureSpec.makeMeasureSpec((int) (mRadius * RADIO_DEFUALT_CENTER_DIMENSION), childMode);
            } else {
                makeMeasureSpec = MeasureSpec.makeMeasureSpec(childSize, childMode);
            }
            child.measure(makeMeasureSpec, makeMeasureSpec);
        }
        mPadding = (int) (mRadius * RADIO_PADDING_LAYOUT);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int childCount = getChildCount();
        int radiusLayout = mRadius;
        int left = 0;
        int top = 0;
        int cWidth = (int) (mRadius * RADIO_DEFUALT_CENTER_DIMENSION);
        float angle = 360 / (childCount - 1);
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == View.GONE) {
                continue;
            }
            if (child.getId() == R.id.id_circle_menu_item_center) {
                continue;
            }
            mStartAngle = mStartAngle % 360;
            float tmp = radiusLayout / 2f - cWidth / 2f - mPadding;
            top = radiusLayout / 2 + (int) Math.round(tmp * Math.sin(Math.toRadians(mStartAngle)) - 1 / 2f * cWidth);
            left = radiusLayout / 2 + (int) Math.round(tmp * Math.cos(Math.toRadians(mStartAngle)) - 1 / 2f * cWidth);
            child.layout(left, top, left + cWidth, top + cWidth);
            mStartAngle += angle;
        }
        // 找到中心的view，如果存在设置onclick事件
        View cView = findViewById(R.id.id_circle_menu_item_center);
        if (cView != null) {
            cView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {

                    if (mOnMenuItemClickListener != null) {
                        mOnMenuItemClickListener.onCenterItemClick(v);
                    }
                }
            });
            // 设置center item位置
            int cl = radiusLayout / 2 - cView.getMeasuredWidth() / 2;
            int cr = cl + cView.getMeasuredWidth();
            cView.layout(cl, cl, cr, cr);
        }
    }

    private float mLastX, mLastY;
    private AutoFlingRunnable mFlingRunnable;

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        float x = ev.getX();
        float y = ev.getY();
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mLastX = x;
                mLastY = y;
                mDownTime = System.currentTimeMillis();
                mTmpAngle = 0;
                if (isFiling) {
                    removeCallbacks(mFlingRunnable);
                    isFiling = false;
                    return true;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                float startAngle = getAngle(mLastX, mLastY);
                float endAngle = getAngle(x, y);
                if (getQuadrant(x, y) == 1 || getQuadrant(x, y) == 4) {
                    mStartAngle += endAngle - startAngle;
                    mTmpAngle += endAngle - startAngle;
                } else {
                    mStartAngle += -endAngle + startAngle;
                    mTmpAngle += -endAngle + startAngle;
                }
                Log.e("TAG", "startAngle = " + startAngle + " , endAngle =" + endAngle);
                requestLayout();
                mLastY = y;
                mLastX = x;
                break;
            case MotionEvent.ACTION_UP:
                float anglePerSecond = mTmpAngle * 1000 / (System.currentTimeMillis() - mDownTime);
                if (Math.abs(anglePerSecond) > mFlingable && !isFiling) {
                    post(mFlingRunnable = new AutoFlingRunnable(anglePerSecond));
                    return true;
                }
                if (Math.abs(mTmpAngle) > NOCLICK_VALUE) {
                    return true;
                }
                break;
        }
        return super.dispatchTouchEvent(ev);
    }

    private int getQuadrant(float x, float y) {
        int tmpx = (int) (x - mRadius / 2);
        int tmpy = (int) (y - mRadius / 2);
        if (tmpx > 0) {
            return tmpy >= 0 ? 1 : 4;
        } else {
            return tmpy >= 0 ? 2 : 3;
        }

    }

    private float getAngle(float x, float y) {
        double tmpx = x - mRadius / 2;
        double tmpy = y - mRadius / 2;

        return (float) (Math.asin(tmpy / Math.hypot(tmpx, tmpy)) * 180 / Math.PI);
    }

    private class AutoFlingRunnable implements Runnable {
        private float angelPerSecond;

        public AutoFlingRunnable(float velocity) {
            this.angelPerSecond = velocity;
        }

        @Override
        public void run() {
            if (Math.abs(angelPerSecond) < 20) {
                isFiling = false;
                return;
            }
            isFiling = true;
            mStartAngle += (angelPerSecond / 30);
            angelPerSecond /= 1.0666F;
            postDelayed(this, 30);
            requestLayout();
        }
    }

    public void setMenuItemIconsAndTexts(String[] itemTexts, int[] resIds) {
        mItemsImages = resIds;
        mItemsTexts = itemTexts;
        if (resIds == null || itemTexts == null) {
            throw new IllegalArgumentException("文本或者图片至少一项");
        }
        mChildCount = resIds == null ? itemTexts.length : resIds.length;
        if (resIds != null || itemTexts != null) {
            mChildCount = Math.min(resIds.length, itemTexts.length);
        }
        addItems();
        invalidate();
    }

    private void addItems() {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        for (int i = 0; i < mChildCount; i++) {
            final int j = i;
            View view = inflater.inflate(R.layout.view_circle_menu_item, this, false);
            TextView tv = (TextView) view.findViewById(R.id.id_circle_menu_item_text);
            ImageView iv = (ImageView) view.findViewById(R.id.id_circle_menu_item_image);
            if (iv != null) {
                iv.setVisibility(View.VISIBLE);
                iv.setImageResource(mItemsImages[i]);
                iv.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mOnMenuItemClickListener != null) {
                            mOnMenuItemClickListener.onItemClick(v, j);
                        }
                    }
                });
            }
            if (tv != null) {
                tv.setVisibility(View.VISIBLE);
                tv.setText(mItemsTexts[i]);
            }
            addView(view);
        }
    }

    private int getDefaultHeight() {
        WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics dm = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(dm);
        return dm.heightPixels;
    }

    private int getDefaultWidth() {
        WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics dm = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(dm);
        return dm.widthPixels;
    }

    public interface OnMenuItemClickListener {
        void onItemClick(View view, int position);

        void onCenterItemClick(View view);
    }

    private OnMenuItemClickListener mOnMenuItemClickListener;

    public void setOnItemClickListener(OnMenuItemClickListener l) {
        this.mOnMenuItemClickListener = l;
    }

}
