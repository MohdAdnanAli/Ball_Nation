package com.example.myapplication;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Intent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

public class SmartFeatureModule extends AccessibilityService {

    private static SmartFeatureModule instance;
    private final StringBuilder screenContent = new StringBuilder();

    public static SmartFeatureModule getInstance() {
        return instance;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        AccessibilityNodeInfo source = event.getSource();
        if (source != null) {
            synchronized (screenContent) {
                screenContent.setLength(0);
                getTextFromNode(source);
            }
            source.recycle();
        }
    }

    private void getTextFromNode(AccessibilityNodeInfo node) {
        if (node == null) {
            return;
        }

        if (node.getText() != null && node.getText().length() > 0) {
            screenContent.append(node.getText()).append("\n");
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                getTextFromNode(child);
            }
        }
    }

    @Override
    public void onInterrupt() {
        // This method is called when the service is interrupted.
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED | AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        info.flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS | AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS;
        setServiceInfo(info);
    }

    @Override
    public void onDestroy() {
        instance = null;
        super.onDestroy();
    }


    public String getScreenContent() {
        synchronized (screenContent) {
            return screenContent.toString();
        }
    }
}
