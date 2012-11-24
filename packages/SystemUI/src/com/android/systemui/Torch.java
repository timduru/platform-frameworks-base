/*
 * Copyright 2011 Colin McDonough
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * Additional modifications by bigrushdog for TeamEos
 */

package com.android.systemui;

import java.io.IOException;
import java.util.List;

import org.teameos.jellybean.settings.EOSConstants;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.Toast;

import com.android.systemui.R;

/*
 * Torch is an LED flashlight.
 */
public class Torch extends Activity implements SurfaceHolder.Callback {

  private static final String TAG = Torch.class.getSimpleName();

  private static final String WAKE_LOCK_TAG = "TORCH_WAKE_LOCK";

  private Camera mCamera;
  private boolean lightOn;
  private boolean previewOn;
  private SurfaceView surfaceView;
  private SurfaceHolder surfaceHolder;
  private IntentFilter mFilter;
  private OffReceiver mOffReceiver;

  private WakeLock wakeLock;

  private static Torch torch;

  public Torch() {
    super();
    torch = this;
  }

  public static Torch getTorch() {
    return torch;
  }

  private void getCamera() {
    if (mCamera == null) {
      try {
        mCamera = Camera.open();
      } catch (RuntimeException e) {
        Log.i(TAG, "Camera.open() failed: " + e.getMessage());
      }
    }
  }

  private void turnLightOn() {
    if (mCamera == null) {
      Toast.makeText(this, "Camera not found", Toast.LENGTH_LONG);
      return;
    }
    lightOn = true;
    Parameters parameters = mCamera.getParameters();
    if (parameters == null) {
      return;
    }
    List<String> flashModes = parameters.getSupportedFlashModes();
    // Check if camera flash exists
    if (flashModes == null) {
      return;
    }
    String flashMode = parameters.getFlashMode();
    Log.i(TAG, "Flash mode: " + flashMode);
    Log.i(TAG, "Flash modes: " + flashModes);
    if (!Parameters.FLASH_MODE_TORCH.equals(flashMode)) {
      // Turn on the flash
      if (flashModes.contains(Parameters.FLASH_MODE_TORCH)) {
        parameters.setFlashMode(Parameters.FLASH_MODE_TORCH);
        mCamera.setParameters(parameters);
        startWakeLock();
      } else {
        Toast.makeText(this, "Flash mode (torch) not supported",
            Toast.LENGTH_LONG);
        Log.e(TAG, "FLASH_MODE_TORCH not supported");
      }
    }
  }

  private void turnLightOff() {
    if (lightOn) {
      lightOn = false;
      if (mCamera == null) {
        return;
      }
      Parameters parameters = mCamera.getParameters();
      if (parameters == null) {
        return;
      }
      List<String> flashModes = parameters.getSupportedFlashModes();
      String flashMode = parameters.getFlashMode();
      // Check if camera flash exists
      if (flashModes == null) {
        return;
      }
      Log.i(TAG, "Flash mode: " + flashMode);
      Log.i(TAG, "Flash modes: " + flashModes);
      if (!Parameters.FLASH_MODE_OFF.equals(flashMode)) {
        // Turn off the flash
        if (flashModes.contains(Parameters.FLASH_MODE_OFF)) {
          parameters.setFlashMode(Parameters.FLASH_MODE_OFF);
          mCamera.setParameters(parameters);
          stopWakeLock();
          finish();
        } else {
          Log.e(TAG, "FLASH_MODE_OFF not supported");
        }
      }
    }
  }

  private void startPreview() {
    if (!previewOn && mCamera != null) {
      mCamera.startPreview();
      previewOn = true;
    }
  }

  private void stopPreview() {
    if (previewOn && mCamera != null) {
      mCamera.stopPreview();
      previewOn = false;
    }
  }

  private void startWakeLock() {
    if (wakeLock == null) {
      Log.d(TAG, "wakeLock is null, getting a new WakeLock");
      PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
      Log.d(TAG, "PowerManager acquired");
      wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG);
      Log.d(TAG, "WakeLock set");
    }
    wakeLock.acquire();
    Log.d(TAG, "WakeLock acquired");
  }

  private void stopWakeLock() {
    if (wakeLock != null) {
      wakeLock.release();
      Log.d(TAG, "WakeLock released");
    }
  }

  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.torch);
    mFilter = new IntentFilter();
    mFilter.addAction(EOSConstants.ACTION_TORCH_OFF);
    mOffReceiver = new OffReceiver();
    this.getApplicationContext().registerReceiver(mOffReceiver, mFilter);
    surfaceView = (SurfaceView) this.findViewById(R.id.surfaceview);
    surfaceHolder = surfaceView.getHolder();
    surfaceHolder.addCallback(this);
    surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    disablePhoneSleep();
    Log.i(TAG, "onCreate");
  }

  private void disablePhoneSleep() {
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
  }

  @Override
  public void onRestart() {
    super.onRestart();
    Log.i(TAG, "onRestart");
  }

  @Override
  public void onStart() {
    super.onStart();
    Log.i(TAG, "onStart");
    getCamera();
    startPreview();
    turnLightOn();
  }

  @Override
  public void onResume() {
    super.onResume();
    Log.i(TAG, "onResume");
  }

  @Override
  public void onPause() {
    super.onPause();
    Log.i(TAG, "onPause");
  }

  @Override
  public void onStop() {
    super.onStop();
    Log.i(TAG, "onStop");
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    if (mCamera != null) {
      turnLightOff();
      stopPreview();
      mCamera.release();
	  mCamera = null;
    }
	torch = null;
    Log.i(TAG, "onDestroy");
  }

  @Override
  public void surfaceChanged(SurfaceHolder holder, int I, int J, int K) {
    moveTaskToBack(true);
    Log.d(TAG, "surfaceChanged");
  }

  @Override
  public void surfaceCreated(SurfaceHolder holder) {
    Log.d(TAG, "surfaceCreated");
    try {
      mCamera.setPreviewDisplay(holder);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void surfaceDestroyed(SurfaceHolder holder) {
    Log.d(TAG, "surfaceDestroyed");
  }

  private class OffReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action.equals(EOSConstants.ACTION_TORCH_OFF)) {
            turnLightOff();
            finish();
        }  
    }      
  }
}