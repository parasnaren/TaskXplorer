package com.example.paras.tasxplorer;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Process;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private boolean cpuTotal, cpuAM,
            memUsed, memAvailable, memFree, cached, threshold,
            settingsShown, canvasLocked, orientationChanged;
    private int intervalRead, intervalUpdate, intervalWidth, statusBarHeight, navigationBarHeight, animDuration=200,
            settingsHeight, orientation, processesMode, graphicMode;
    private float sD;
    private SharedPreferences mPrefs;
    private FrameLayout mLSettings, mLGraphicSurface, mCloseSettings;
    private LinearLayout mLParent, mLTopBar, mLMenu, mLProcessContainer, mLFeedback, mLWelcome,
            mLCPUTotal, mLCPUAM,
            mLMemUsed, mLMemAvailable, mLMemFree, mLCached, mLThreshold;
    private TextView mTVCPUTotalP, mTVCPUAMP, mTVMemoryAM,
            mTVMemTotal, mTVMemUsed, mTVMemAvailable, mTVMemFree, mTVCached, mTVThreshold,
            mTVMemUsedP, mTVMemAvailableP, mTVMemFreeP, mTVCachedP, mTVThresholdP;
    private ImageView mLButtonMenu, mLButtonRecord/*, mIVSettingsBG*/;
    private DecimalFormat mFormat = new DecimalFormat("##,###,##0"), mFormatPercent = new DecimalFormat("##0.0"),
            mFormatTime = new DecimalFormat("0.#");
    private Resources res;
    private Button mBChooseProcess, mBMemory, mBRemoveAll;
    private ToggleButton mBHide;
