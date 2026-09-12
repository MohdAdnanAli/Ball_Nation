package com.example.myapplication;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.os.Handler;
import java.util.Calendar;

public class FloatingBallService extends Service {

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
    private SharedPreferences sharedPreferences;

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
        floatingBall.setImageResource(R.drawable.ball);
        Animation glowAnimation = AnimationUtils.loadAnimation(this, R.anim.glow_multicolor_animation);
        floatingBall.startAnimation(glowAnimation);

        greetingText = new TextView(this);
        greetingText.setText(getGreeting());
        greetingText.setBackgroundResource(R.drawable.speech_bubble);
        greetingText.setVisibility(View.GONE);

        ballParams = new WindowManager.LayoutParams(
                100,
                100,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        ballParams.gravity = Gravity.TOP | Gravity.LEFT;
        ballParams.x = sharedPreferences.getInt("ball_x", 0);
        ballParams.y = sharedPreferences.getInt("ball_y", 100);

        final WindowManager.LayoutParams textParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        textParams.gravity = Gravity.TOP | Gravity.LEFT;
        textParams.x = 0;
        textParams.y = 0;

        windowManager.addView(floatingBall, ballParams);
        windowManager.addView(greetingText, textParams);

        startInactivityTimer();

        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                resetInactivityTimer();
                tapCount++;
                if (tapCount == 1) {
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (tapCount == 1) {
                                greetingText.setText(getGreeting());
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
                        resetInactivityTimer();
                        ballParams.x = initialX + (int) (event.getRawX() - initialTouchX);
                        ballParams.y = initialY + (int) (event.getRawY() - initialTouchY);
                        textParams.x = ballParams.x - (greetingText.getWidth() - floatingBall.getWidth()) / 2;
                        textParams.y = ballParams.y - greetingText.getHeight();
                        windowManager.updateViewLayout(floatingBall, ballParams);
                        windowManager.updateViewLayout(greetingText, textParams);
                        return true;
                }
                return false;
            }
        });
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
        inactivityHandler.removeCallbacks(inactivityRunnable);

        if (ballParams != null) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt("ball_x", ballParams.x);
            editor.putInt("ball_y", ballParams.y);
            editor.apply();
        }

        if (floatingBall != null) {
            windowManager.removeView(floatingBall);
        }
        if (greetingText != null) {
            windowManager.removeView(greetingText);
        }
    }
}
