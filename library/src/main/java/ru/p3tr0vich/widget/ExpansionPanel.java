package ru.p3tr0vich.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.IntDef;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.ImageView;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static android.animation.PropertyValuesHolder.ofInt;

public class ExpansionPanel extends FrameLayout {

    private static final int COLLAPSED_CHILD_INDEX = 0;
    private static final int EXPANDED_CHILD_INDEX = 1;

    private static final String PROPERTY_NAME_PADDING_TOP = "paddingTop";
    private static final String PROPERTY_NAME_PADDING_BOTTOM = "paddingBottom";

    private FrameLayout mMainLayout;
    private FrameLayout mContentLayout;
    private ImageView mStateIcon;

    private View mCollapsedView;
    private View mExpandedView;

    private int mCollapsedHeight;
    private int mExpandedHeight;

    private int mCollapsedPaddingTop;
    private int mCollapsedPaddingBottom;
    private int mExpandedPaddingTop;
    private int mExpandedPaddingBottom;

    private long mDurationToggle;
    private long mDelayCollapsedViewHiding;
    private long mDurationCollapsedViewChangeVisibility;
    private long mDurationContentHeightChanged;

    private boolean mExpanded;

    private boolean mToggleOnCollapsedClickEnabled;
    private boolean mToggleOnExpandedClickEnabled;

    private AnimatorSet mAnimatorSet = new AnimatorSet();
    private AnimatorSet mChangeViewAnimatorSet = new AnimatorSet();

    private ExpansionPanelListener mListener;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({COLLAPSED, EXPANDED, COLLAPSING, EXPANDING, UPDATING})
    public @interface State {
    }

    private static final int COLLAPSED = 0;
    private static final int EXPANDED = 1;
    private static final int COLLAPSING = 2;
    private static final int EXPANDING = 3;
    private static final int UPDATING = 4;

    @State
    private int mState;

    public ExpansionPanel(Context context) {
        this(context, null);
    }

