package com.flyvercity.rps.dji;

import android.app.Application;
import android.content.Context;

import com.secneo.sdk.Helper;

public class MApplication extends Application {

    private ApplicationContext context = null;

    @Override
    protected void attachBaseContext(Context paramContext) {
        super.attachBaseContext(paramContext);
        Helper.install(MApplication.this);
        if (context == null) {
            context = ApplicationContext.create(this);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        context.onCreate();
    }
}
