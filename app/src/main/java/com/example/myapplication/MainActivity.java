package com.example.myapplication;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.content.Intent;
import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ToggleButton;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ImageView floatingBallPreview = findViewById(R.id.floating_ball_preview);

        AnimatorSet breathingAnimation = (AnimatorSet) AnimatorInflater.loadAnimator(this, R.animator.breathing);
        breathingAnimation.setTarget(floatingBallPreview);
        breathingAnimation.start();

        ToggleButton toggleButton = findViewById(R.id.toggle_button);
        toggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    startService(new Intent(MainActivity.this, FloatingBallService.class));
                } else {
                    stopService(new Intent(MainActivity.this, FloatingBallService.class));
                }
            }
        });
    }
}
