package com.flyvercity.rps.dji;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.flyvercity.rps.dji.databinding.ActivityMainBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import dji.common.error.DJIError;
import dji.common.error.DJISDKError;
import dji.sdk.base.BaseComponent;
import dji.sdk.base.BaseProduct;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKInitEvent;
import dji.sdk.sdkmanager.DJISDKManager;

// TODO: Extract registration to ApplicationContext
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "DJIDEMO";


    // TODO: extract these to some kind of permissions manager
    private static final String[] REQUIRED_PERMISSION_LIST = new String[]{
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.VIBRATE,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.READ_PHONE_STATE,
    };

    private final List<String> missingPermission = new ArrayList<>();
    private final AtomicBoolean isRegistrationInProgress = new AtomicBoolean(false);
    private static final int REQUEST_PERMISSION_CODE = 12345;

    private Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        // When the compile and target version is higher than 22, please request the following permission at runtime to ensure the SDK works well.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkAndRequestPermissions();
        }

        Button register = (Button) findViewById(R.id.button_register);
        register.setOnClickListener(view -> startSDKRegistration());

        //Initialize DJI SDK Manager
        mHandler = new Handler(Looper.getMainLooper());
        notifyStatusChange();

        Button open = (Button) findViewById(R.id.button_open);
        open.setOnClickListener(view -> {
            Intent intent = new Intent(this, CameraActivity.class);
            startActivity(intent);
        });
    }

    /**
     * Checks if there is any missing permissions, and
     * requests runtime permission if needed.
     */
    private void checkAndRequestPermissions() {
        // Check for permissions
        for (String eachPermission : REQUIRED_PERMISSION_LIST) {
            if (ContextCompat.checkSelfPermission(this, eachPermission) != PackageManager.PERMISSION_GRANTED) {
                missingPermission.add(eachPermission);
            }
        }
        // Request for missing permissions
        if (missingPermission.isEmpty()) {
            showToast("Permissions OK");
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            showToast("Need to grant the permissions!");
            ActivityCompat.requestPermissions(this,
                    missingPermission.toArray(new String[missingPermission.size()]),
                    REQUEST_PERMISSION_CODE);
        }

    }

    /**
     * Result of runtime permission request
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // Check for granted permission and remove from missing list
        if (requestCode == REQUEST_PERMISSION_CODE) {
            for (int i = grantResults.length - 1; i >= 0; i--) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    missingPermission.remove(permissions[i]);
                }
            }
        }
        // If there is enough permission, we will start the registration
        if (missingPermission.isEmpty()) {
            showToast("Permissions added");
        } else {
            showToast("Missing permissions!!!");
        }
    }

    private void startSDKRegistration() {
        Log.d(TAG, "Start registration called");
        if (isRegistrationInProgress.compareAndSet(false, true)) {
            AsyncTask.execute(() -> {
                showToast("Registering, please wait...");

                // TODO: refactor out anonymous class
                DJISDKManager.getInstance().registerApp(MainActivity.this.getApplicationContext(), new DJISDKManager.SDKManagerCallback() {
                    @Override
                    public void onRegister(DJIError djiError) {
                        if (djiError == DJISDKError.REGISTRATION_SUCCESS) {
                            showToast("Register Success");
                            DJISDKManager.getInstance().startConnectionToProduct();
                        } else {
                            showToast("Register SDK fails, please check the bundle id and network connection!");
                        }
                        Log.v(TAG, djiError.getDescription());
                    }

                    @Override
                    public void onProductDisconnect() {
                        Log.d(TAG, "onProductDisconnect");
                        showToast("Product Disconnected");
                        notifyStatusChange();
                    }

                    @Override
                    public void onProductConnect(BaseProduct baseProduct) {
                        Log.d(TAG, String.format("onProductConnect newProduct:%s", baseProduct));
                        showToast("Product Connected");
                        notifyStatusChange();
                    }

                    @Override
                    public void onProductChanged(BaseProduct baseProduct) { }

                    @Override
                    public void onComponentChange(BaseProduct.ComponentKey componentKey, BaseComponent oldComponent,
                                                  BaseComponent newComponent) {
                        if (newComponent != null) {
                            newComponent.setComponentListener(isConnected -> {
                                Log.d(TAG, "onComponentConnectivityChanged: " + isConnected);
                                notifyStatusChange();
                            });
                        }
                        Log.d(TAG,
                                String.format("onComponentChange key:%s, oldComponent:%s, newComponent:%s",
                                        componentKey,
                                        oldComponent,
                                        newComponent));

                    }

                    @Override
                    public void onInitProcess(DJISDKInitEvent djisdkInitEvent, int i) { }

                    @Override
                    public void onDatabaseDownloadProgress(long l, long l1) { }
                });
            });
        }
    }

    private void notifyStatusChange() {
        runOnUiThread(this::refreshUI);
        mHandler.removeCallbacks(updateRunnable);
        mHandler.postDelayed(updateRunnable, 500);
    }

    private final Runnable updateRunnable = () -> {
        Intent intent = new Intent(ApplicationContext.FLAG_CONNECTION_CHANGE);
        sendBroadcast(intent);
    };

    private void showToast(final String toastMsg) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(() -> Toast.makeText(
                getApplicationContext(),
                toastMsg, Toast.LENGTH_LONG).show());
    }

    private void refreshUI() {
        BaseProduct mProduct = ApplicationContext.getInstance().getProductInstance();
        Button button_open = (Button) findViewById(R.id.button_open);
        TextView text_conn = (TextView) findViewById(R.id.text_connection_status);
        TextView text_product = (TextView) findViewById(R.id.text_product);

        if (null != mProduct && mProduct.isConnected()) {

            Log.v(TAG, "refreshSDK: True");
            button_open.setEnabled(true);

            String str = mProduct instanceof Aircraft ? "DJIAircraft" : "DJIHandHeld";
            text_conn.setText("Status: " + str + " connected");

            if (null != mProduct.getModel()) {
                text_product.setText("" + mProduct.getModel().getDisplayName());
            } else {
                text_product.setText("Product Information");
            }

        } else {

            Log.v(TAG, "refreshSDK: False");
            button_open.setEnabled(false);

            text_product.setText("Product Information");
            text_conn.setText("Connection Lost");
        }
    }
}