    public ExpansionPanel(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ExpansionPanel(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        @LayoutRes int collapsedLayoutId = R.layout.expansion_panel_collapsed_default;
        @LayoutRes int expandedLayoutId = R.layout.expansion_panel_expanded_default;

        final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ExpansionPanel, 0, 0);

        try {
            mExpanded = a.getBoolean(R.styleable.ExpansionPanel_defaultExpanded, false);

            collapsedLayoutId = a.getResourceId(R.styleable.ExpansionPanel_collapsedLayout, collapsedLayoutId);
            expandedLayoutId = a.getResourceId(R.styleable.ExpansionPanel_expandedLayout, expandedLayoutId);

            mToggleOnCollapsedClickEnabled = a.getBoolean(R.styleable.ExpansionPanel_toggleOnCollapsedClickEnabled, true);
            mToggleOnExpandedClickEnabled = a.getBoolean(R.styleable.ExpansionPanel_toggleOnExpandedClickEnabled, true);

            mDurationToggle = a.getInteger(R.styleable.ExpansionPanel_durationToggle,
                    getResources().getInteger(R.integer.expansion_panel_animation_duration_toggle));
            mDelayCollapsedViewHiding = a.getInteger(R.styleable.ExpansionPanel_delayCollapsedViewHiding,
                    getResources().getInteger(R.integer.expansion_panel_animation_delay_collapsed_view_hiding));
            mDurationCollapsedViewChangeVisibility = a.getInteger(R.styleable.ExpansionPanel_durationCollapsedViewChangeVisibility,
                    getResources().getInteger(R.integer.expansion_panel_animation_duration_collapsed_view_change_visibility));
            mDurationContentHeightChanged = a.getInteger(R.styleable.ExpansionPanel_durationContentHeightChanged,
                    getResources().getInteger(R.integer.expansion_panel_animation_duration_content_height_changed));
        } finally {
            a.recycle();
        }

        mCollapsedPaddingTop = getResources().getDimensionPixelSize(R.dimen.expansion_panel_collapsed_padding_top);
        mCollapsedPaddingBottom = getResources().getDimensionPixelSize(R.dimen.expansion_panel_collapsed_padding_bottom);
        mExpandedPaddingTop = getResources().getDimensionPixelSize(R.dimen.expansion_panel_expanded_padding_top);
        mExpandedPaddingBottom = getResources().getDimensionPixelSize(R.dimen.expansion_panel_expanded_padding_bottom);

        final LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        mMainLayout = (FrameLayout) inflater.inflate(R.layout.expansion_panel, this, false);

        addView(mMainLayout);

        mContentLayout = (FrameLayout) mMainLayout.findViewById(R.id.expansion_panel_content_layout);
        mStateIcon = (ImageView) mMainLayout.findViewById(R.id.expansion_panel_state_icon);

        mMainLayout.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                toggle(true);
            }
        });

        setCollapsedView(collapsedLayoutId);
        setExpandedView(expandedLayoutId);

        measureHeights();

        setExpanded(mExpanded, false);
    }

    public void setCollapsedView(@NonNull View collapsedView) {
        mContentLayout.addView(collapsedView, COLLAPSED_CHILD_INDEX);
        mCollapsedView = collapsedView;

        mCollapsedView.addOnLayoutChangeListener(new OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                if (mState == COLLAPSED) {
                    measureCollapsedHeight();
                    ExpansionPanel.this.post(new Runnable() {
                        @Override
                        public void run() {
                            checkHeights();
                        }
                    });
                }
            }
        });
    }

    public void setCollapsedView(@LayoutRes int collapsedViewId) {
        setCollapsedView(LayoutInflater.from(getContext()).inflate(collapsedViewId, mContentLayout, false));
    }

    public void setExpandedView(@NonNull View expandedView) {
        mContentLayout.addView(expandedView, EXPANDED_CHILD_INDEX);
        mExpandedView = expandedView;

        mExpandedView.addOnLayoutChangeListener(new OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                if (mState == EXPANDED) {
                    measureExpandedHeight();
                    ExpansionPanel.this.post(new Runnable() {
                        @Override
                        public void run() {
                            checkHeights();
                        }
                    });
                }
            }
        });
    }

    public void setExpandedView(@LayoutRes int expandedViewId) {
        setExpandedView(LayoutInflater.from(getContext()).inflate(expandedViewId, mContentLayout, false));
    }

    private int getContentMeasuredHeight(View view, int minHeight, int paddingTop, int paddingBottom) {
        view.measure(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        int height = view.getMeasuredHeight();

        minHeight = minHeight - paddingTop - paddingBottom;

        if (height < minHeight) height = minHeight;

        return height;
    }

    private void measureCollapsedHeight() {
        mCollapsedHeight = getContentMeasuredHeight(mCollapsedView,
                getResources().getDimensionPixelSize(R.dimen.expansion_panel_collapsed_min_height),
                mCollapsedPaddingTop, mCollapsedPaddingBottom);
    }

    private void measureExpandedHeight() {
        mExpandedHeight = getContentMeasuredHeight(mExpandedView,
                getResources().getDimensionPixelSize(R.dimen.expansion_panel_expanded_min_height),
                mExpandedPaddingTop, mExpandedPaddingBottom);
    }

    private void measureHeights() {
        measureCollapsedHeight();
        measureExpandedHeight();
    }

    private int getContentLayoutHeight() {
        return mContentLayout.getLayoutParams().height;
    }

    private void setContentLayoutHeight(int height) {
        mContentLayout.getLayoutParams().height = height;
        mContentLayout.requestLayout();
    }

    private void setMainLayoutPadding(int top, int bottom) {
        int start = mMainLayout.getPaddingStart();
        int end = mMainLayout.getPaddingEnd();
        mMainLayout.setPaddingRelative(start, top, end, bottom);
    }

    private void checkHeights() {
        int height = getContentLayoutHeight();

        if (mExpanded) {
            if (mExpandedHeight == height) return;
        } else {
            if (mCollapsedHeight == height) return;
        }

        if (isShown() && mDurationContentHeightChanged > 0) {
            final ValueAnimator resizePanelValueAnimator = ValueAnimator.ofInt(
                    height, mExpanded ? mExpandedHeight : mCollapsedHeight);
            resizePanelValueAnimator.setDuration(mDurationContentHeightChanged);
            resizePanelValueAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
            resizePanelValueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    setContentLayoutHeight((int) animation.getAnimatedValue());
                }
            });
            resizePanelValueAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    setState(UPDATING);
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    setState(mExpanded ? EXPANDED : COLLAPSED);
                }
            });

            resizePanelValueAnimator.start();
        } else {
            setState(UPDATING);
            setContentLayoutHeight(mExpanded ? mExpandedHeight : mCollapsedHeight);
            setState(mExpanded ? EXPANDED : COLLAPSED);
        }
    }

    public void notifyContentHeightChanged() {
        measureHeights();
        checkHeights();
    }

    public void toggle() {
        toggle(true);
    }

    public void expand() {
        expand(true);
    }

    public void collapse() {
        collapse(true);
    }

    public void expand(boolean animate) {
        if (!mExpanded) toggle(animate);
    }

    public void collapse(boolean animate) {
        if (mExpanded) toggle(animate);
    }

    public void toggle(boolean animate) {
        setExpanded(!mExpanded, animate);
    }

    public boolean isExpanded() {
        return mExpanded;
    }

    public void setExpanded(boolean expanded, final boolean animate) {
        mExpanded = expanded;

        setToggleOnCollapsedClickEnabled(mToggleOnCollapsedClickEnabled);
        setToggleOnExpandedClickEnabled(mToggleOnExpandedClickEnabled);

        final View hidingView;
        final View showingView;

        if (mExpanded) {
            hidingView = mCollapsedView;
            showingView = mExpandedView;

            measureExpandedHeight();

            setState(EXPANDING);
        } else {
            hidingView = mExpandedView;
            showingView = mCollapsedView;

            measureCollapsedHeight();

            setState(COLLAPSING);
        }

        if (animate && isShown() && mDurationToggle > 0) {
            final ValueAnimator resizePanelAnimator = ValueAnimator.ofInt(
                    getContentLayoutHeight(),
                    mExpanded ? mExpandedHeight : mCollapsedHeight);
            resizePanelAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
            resizePanelAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    setContentLayoutHeight((int) animation.getAnimatedValue());
                }
            });

            final PropertyValuesHolder holderPaddingTop = ofInt(PROPERTY_NAME_PADDING_TOP,
                    mMainLayout.getPaddingTop(),
                    mExpanded ? mExpandedPaddingTop : mCollapsedPaddingTop);
            final PropertyValuesHolder holderPaddingBottom = ofInt(PROPERTY_NAME_PADDING_BOTTOM,
                    mMainLayout.getPaddingBottom(),
                    mExpanded ? mExpandedPaddingBottom : mCollapsedPaddingBottom);

            final ValueAnimator resizePaddingAnimator = ValueAnimator.ofPropertyValuesHolder(
                    holderPaddingTop, holderPaddingBottom);
            resizePaddingAnimator.setInterpolator(new LinearInterpolator());
            resizePaddingAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    setMainLayoutPadding(
                            (int) animation.getAnimatedValue(PROPERTY_NAME_PADDING_TOP),
                            (int) animation.getAnimatedValue(PROPERTY_NAME_PADDING_BOTTOM));
                }
            });

            final float stateIconRotation = mStateIcon.getRotation();

            final ValueAnimator rotateIconAnimator = ValueAnimator.ofFloat(
                    stateIconRotation,
                    mExpanded ? 180f : 0f);
            rotateIconAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
            rotateIconAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    mStateIcon.setRotation((float) animation.getAnimatedValue());
                }
            });

            final long delayCollapsedViewHiding;
            final long durationCollapsedViewChangeVisibility;

            if (mDelayCollapsedViewHiding > mDurationToggle ||
                    mDurationCollapsedViewChangeVisibility > mDurationToggle ||
                    mDelayCollapsedViewHiding + mDurationCollapsedViewChangeVisibility > mDurationToggle) {
                delayCollapsedViewHiding = 0;
                durationCollapsedViewChangeVisibility = 0;
            } else {
                delayCollapsedViewHiding = mDelayCollapsedViewHiding;
                durationCollapsedViewChangeVisibility = mDurationCollapsedViewChangeVisibility;
            }

            final long duration;
            final long delay;

            final long durationHide;
            final long durationShow;

            final long durationExpandedViewChangeVisibility =
                    mDurationToggle - delayCollapsedViewHiding - durationCollapsedViewChangeVisibility;

            if (mExpanded) {
                if (stateIconRotation == 0f) {
                    duration = mDurationToggle;

                    delay = delayCollapsedViewHiding;
                    durationHide = durationCollapsedViewChangeVisibility;
                    durationShow = durationExpandedViewChangeVisibility;
                } else {
                    duration = (long) (mDurationToggle * (180f - stateIconRotation) / 180f);

                    if (duration <= durationExpandedViewChangeVisibility) {
                        delay = 0;
                        durationHide = 0;
                        durationShow = duration;
                    } else if (duration < (durationCollapsedViewChangeVisibility + durationExpandedViewChangeVisibility)) {
                        delay = 0;
                        durationHide = duration - durationExpandedViewChangeVisibility;
                        durationShow = durationExpandedViewChangeVisibility;
                    } else {
                        delay = duration - (durationCollapsedViewChangeVisibility + durationExpandedViewChangeVisibility);
                        durationHide = durationCollapsedViewChangeVisibility;
                        durationShow = durationExpandedViewChangeVisibility;
                    }
                }
            } else {
                delay = 0;

                if (stateIconRotation == 180f) {
                    duration = mDurationToggle;

                    durationShow = durationCollapsedViewChangeVisibility;
                    durationHide = durationExpandedViewChangeVisibility;
                } else {
                    duration = (long) (mDurationToggle * stateIconRotation / 180f);

                    if (duration <= delayCollapsedViewHiding) {
                        durationShow = 0;
                        durationHide = 0;
                    } else if (duration <= (delayCollapsedViewHiding + durationCollapsedViewChangeVisibility)) {
                        durationShow = duration - delayCollapsedViewHiding;
                        durationHide = 0;
                    } else {
                        durationShow = durationCollapsedViewChangeVisibility;
                        durationHide = duration - (delayCollapsedViewHiding + durationCollapsedViewChangeVisibility);
                    }
                }
            }

            final ValueAnimator hideAnimator;

            if (durationHide > 0) {
                hideAnimator = ValueAnimator.ofFloat(hidingView.getAlpha(), 0f);
                hideAnimator.setInterpolator(new LinearInterpolator());
                hideAnimator.setDuration(durationHide);
                hideAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        hidingView.setAlpha((float) animation.getAnimatedValue());
                    }
                });
            } else
                hideAnimator = null;

            final ValueAnimator showAnimator;

            if (durationShow > 0) {
                showAnimator = ValueAnimator.ofFloat(showingView.getAlpha(), 1f);
                showAnimator.setInterpolator(new LinearInterpolator());
                showAnimator.setDuration(durationShow);
                showAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        showingView.setAlpha((float) animation.getAnimatedValue());
                    }
                });
            } else
                showAnimator = null;

            mAnimatorSet.cancel();
            mChangeViewAnimatorSet.cancel();

            mAnimatorSet = new AnimatorSet();
            mChangeViewAnimatorSet = new AnimatorSet();

            if (delay > 0) mChangeViewAnimatorSet.setStartDelay(delay);

            if (hideAnimator != null && showAnimator != null) {
                mChangeViewAnimatorSet.playSequentially(hideAnimator, showAnimator);
            } else {
                if (showAnimator != null) mChangeViewAnimatorSet.play(showAnimator);
                if (hideAnimator != null) mChangeViewAnimatorSet.play(hideAnimator);
            }
            mChangeViewAnimatorSet.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    hidingView.setVisibility(durationHide > 0 ? VISIBLE : GONE);
                    showingView.setVisibility(durationShow > 0 ? VISIBLE : GONE);
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    hidingView.setAlpha(0f);
                    showingView.setAlpha(1f);

                    hidingView.setVisibility(GONE);
                    showingView.setVisibility(VISIBLE);
                }
            });

            mAnimatorSet.setDuration(duration);
            mAnimatorSet.playTogether(resizePanelAnimator, resizePaddingAnimator, rotateIconAnimator);
            mAnimatorSet.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    ((InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE))
                            .hideSoftInputFromWindow(getWindowToken(), 0);
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    int height = getContentLayoutHeight();
                    if (mExpanded) {
                        if (height == mExpandedHeight) setState(EXPANDED);
                    } else {
                        if (height == mCollapsedHeight) setState(COLLAPSED);
                    }
                }
            });

            mAnimatorSet.start();
            mChangeViewAnimatorSet.start();
        } else {
            hidingView.setAlpha(0f);
            showingView.setAlpha(1f);

            hidingView.setVisibility(GONE);
            showingView.setVisibility(VISIBLE);

            if (mExpanded) {
                setContentLayoutHeight(mExpandedHeight);

                setMainLayoutPadding(mExpandedPaddingTop, mExpandedPaddingBottom);

                mStateIcon.setRotation(180f);

                setState(EXPANDED);
            } else {
                setContentLayoutHeight(mCollapsedHeight);

                setMainLayoutPadding(mCollapsedPaddingTop, mCollapsedPaddingBottom);

                mStateIcon.setRotation(0f);

                setState(COLLAPSED);
            }
        }
    }

    public boolean isToggleOnExpandedClickEnabled() {
        return mToggleOnExpandedClickEnabled;
    }

    public void setToggleOnExpandedClickEnabled(boolean toggleOnExpandedClickEnabled) {
        mToggleOnExpandedClickEnabled = toggleOnExpandedClickEnabled;
        if (mExpanded) mMainLayout.setClickable(mToggleOnExpandedClickEnabled);
    }

    public boolean isToggleOnCollapsedClickEnabled() {
        return mToggleOnCollapsedClickEnabled;
    }

    public void setToggleOnCollapsedClickEnabled(boolean toggleOnCollapsedClickEnabled) {
        mToggleOnCollapsedClickEnabled = toggleOnCollapsedClickEnabled;
        if (!mExpanded) mMainLayout.setClickable(mToggleOnCollapsedClickEnabled);
    }

    public void setListener(@Nullable ExpansionPanelListener listener) {
        mListener = listener;
    }

    public boolean hasListener() {
        return mListener != null;
    }

    @State
    public int getState() {
        return mState;
    }

    private void setState(@State int state) {
        mState = state;
        if (mListener != null)
            switch (state) {
                case COLLAPSED:
                    mListener.onCollapsed(this);
                    break;
                case COLLAPSING:
                    mListener.onCollapsing(this);
                    break;
                case EXPANDED:
                    mListener.onExpanded(this);
                    break;
                case EXPANDING:
                    mListener.onExpanding(this);
                    break;
                case UPDATING:
                    break;
            }
    }

    public long getDurationToggle() {
        return mDurationToggle;
    }

    public void setDurationToggle(long durationToggle) {
        mDurationToggle = durationToggle >= 0 ?
                durationToggle : getResources().getInteger(R.integer.expansion_panel_animation_duration_toggle);
    }

    public long getDelayCollapsedViewHiding() {
        return mDelayCollapsedViewHiding;
    }

    public void setDelayCollapsedViewHiding(long delayCollapsedViewHiding) {
        mDelayCollapsedViewHiding = delayCollapsedViewHiding >= 0 ?
                delayCollapsedViewHiding : getResources().getInteger(R.integer.expansion_panel_animation_delay_collapsed_view_hiding);
    }

    public long getDurationCollapsedViewChangeVisibility() {
        return mDurationCollapsedViewChangeVisibility;
    }

    public void setDurationCollapsedViewChangeVisibility(long durationCollapsedViewChangeVisibility) {
        mDurationCollapsedViewChangeVisibility = durationCollapsedViewChangeVisibility >= 0 ?
                durationCollapsedViewChangeVisibility : getResources().getInteger(R.integer.expansion_panel_animation_duration_collapsed_view_change_visibility);
    }

    public long getDurationContentHeightChanged() {
        return mDurationContentHeightChanged;
    }

    public void setDurationContentHeightChanged(long durationContentHeightChanged) {
        mDurationContentHeightChanged = durationContentHeightChanged >= 0 ?
                durationContentHeightChanged : getResources().getInteger(R.integer.expansion_panel_animation_duration_content_height_changed);
    }

    private static class SavedState extends BaseSavedState {

        int collapsedHeight;
        int expandedHeight;

        boolean expanded;

        SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            collapsedHeight = in.readInt();
            expandedHeight = in.readInt();
            expanded = in.readInt() != 0;
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(expanded ? 1 : 0);
            out.writeInt(expandedHeight);
            out.writeInt(collapsedHeight);
        }

        public static final Parcelable.Creator<SavedState> CREATOR
                = new Parcelable.Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState ss = new SavedState(superState);
        ss.collapsedHeight = mCollapsedHeight;
        ss.expandedHeight = mExpandedHeight;
        ss.expanded = mExpanded;
        return ss;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        mCollapsedHeight = ss.collapsedHeight;
        mExpandedHeight = ss.expandedHeight;
        setExpanded(ss.expanded, false);
    }
}