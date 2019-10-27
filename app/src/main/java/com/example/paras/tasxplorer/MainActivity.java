package com.example.paras.tasxplorer;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.LayoutTransition;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Process;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewManager;
import android.view.ViewStub;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

public class MainActivity extends Activity implements ActivityCompat.OnRequestPermissionsResultCallback {

    private boolean cpuTotal, cpuAM,
            memUsed, memAvailable, memFree, cached, threshold,
            settingsShown, canvasLocked, orientationChanged;
    private int intervalRead, intervalUpdate, intervalWidth, statusBarHeight, navigationBarHeight, animDuration = 200,
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

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @SuppressLint("NewApi")
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            mSR = ((ServiceReader.ServiceReaderDataBinder) service).getService();

//            setIconRecording();

            mTVMemTotal.setText(mFormat.format(mSR.getMemTotal()) + C.kB);

            switchParameter(cpuTotal, mLCPUTotal);
            switchParameter(cpuAM, mLCPUAM);

            switchParameter(memUsed, mLMemUsed);
            switchParameter(memAvailable, mLMemAvailable);
            switchParameter(memFree, mLMemFree);
            switchParameter(cached, mLCached);
            switchParameter(threshold, mLThreshold);

            mHandler.removeCallbacks(drawRunnable);
            mHandler.post(drawRunnable);

            // When on ActivityProcesses the screen is rotated, ActivityMain is destroyed and back is pressed from ActivityProcesses
            // mSR isn't ready before onActivityResult() is called. So the Intent is saved till mSR is ready.
            if (tempIntent != null) {
                tempIntent.putExtra(C.screenRotated, true);
                onActivityResult(1, 1, tempIntent);
                tempIntent = null;
            } else onActivityResult(1, 1, null);

