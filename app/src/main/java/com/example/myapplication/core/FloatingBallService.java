package com.example.myapplication.core;

import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.os.Handler;

import com.example.myapplication.R;
import com.example.myapplication.features.SmartFeatureModule;
import com.example.myapplication.ui.MainActivity;

import java.util.Calendar;

public class FloatingBallService extends Service {

    private static final String TAG = "FloatingBallService";
    private WindowManager windowManager;
    private ImageView floatingBall;
    private TextView greetingText;
    private GestureDetector gestureDetector;
    private Handler handler;
    private int tapCount = 0;
    private SmartFeatureModule smartFeatureModule;
    private Handler inactivityHandler = new Handler();
    private boolean isPeeking = false;
    private WindowManager.LayoutParams ballParams;
    private WindowManager.LayoutParams textParams;
    private SharedPreferences sharedPreferences;
    private boolean isHiddenForPayment = false;

    private final BroadcastReceiver paymentAppReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null) {
                if (action.equals(SmartFeatureModule.ACTION_PAYMENT_APP_OPENED)) {
                    hideFloatingBall();
                    isHiddenForPayment = true;
                } else if (action.equals(SmartFeatureModule.ACTION_PAYMENT_APP_CLOSED)) {
                    showFloatingBall();
                    isHiddenForPayment = false;
                }
            }
        }
    };

    private Runnable inactivityRunnable = new Runnable() {
        @Override
        public void run() {
            floatingBall.setAlpha(0.5f);
            isPeeking = true;
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        handler = new Handler();
        smartFeatureModule = SmartFeatureModule.getInstance();
        sharedPreferences = getSharedPreferences("FloatingBallPrefs", MODE_PRIVATE);

        floatingBall = new ImageView(this);
        floatingBall.setImageResource(R.drawable.subtle_ball);

        greetingText = new TextView(this);
        greetingText.setText(getGreeting());
        greetingText.setBackgroundResource(R.drawable.speech_bubble);
        greetingText.setVisibility(View.GONE);

        ballParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        ballParams.gravity = Gravity.TOP | Gravity.LEFT;
        ballParams.x = sharedPreferences.getInt("ball_x", 0);
        ballParams.y = sharedPreferences.getInt("ball_y", 100);

        textParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        textParams.gravity = Gravity.TOP | Gravity.LEFT;
        textParams.x = 0;
        textParams.y = 0;

        try {
            windowManager.addView(floatingBall, ballParams);
            windowManager.addView(greetingText, textParams);
        } catch (Exception e) {
            Log.e(TAG, "Error adding views to window manager", e);
        }

        AnimatorSet breathingAnimation = (AnimatorSet) AnimatorInflater.loadAnimator(this, R.animator.breathing);
        breathingAnimation.setTarget(floatingBall);
        breathingAnimation.start();

        IntentFilter filter = new IntentFilter();
        filter.addAction(SmartFeatureModule.ACTION_PAYMENT_APP_OPENED);
        filter.addAction(SmartFeatureModule.ACTION_PAYMENT_APP_CLOSED);
        registerReceiver(paymentAppReceiver, filter);

        startInactivityTimer();

        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                if (isHiddenForPayment) return true;
                resetInactivityTimer();
                tapCount++;
                if (tapCount == 1) {
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (tapCount == 1) {
                                greetingText.setText(getGreeting());

                                greetingText.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                                int textWidth = greetingText.getMeasuredWidth();
                                int textHeight = greetingText.getMeasuredHeight();

                                textParams.x = ballParams.x - (textWidth - floatingBall.getWidth()) / 2;
                                textParams.y = ballParams.y - textHeight;
                                windowManager.updateViewLayout(greetingText, textParams);

                                greetingText.setVisibility(View.VISIBLE);
                                handler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        greetingText.setVisibility(View.GONE);
                                    }
                                }, 2000);
                            } else if (tapCount >= 4) {
                                Intent intent = new Intent(FloatingBallService.this, MainActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                            }
                            tapCount = 0;
                        }
                    }, 500);
                }
                return true;
            }
        });

        floatingBall.setOnTouchListener(new View.OnTouchListener() {
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (isHiddenForPayment) return true;
                if (isPeeking) {
                    floatingBall.setAlpha(1.0f);
                    isPeeking = false;
                    resetInactivityTimer();
                    return true;
                }

                gestureDetector.onTouchEvent(event);

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = ballParams.x;
                        initialY = ballParams.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        try {
                            resetInactivityTimer();
                            ballParams.x = initialX + (int) (event.getRawX() - initialTouchX);
                            ballParams.y = initialY + (int) (event.getRawY() - initialTouchY);
                            windowManager.updateViewLayout(floatingBall, ballParams);
                        } catch (Exception ex) {
                            Log.e(TAG, "Error updating view layout during move", ex);
                        }
                        return true;
                }
                return false;
            }
        });
    }

    private void hideFloatingBall() {
        floatingBall.setVisibility(View.GONE);
        greetingText.setVisibility(View.GONE);
    }

    private void showFloatingBall() {
        floatingBall.setVisibility(View.VISIBLE);
    }

    private void startInactivityTimer() {
        inactivityHandler.postDelayed(inactivityRunnable, 5000); // 5 seconds
    }

    private void resetInactivityTimer() {
        inactivityHandler.removeCallbacks(inactivityRunnable);
        startInactivityTimer();
    }

    private String getGreeting() {
        if (smartFeatureModule != null) {
            String screenContent = smartFeatureModule.getScreenContent();
            if (screenContent != null && !screenContent.isEmpty()) {
                if (screenContent.toLowerCase().contains("game")) {
                    return "Good luck with your game!";
                } else if (screenContent.toLowerCase().contains("work")) {
                    return "Hope work is going well!";
                }
            }
        }

        Calendar c = Calendar.getInstance();
        int timeOfDay = c.get(Calendar.HOUR_OF_DAY);

        if (timeOfDay >= 0 && timeOfDay < 12) {
            return "Good morning!";
        } else if (timeOfDay >= 12 && timeOfDay < 16) {
            return "Good afternoon!";
        } else if (timeOfDay >= 16 && timeOfDay < 21) {
            return "Good evening!";
        } else {
            return "Good night!";
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(paymentAppReceiver);
        inactivityHandler.removeCallbacks(inactivityRunnable);

        if (ballParams != null) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt("ball_x", ballParams.x);
            editor.putInt("ball_y", ballParams.y);
            editor.apply();
        }

        try {
            if (floatingBall != null) {
                windowManager.removeView(floatingBall);
            }
            if (greetingText != null) {
                windowManager.removeView(greetingText);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error removing views from window manager", e);
        }
    }
}
