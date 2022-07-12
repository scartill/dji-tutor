package com.flyvercity.rps.dji;

import androidx.appcompat.app.AppCompatActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.TextureView;
import android.widget.Toast;

import dji.common.product.Model;
import dji.sdk.base.BaseProduct;
import dji.sdk.camera.VideoFeeder;
import dji.sdk.codec.DJICodecManager;

public class CameraActivity extends AppCompatActivity implements TextureView.SurfaceTextureListener {
    private static final String TAG = "DJIDEMO";

    private DJICodecManager codec_manager = null;

    private TextureView video = null;
    private BroadcastReceiver receiver = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);


        video = (TextureView) findViewById(R.id.video);
        video.setSurfaceTextureListener(this);

        IntentFilter filter = new IntentFilter();
        filter.addAction(ApplicationContext.FLAG_CONNECTION_CHANGE);
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                onProductChange();
            }
        };
        registerReceiver(receiver, filter);

        initPreviewer();
    }

    protected void onProductChange() {
        initPreviewer();
    }

    private void initPreviewer() {
        BaseProduct product = ApplicationContext.getInstance().getProductInstance();

        if (product == null || !product.isConnected()) {
            showToast("Disconnected");
        } else {
            if (null != video) {
                video.setSurfaceTextureListener(this);
            }
            if (!product.getModel().equals(Model.UNKNOWN_AIRCRAFT)) {
                VideoFeeder
                        .getInstance()
                        .getPrimaryVideoFeed()
                        .addVideoDataListener((videoBuffer, size) -> {
                            if (codec_manager != null) {
                                codec_manager.sendDataToDecoder(videoBuffer, size);
                            }
                        });
            }
        }
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        Log.e(TAG, "onSurfaceTextureAvailable");
        if (codec_manager == null) {
            codec_manager = new DJICodecManager(this, surface, width, height);
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        Log.e(TAG, "onSurfaceTextureSizeChanged");
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        Log.e(TAG,"onSurfaceTextureDestroyed");
        if (codec_manager != null) {
            codec_manager.cleanSurface();
            codec_manager = null;
        }

        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
    }

    private void showToast(final String toastMsg) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(() -> Toast.makeText(
                getApplicationContext(),
                toastMsg, Toast.LENGTH_LONG).show());
    }
}