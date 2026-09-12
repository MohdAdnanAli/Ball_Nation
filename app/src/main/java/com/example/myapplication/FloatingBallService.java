package com.example.myapplication;

import android.app.Service;
import android.content.Intent;
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

public class FloatingBallService extends Service {

    private WindowManager windowManager;
    private ImageView floatingBall;
    private TextView greetingText;
    private GestureDetector gestureDetector;
    private Handler handler;
    private boolean doubleTapped = false;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        handler = new Handler();

        floatingBall = new ImageView(this);
        floatingBall.setImageResource(R.drawable.ball);

        greetingText = new TextView(this);
        greetingText.setText("Hi!");
        greetingText.setBackgroundResource(R.drawable.speech_bubble);
        greetingText.setVisibility(View.GONE);


        final WindowManager.LayoutParams ballParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        ballParams.gravity = Gravity.TOP | Gravity.LEFT;
        ballParams.x = 0;
        ballParams.y = 100;

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

        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                greetingText.setVisibility(View.VISIBLE);
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        greetingText.setVisibility(View.GONE);
                    }
                }, 1000);

                doubleTapped = true;
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        doubleTapped = false;
                    }
                }, 300); // 300ms window for triple tap

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
                gestureDetector.onTouchEvent(event);

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        if (doubleTapped) {
                            Animation animation = AnimationUtils.loadAnimation(FloatingBallService.this, R.anim.catchy_animation);
                            floatingBall.startAnimation(animation);
                            doubleTapped = false;
                            return true; // Consume the event
                        }
                        initialX = ballParams.x;
                        initialY = ballParams.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;
                    case MotionEvent.ACTION_MOVE:
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (floatingBall != null) {
            windowManager.removeView(floatingBall);
        }
        if (greetingText != null) {
            windowManager.removeView(greetingText);
        }
    }
}