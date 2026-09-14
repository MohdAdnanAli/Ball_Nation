package com.example.myapplication.features;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

public class SmartFeatureModule extends AccessibilityService {

    private static SmartFeatureModule instance;
    private final StringBuilder screenContent = new StringBuilder();
    private String currentApp = "";

    public static final String ACTION_PAYMENT_APP_OPENED = "com.example.myapplication.ACTION_PAYMENT_APP_OPENED";
    public static final String ACTION_PAYMENT_APP_CLOSED = "com.example.myapplication.ACTION_PAYMENT_APP_CLOSED";

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            String packageName = event.getPackageName() != null ? event.getPackageName().toString() : "";
            if (!packageName.equals(currentApp)) {
                boolean wasPaymentApp = PaymentSecurityModule.isPaymentApp(currentApp);
                boolean isPaymentApp = PaymentSecurityModule.isPaymentApp(packageName);

                if (wasPaymentApp && !isPaymentApp) {
                    sendBroadcast(new Intent(ACTION_PAYMENT_APP_CLOSED));
                } else if (!wasPaymentApp && isPaymentApp) {
                    sendBroadcast(new Intent(ACTION_PAYMENT_APP_OPENED));
                }
                currentApp = packageName;
            }
        }

        AccessibilityNodeInfo source = event.getSource();
        if (source != null) {
            synchronized (screenContent) {
                screenContent.setLength(0);
                getTextFromNode(source);
            }
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
    }

    @Override
    public void onDestroy() {
        instance = null;
        super.onDestroy();
    }
    
    public static SmartFeatureModule getInstance() {
        return instance;
    }

    public String getScreenContent() {
        synchronized (screenContent) {
            return screenContent.toString();
        }
    }
}
