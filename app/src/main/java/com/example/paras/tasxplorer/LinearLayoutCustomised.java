package com.example.paras.tasxplorer;

/**
 * Created by Paras on 24-10-2019.
 */

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.LinearLayout;


public class LinearLayoutCustomised extends LinearLayout {
    private boolean touchEventsDisabled = true;

    public LinearLayoutCustomised(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return touchEventsDisabled;
    }

    public void interceptChildTouchEvents(boolean b) {
        touchEventsDisabled = b;
    }
}
