package org.ioquake.android;

import android.content.Context;
import android.hardware.input.InputManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.InputDevice;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import org.libsdl.app.SDLActivity;

public class MainActivity extends SDLActivity implements InputManager.InputDeviceListener {

    private static final String TAG = "MainActivity";
    private TouchOverlayView mTouchOverlay;
    private InputManager mInputManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Ensure fullscreen & cutout flags stick after super.onCreate()
        applyEdgeToEdgeFullscreen();

        // Ensure baseq3 directory exists in app external files directory with correct permissions
        java.io.File extFiles = getExternalFilesDir(null);
        if (extFiles != null) {
            java.io.File baseq3 = new java.io.File(extFiles, "baseq3");
            if (!baseq3.exists()) {
                baseq3.mkdirs();
            }
        }

        // Attach touch controls overlay on top of SDL game surface
        mTouchOverlay = new TouchOverlayView(this);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        );
        mLayout.addView(mTouchOverlay, params);

        // Register InputManager listener for real-time external peripheral detection
        mInputManager = (InputManager) getSystemService(Context.INPUT_SERVICE);
        if (mInputManager != null) {
            mInputManager.registerInputDeviceListener(this, null);
        }
        updatePeripheralStatus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        applyEdgeToEdgeFullscreen();
        updatePeripheralStatus();
    }

    @Override
    protected void onDestroy() {
        if (mInputManager != null) {
            mInputManager.unregisterInputDeviceListener(this);
        }
        super.onDestroy();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            applyEdgeToEdgeFullscreen();
        }
    }

    @Override
    public void onInputDeviceAdded(int deviceId) {
        Log.i(TAG, "Input device added: " + deviceId);
        updatePeripheralStatus();
    }

    @Override
    public void onInputDeviceRemoved(int deviceId) {
        Log.i(TAG, "Input device removed: " + deviceId);
        updatePeripheralStatus();
    }

    @Override
    public void onInputDeviceChanged(int deviceId) {
        updatePeripheralStatus();
    }

    private void updatePeripheralStatus() {
        if (mTouchOverlay == null) return;

        boolean hasPeripheral = false;
        int[] deviceIds = InputDevice.getDeviceIds();
        for (int id : deviceIds) {
            InputDevice dev = InputDevice.getDevice(id);
            if (dev == null || dev.isVirtual()) continue;

            // Filter out internal phone hardware (e.g. fingerprint sensor uinput-fpc, volume/power buttons, etc.)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (!dev.isExternal()) {
                    continue;
                }
            } else {
                String name = dev.getName();
                if (name != null) {
                    String lower = name.toLowerCase();
                    if (lower.contains("fpc") || lower.contains("fingerprint") || lower.contains("gpio") || lower.contains("snd-card")) {
                        continue;
                    }
                }
            }

            int sources = dev.getSources();
            boolean isGamepad = (sources & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD
                             || (sources & InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK;
            boolean isKeyboard = (sources & InputDevice.SOURCE_KEYBOARD) == InputDevice.SOURCE_KEYBOARD
                              && dev.getKeyboardType() == InputDevice.KEYBOARD_TYPE_ALPHABETIC;
            boolean isMouse = (sources & InputDevice.SOURCE_MOUSE) == InputDevice.SOURCE_MOUSE;

            if (isGamepad) {
                hasPeripheral = true;
                Log.i(TAG, "Detected external Gamepad/Joystick: " + dev.getName());
                break;
            }

            if (isKeyboard || isMouse) {
                hasPeripheral = true;
                Log.i(TAG, "Detected external Keyboard/Mouse: " + dev.getName());
                break;
            }
        }

        final boolean peripheralActive = hasPeripheral;
        runOnUiThread(() -> mTouchOverlay.setPeripheralMode(peripheralActive));
    }

    private void applyEdgeToEdgeFullscreen() {
        Window window = getWindow();
        if (window == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            WindowManager.LayoutParams lp = window.getAttributes();
            lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            window.setAttributes(lp);
        }

        View decorView = window.peekDecorView();
        if (decorView == null) {
            decorView = window.getDecorView();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false);
            WindowInsetsController controller = window.getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.systemBars());
                controller.setSystemBarsBehavior(
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                );
            }
        } else {
            int flags = View.SYSTEM_UI_FLAG_FULLSCREEN
                      | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                      | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                      | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                      | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                      | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
            if (decorView != null) {
                decorView.setSystemUiVisibility(flags);
            }
            window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        setWindowStyle(true);
    }

    @Override
    protected String[] getLibraries() {
        return new String[] {
            "SDL2",
            "ioquake3"
        };
    }

    @Override
    protected String getMainFunction() {
        return "SDL_main";
    }
}
