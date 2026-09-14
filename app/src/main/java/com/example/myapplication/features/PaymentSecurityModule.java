package com.example.myapplication.features;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class PaymentSecurityModule {

    private static final Set<String> PAYMENT_APP_PACKAGES = new HashSet<>(Arrays.asList(
            "com.google.android.apps.wallet", // Google Pay
            "com.paypal.android.p2p",          // PayPal
            "com.venmo",                       // Venmo
            "com.squareup.cash",               // Cash App
            "net.one97.paytm",                 // Paytm
            "com.phonepe.app"                  // PhonePe
    ));

    public static boolean isPaymentApp(String packageName) {
        if (packageName == null) {
            return false;
        }
        return PAYMENT_APP_PACKAGES.contains(packageName);
    }
}