            if (Build.VERSION.SDK_INT >= 16) {
                mLProcessContainer.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @SuppressWarnings("deprecation")
                    @Override
                    public void onGlobalLayout() {
                        mLProcessContainer.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                        LayoutTransition lt = new LayoutTransition();
                        lt.enableTransitionType(LayoutTransition.APPEARING);
                        lt.enableTransitionType(LayoutTransition.DISAPPEARING);
                        lt.enableTransitionType(LayoutTransition.CHANGING);
                        mLProcessContainer.setLayoutTransition(lt);
                        LayoutTransition lt2 = new LayoutTransition();
                        lt2.enableTransitionType(LayoutTransition.CHANGING);
                        lt2.setStartDelay(LayoutTransition.CHANGING, 300);
                        ((LinearLayout) mLProcessContainer.getParent()).setLayoutTransition(lt2);
                    }
                });
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            mSR = null;
        }
    };

    private BroadcastReceiver receiverSetIconRecord = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            setIconRecording();
        }
    }, receiverDeadProcess = new BroadcastReceiver() {
        @SuppressWarnings("unchecked")
        @Override
        public void onReceive(Context context, Intent intent) {
            switchParameterForProcess((Map<String, Object>) intent.getSerializableExtra(C.process));
        }
    }, receiverFinish = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            finish();
        }
    };

    @Override
    public void onBackPressed() {
        if (mLFeedback != null && mLFeedback.getAlpha() != 0) {
            mPrefs.edit().putLong(C.welcomeDate, Calendar.getInstance(TimeZone.getTimeZone(C.europeLondon)).getTimeInMillis()).apply();
            Toast.makeText(MainActivity.this, getString(R.string.w_main_feedback_no_remind), Toast.LENGTH_LONG).show();
            mLFeedback.animate().setDuration(animDuration).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    ((ViewManager) mLFeedback.getParent()).removeView(mLFeedback);
                    mLFeedback = null;
                }
            }).setStartDelay(0).alpha(0).translationYBy(-15*sD);
            return;
        }
        if (mLWelcome != null && mLWelcome.getAlpha() != 0) {
            mPrefs.edit().putBoolean(C.welcome, false).apply();
            mLWelcome.animate().setDuration(animDuration).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    ((ViewManager) mLWelcome.getParent()).removeView(mLWelcome);
                    mLWelcome = null;
                }
            }).setStartDelay(0).alpha(0).translationYBy(-15*sD);
            return;
        }

        if (settingsShown) {
            mCloseSettings.performClick();
            return;
        }

        super.onBackPressed();
    }





    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode ==  KeyEvent.KEYCODE_MENU && event.getRepeatCount() == 0) {
            mPWMenu.setAnimationStyle(R.style.Animations_PopDownMenuBottom);
            mPWMenu.showAtLocation(mLParent, Gravity.BOTTOM | Gravity.CENTER,  0, 0);
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }





    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == C.storagePermission && PackageManager.PERMISSION_DENIED == grantResults[0]) {
            Toast.makeText(MainActivity.this, getString(R.string.w_main_storage_permission), Toast.LENGTH_LONG).show();
        }
    }




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
            else
                ((TextView) l.findViewById(R.id.TVpPercentage)).setText(mFormatPercent.format(((List<Integer>) entry.get(C.pTPD)).get(0) * 100 / (float) mSR.getMemTotal()) + C.percent);
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
    }


    private void setIconRecording() {
        if (mSR == null) // This can happen when stopping and closing the app from the system bar action button.
            return;
        if (mSR.isRecording()) {
            mSBRead.setEnabled(false);
            mBChooseProcess.setEnabled(false);
            mLButtonRecord.setImageResource(R.drawable.button_stop_record);
        } else {
            mSBRead.setEnabled(true);
            mBChooseProcess.setEnabled(true);
            mLButtonRecord.setImageResource(R.drawable.button_start_record);
        }
    }

    private int getColourForProcess(int n) {
        if (n==0)
            return res.getColor(R.color.process3);
        else if (n==1)
            return res.getColor(R.color.process4);
        else if (n==2)
            return res.getColor(R.color.process5);
        else if (n==3)
            return res.getColor(R.color.process6);
        else if (n==4)
            return res.getColor(R.color.process7);
        else if (n==5)
            return res.getColor(R.color.process8);
        else if (n==6)
            return res.getColor(R.color.process1);
        else if (n==7)
            return res.getColor(R.color.process2);
        n-=8;
        return getColourForProcess(n);
    }

    @SuppressLint({ "NewApi", "InflateParams" })
    @SuppressWarnings("unchecked")
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)  {
        if (requestCode == 1 && resultCode == 1) {
            // List
            // Map
            // Integer	   C.pId
            // String	   C.pName
            // Integer	   C.work
            // Integer	   C.workBefore
            // List<Sring> C.finalValue
            // Boolean	   C.pDead

            // Boolean	   C.pCheckBox
            List<Map<String, Object>> mListSelectedProv = null;
            if (data != null) {
                mListSelectedProv = (List<Map<String, Object>>) data.getSerializableExtra(C.listSelected);
                if (mListSelectedProv == null)
                    return;

                // When on ActivityProcesses the screen is rotated, ActivityMain is destroyed and back is pressed from ActivityProcesses
                // mSR isn't ready before onActivityResult() is called. So the Intent is saved till mSR is ready.
                if (mSR == null) {
                    tempIntent = data;
                    return;
                }

                for(Map<String, Object> process : mListSelectedProv) {
                    process.put(C.pColour, getColourForProcess(mSR.getProcesses() != null ? mSR.getProcesses().size() : 0));
                    mSR.addProcess(process);
                }

                mListSelected = mSR.getProcesses();

                if (data.getBooleanExtra(C.screenRotated, false))
                    mListSelectedProv = mListSelected;

            } else {
                mListSelected = mSR.getProcesses();
                mListSelectedProv = mListSelected;
            }

            if (mListSelectedProv == null)
                return;

            mBRemoveAll.setAlpha(1);
            mBRemoveAll.setVisibility(View.VISIBLE);

            synchronized (mListSelected) {
                for (final Map<String, Object> process : mListSelectedProv) {
                    if (process.get(C.pSelected) == null)
                        process.put(C.pSelected, Boolean.TRUE);

                    final LinearLayout l = (LinearLayout) getLayoutInflater().inflate(R.layout.layer_process_entry, null);
                    l.setTag(process);
                    l.setOnLongClickListener(new View.OnLongClickListener() {
                        @Override
                        public boolean onLongClick(View v) {
                            if (!mSR.isRecording()) {
                                mSR.removeProcess(process);
                                mListSelected.remove(process);
                                mLProcessContainer.removeView(l);
                                return true;
                            } else return false;
                        }
                    });
                    l.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Boolean b = (Boolean) process.get(C.pSelected);
                            process.put(C.pSelected, !b);
                            switchParameterForProcess(process);
                        }
                    });

                    Drawable d = null;
                    try {
                        d = getPackageManager().getApplicationIcon((String) process.get(C.pPackage));
                    } catch (PackageManager.NameNotFoundException e) {
                    }

                    ImageView pIcon = (ImageView) l.getChildAt(1);
                    pIcon.setImageDrawable(d);

                    int colour = (Integer) process.get(C.pColour);

                    TextView pName = (TextView) l.findViewById(R.id.TVpAppName);
                    pName.setText((String) process.get(C.pAppName));
                    pName.setTextColor(colour);

                    TextView pId = (TextView) l.findViewById(R.id.TVpName);
                    pId.setText("Pid: " + process.get(C.pId));

                    TextView pUsage = (TextView) l.findViewById(R.id.TVpPercentage);
                    pUsage.setTextColor(colour);

                    mLProcessContainer.addView(l);
                    switchParameterForProcess(process);
                }
            }
        }
        orientationChanged = false;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(C.orientation, orientation);
