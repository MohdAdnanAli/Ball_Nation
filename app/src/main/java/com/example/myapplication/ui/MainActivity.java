package com.example.myapplication.ui;

import android.animation.AnimatorSet;
import android.animation.AnimatorInflater;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ToggleButton;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;
import com.example.myapplication.core.FloatingBallService;

public class MainActivity extends AppCompatActivity {

    private static final int OVERLAY_PERMISSION_REQ_CODE = 1234;
    private ToggleButton toggleButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ImageView floatingBallPreview = findViewById(R.id.floating_ball_preview);

        AnimatorSet breathingAnimation = (AnimatorSet) AnimatorInflater.loadAnimator(this, R.animator.breathing);
        breathingAnimation.setTarget(floatingBallPreview);
        breathingAnimation.start();

        toggleButton = findViewById(R.id.toggle_button);
        toggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    checkOverlayPermission();
                } else {
                    stopService(new Intent(MainActivity.this, FloatingBallService.class));
                }
            }
        });
    }

    private void checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, OVERLAY_PERMISSION_REQ_CODE);
        } else {
            startFloatingBallService();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == OVERLAY_PERMISSION_REQ_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.canDrawOverlays(this)) {
                    startFloatingBallService();
                } else {
                    Toast.makeText(this, "Permission not granted, bubble cannot be displayed.", Toast.LENGTH_SHORT).show();
                    toggleButton.setChecked(false);
                }
            }
        }
    }

    private void startFloatingBallService() {
        startService(new Intent(MainActivity.this, FloatingBallService.class));
    }
}
