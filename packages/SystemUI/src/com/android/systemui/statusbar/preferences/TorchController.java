
package com.android.systemui.statusbar.preferences;

import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.view.View;

import com.android.systemui.R;

public class TorchController extends SettingsController {
    private Camera mCamera;
    private SurfaceTexture mSurfaceTexture;
    private WakeLock mWakeLock;
    private boolean mTorchEnabled = false;

    public TorchController(Context context, View button) {
        super(context, button);
        mContext = context;
        getIcons(R.drawable.toggle_torch_off, R.drawable.toggle_torch);
        updateController();
    }

    protected int getPreferenceStatus() {
        return mTorchEnabled ? 1 : 0;
    }

    protected void setPreferenceStatus(int status) {
        if (mCamera == null) {
            mCamera = Camera.open();
        }

        if (status == 1) {
            if (mSurfaceTexture == null) {
                int[] textures = new int[1];
                GLES20.glGenTextures(1, textures, 0);
                GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                        textures[0]);
                GLES20.glTexParameterf(
                        GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                        GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
                GLES20.glTexParameterf(
                        GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                        GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
                GLES20.glTexParameteri(
                        GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                        GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
                GLES20.glTexParameteri(
                        GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                        GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);

                mSurfaceTexture = new SurfaceTexture(textures[0]);
                try {
                    mCamera.setPreviewTexture(mSurfaceTexture);
                } catch (Exception e) {
                    mTorchEnabled = false;
                }
                mCamera.startPreview();
            }

            Camera.Parameters mParams = mCamera.getParameters();
            mParams.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
            mCamera.setParameters(mParams);

            PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
            if (mWakeLock == null) {
                mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "QS_Torch");
            }
            if (!mWakeLock.isHeld()) {
                mWakeLock.acquire();
            }
            mTorchEnabled = true;
        } else {
            Camera.Parameters mParams = mCamera.getParameters();
            mParams.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            mCamera.setParameters(mParams);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
            mSurfaceTexture = null;

            if (mWakeLock != null && mWakeLock.isHeld()) {
                mWakeLock.release();
            }
            mWakeLock = null;
            mTorchEnabled = false;
        }
    }
}