//        outState.putBoolean(C.menuShown, mPWMenu.isShowing());
//        outState.putBoolean(C.settingsShown, settingsShown);
//        outState.putBoolean(C.canvasLocked, canvasLocked);
    }
//
    @Override
    public void onStart() {
        super.onStart();
        bindService(new Intent(this, ServiceReader.class), mServiceConnection, 0);
        registerReceiver(receiverSetIconRecord, new IntentFilter(C.actionSetIconRecord));
        registerReceiver(receiverDeadProcess, new IntentFilter(C.actionDeadProcess));
        registerReceiver(receiverFinish, new IntentFilter(C.actionFinishActivity));
    }





    @Override
    public void onResume() {
        super.onResume();
        mHandler.removeCallbacks(drawRunnable);
        mHandler.post(drawRunnable);
    }





    @Override
    public void onPause() {
        super.onPause();
        if (mThread != null) {
            mThread.interrupt();
            mThread = null;
        }
        mHandler.removeCallbacks(drawRunnable);
    }





    @Override
    public void onStop() {
        super.onStop();
        if (mThread != null) {
            mThread.interrupt();
            mThread = null;
        }
        mHandler.removeCallbacks(drawRunnable);
    }





    @Override
    public void onDestroy() {
        super.onDestroy();
        orientationChanged = false;
        if (mThread != null) {
            mThread.interrupt();
            mThread = null;
        }
        mHandler.removeCallbacks(drawRunnable);
        if (mPWMenu.isShowing()) // To avoid android.view.WindowLeaked exception
            mPWMenu.dismiss();
        unregisterReceiver(receiverSetIconRecord);
        unregisterReceiver(receiverDeadProcess);
        unregisterReceiver(receiverFinish);
        unbindService(mServiceConnection);
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


        mBRemoveAll = (Button) findViewById(R.id.BRemoveAll);
        mBRemoveAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListSelected.clear(); // This also updates the List on ServiceReader because it is poiting to the same object
                mLProcessContainer.removeAllViews();
                mBRemoveAll.animate().setDuration(300).alpha(0).setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mBRemoveAll.setVisibility(View.GONE);
                    }
                });
            }
        });

//        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, C.storagePermission);

        if (savedInstanceState != null && !savedInstanceState.isEmpty()) {
            processesMode = savedInstanceState.getInt(C.processesMode);
            mBMemory.setText(processesMode == C.processesModeShowCPU ? getString(R.string.w_main_memory) : getString(R.string.p_cpuusage));

            canvasLocked = savedInstanceState.getBoolean(C.canvasLocked);
            settingsShown = savedInstanceState.getBoolean(C.settingsShown);
        }
    }

    private void switchParameterForProcess(Map<String, Object> process) {
        LinearLayout l = null;
        for (int n=0; n<mLProcessContainer.getChildCount(); ++n) {
            l = (LinearLayout) mLProcessContainer.getChildAt(n);
            if (((Map<String, Object>) l.getTag()).get(C.pId).equals(process.get(C.pId)))
                break;
        }
        ImageView iv = (ImageView) l.getChildAt(0);

        if (process.get(C.pDead) != null) {
            ((TextView) l.findViewById(R.id.TVpPercentage)).setText(getString(R.string.w_processes_dead));
            l.findViewById(R.id.TVpName).setAlpha(0.2f);
            l.findViewById(R.id.TVpAbsolute).setVisibility(View.INVISIBLE);
            l.getChildAt(1).setAlpha(0.3f);
        }

        if ((Boolean) process.get(C.pSelected)) {
            iv.setImageResource(R.drawable.icon_play);
            if (process.get(C.pDead) == null)
                setTextLabelCPUProcess(l);
        } else {
            iv.setImageResource(R.drawable.icon_pause);
        }
    }
}

