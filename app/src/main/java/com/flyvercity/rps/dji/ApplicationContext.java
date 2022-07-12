package com.flyvercity.rps.dji;

import android.app.Application;
import android.util.Log;

import dji.sdk.base.BaseProduct;
import dji.sdk.sdkmanager.DJISDKManager;

public class ApplicationContext {
    public static final String FLAG_CONNECTION_CHANGE = "dji_sdk_connection_change";

    private static ApplicationContext instance;

    public static ApplicationContext getInstance() {
        return instance;
    }

    public static ApplicationContext create(Application app) {
        instance = new ApplicationContext(app);
        return instance;
    }

    protected ApplicationContext(Application app) {
    }

    public void onCreate() {
        Log.d("DJIDEMO", "Application context created");
    }

    public synchronized BaseProduct getProductInstance() {
        return DJISDKManager.getInstance().getProduct();
    }
}