//    private ViewGraphic mVG;
    private SeekBar mSBRead;
    private PopupWindow mPWMenu;
    private ServiceReader mSR;
    private List<Map<String, Object>> mListSelected;
    private Intent tempIntent;
    private Thread mThread;

    private Handler mHandler = new Handler();

    private Runnable drawRunnable = new Runnable() {
        @SuppressWarnings("unchecked")
        @SuppressLint("NewApi")
        @Override
        public void run() {
            mHandler.postDelayed(this, intervalUpdate);
            if (mSR != null) { // finish() could have been called from the BroadcastReceiver

                setTextLabelCPU(null, mTVCPUTotalP, mSR.getCPUTotalP());
                if (processesMode == C.processesModeShowCPU)
                    setTextLabelCPU(null, mTVCPUAMP, mSR.getCPUAMP());
                else setTextLabelCPU(null, mTVCPUAMP, null, mSR.getMemoryAM());

                setTextLabelMemory(mTVMemUsed, mTVMemUsedP, mSR.getMemUsed());
                setTextLabelMemory(mTVMemAvailable, mTVMemAvailableP, mSR.getMemAvailable());
                setTextLabelMemory(mTVMemFree, mTVMemFreeP, mSR.getMemFree());
                setTextLabelMemory(mTVCached, mTVCachedP, mSR.getCached());
                setTextLabelMemory(mTVThreshold, mTVThresholdP, mSR.getThreshold());

                for (int n = 0; n < mLProcessContainer.getChildCount(); ++n) {
                    LinearLayout l = (LinearLayout) mLProcessContainer.getChildAt(n);
                    setTextLabelCPUProcess(l);
                    setTextLabelMemoryProcesses(l);
                }
            }
        }
    };

    private void setTextLabelCPU(TextView absolute, TextView percent, List<Float> values, @SuppressWarnings("unchecked") List<Integer>... valuesInteger) {
        if (valuesInteger.length == 1) {
            percent.setText(mFormatPercent.format(valuesInteger[0].get(0) * 100 / (float) mSR.getMemTotal()) + C.percent);
            mTVMemoryAM.setVisibility(View.VISIBLE);
            mTVMemoryAM.setText(mFormat.format(valuesInteger[0].get(0)) + C.kB);
        } else if (!values.isEmpty()) {
            percent.setText(mFormatPercent.format(values.get(0)) + C.percent);
            mTVMemoryAM.setVisibility(View.INVISIBLE);
        }
    }

    private void setTextLabelMemory(TextView absolute, TextView percent, List<String> values) {
        if (!values.isEmpty()) {
            absolute.setText(mFormat.format(Integer.parseInt(values.get(0))) + C.kB);
            percent.setText(mFormatPercent.format(Integer.parseInt(values.get(0)) * 100 / (float) mSR.getMemTotal()) + C.percent);
        }
    }

    private void setTextLabelCPUProcess(LinearLayout l) {
        Map<String, Object> entry = (Map<String, Object>) l.getTag();
        if (entry != null
                && entry.get(C.pFinalValue) != null && ((List<String>) entry.get(C.pFinalValue)).size() != 0
                && entry.get(C.pTPD) != null && !((List<String>) entry.get(C.pTPD)).isEmpty()
                && entry.get(C.pDead) == null)
            if (processesMode == C.processesModeShowCPU)
                ((TextView) l.findViewById(R.id.TVpPercentage)).setText(mFormatPercent.format(((List<String>) entry.get(C.pFinalValue)).get(0)) + C.percent);
            else ((TextView) l.findViewById(R.id.TVpPercentage)).setText(mFormatPercent.format(((List<Integer>) entry.get(C.pTPD)).get(0) * 100 / (float) mSR.getMemTotal()) + C.percent);
    }

    @SuppressWarnings("unchecked")
    private void setTextLabelMemoryProcesses(LinearLayout l) {
        TextView tv = (TextView) l.findViewById(R.id.TVpAbsolute);
        if (processesMode == C.processesModeShowCPU)
            tv.setVisibility(View.INVISIBLE);
        else {
            Map<String, Object> entry = (Map<String, Object>) l.getTag();
            if (entry != null
                    && entry.get(C.pTPD) != null && !((List<String>) entry.get(C.pTPD)).isEmpty()
                    && entry.get(C.pDead) == null) {
                tv.setVisibility(View.VISIBLE);
                tv.setText(mFormat.format(((List<String>) entry.get(C.pTPD)).get(0)) + C.kB);
            }
        }
    }

    private void switchParameter(boolean draw, LinearLayout labelRow) {
        if (mSR == null)
            return;

        mPrefs.edit()
                .putBoolean(C.cpuTotal, cpuTotal)
                .putBoolean(C.cpuAM, cpuAM)

                .putBoolean(C.memUsed, memUsed)
                .putBoolean(C.memAvailable, memAvailable)
                .putBoolean(C.memFree, memFree)
                .putBoolean(C.cached, cached)
                .putBoolean(C.threshold, threshold)

                .apply();

        ImageView icon = (ImageView) labelRow.getChildAt(0);
        if (draw)
            icon.setImageResource(R.drawable.icon_play);
        else icon.setImageResource(R.drawable.icon_pause);

//		mHandlerVG.post(drawRunnableGraphic);
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startService(new Intent(this, ServiceReader.class));
        setContentView(R.layout.activity_main);

        mPrefs = getSharedPreferences(getString(R.string.app_name) + C.prefs, MODE_PRIVATE);
        intervalRead = mPrefs.getInt(C.intervalRead, C.defaultIntervalUpdate);
        intervalUpdate = mPrefs.getInt(C.intervalUpdate, C.defaultIntervalUpdate);
        intervalWidth = mPrefs.getInt(C.intervalWidth, C.defaultIntervalWidth);

        cpuTotal = mPrefs.getBoolean(C.cpuTotal, true);
        cpuAM = mPrefs.getBoolean(C.cpuAM, true);

        memUsed = mPrefs.getBoolean(C.memUsed, true);
        memAvailable = mPrefs.getBoolean(C.memAvailable, true);
        memFree = mPrefs.getBoolean(C.memFree, false);
        cached = mPrefs.getBoolean(C.cached, false);
        threshold = mPrefs.getBoolean(C.threshold, true);

        res = getResources();
        sD = res.getDisplayMetrics().density;
        //		sWidth = res.getDisplayMetrics().widthPixels;
        //		sHeight = res.getDisplayMetrics().heightPixels;
        sD = res.getDisplayMetrics().density;
        orientation = res.getConfiguration().orientation;
        statusBarHeight = res.getDimensionPixelSize(res.getIdentifier(C.sbh, C.dimen, C.android));


        processesMode = mPrefs.getInt(C.processesMode, C.processesModeShowCPU);
        mBMemory = (Button) findViewById(R.id.BMemory);
        mBMemory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                processesMode = processesMode == C.processesModeShowCPU ? C.processesModeShowMemory : C.processesModeShowCPU;
                mPrefs.edit().putInt(C.processesMode, processesMode).apply();
                mBMemory.setText(processesMode == 0 ? getString(R.string.w_main_memory) : getString(R.string.p_cpuusage));
                mHandler.removeCallbacks(drawRunnable);
                mHandler.post(drawRunnable);
            }
        });
        mBMemory.setText(processesMode == 0 ? getString(R.string.w_main_memory) : getString(R.string.p_cpuusage));

        mLCPUTotal = (LinearLayout) findViewById(R.id.LCPUTotal);
        mLCPUTotal.setTag(C.cpuTotal);
        mLCPUTotal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchParameter(cpuTotal = !cpuTotal, mLCPUTotal);
            }
        });

        mLCPUAM = (LinearLayout) findViewById(R.id.LCPUAM);
        mLCPUAM.setTag(C.cpuAM);
        mLCPUAM.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchParameter(cpuAM = !cpuAM, mLCPUAM);
            }
        });
        ((TextView) ((LinearLayout) mLCPUAM.getChildAt(2)).getChildAt(1)).setText("Pid: " + Process.myPid());

        mLMemUsed = (LinearLayout) findViewById(R.id.LMemUsed);
        mLMemUsed.setTag(C.memUsed);
        mLMemUsed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchParameter(memUsed = !memUsed, mLMemUsed);
            }
        });

        mLMemAvailable = (LinearLayout) findViewById(R.id.LMemAvailable);
        mLMemAvailable.setTag(C.memAvailable);
        mLMemAvailable.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchParameter(memAvailable = !memAvailable, mLMemAvailable);
            }
        });

        mTVCPUTotalP = (TextView) findViewById(R.id.TVCPUTotalP);
        mTVCPUAMP = (TextView) findViewById(R.id.TVCPUAMP);
        mTVMemoryAM = (TextView) findViewById(R.id.TVMemoryAM);
        mTVMemTotal = (TextView) findViewById(R.id.TVMemTotal);
        mTVMemUsed = (TextView) findViewById(R.id.TVMemUsed);
        mTVMemUsedP = (TextView) findViewById(R.id.TVMemUsedP);
        mTVMemAvailable = (TextView) findViewById(R.id.TVMemAvailable);
        mTVMemAvailableP = (TextView) findViewById(R.id.TVMemAvailableP);

        mBChooseProcess = (Button) findViewById(R.id.BChooseProcess);
        mBChooseProcess.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(MainActivity.this, ActivityProcesses.class);
                i.putExtra(C.listSelected, (Serializable) mListSelected);
                startActivityForResult(i, 1);
            }
        });



    }
}
