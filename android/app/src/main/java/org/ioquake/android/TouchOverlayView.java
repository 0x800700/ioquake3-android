package org.ioquake.android;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import org.libsdl.app.SDLActivity;

/**
 * Modern Touch Controls Overlay for Quake III Arena on Android.
 * Provides virtual joystick (movement), visual aim touchpad (head turning/looking),
 * and action buttons (fire, jump, weapon cycling, menu, console, keyboard).
 */
public class TouchOverlayView extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // Joystick state (Left side - Movement)
    private int joystickPointerId = -1;
    private float joystickCenterX = 0;
    private float joystickCenterY = 0;
    private float joystickThumbX = 0;
    private float joystickThumbY = 0;
    private float joystickRadius = 140f;
    private final float joystickDeadZone = 25f;
    private boolean movingW = false;
    private boolean movingS = false;
    private boolean movingA = false;
    private boolean movingD = false;

    // Look state (Right side - Aiming / Looking)
    private int lookPointerId = -1;
    private float aimPadCenterX = 0;
    private float aimPadCenterY = 0;
    private float aimPadRadius = 160f;
    private float lookThumbX = 0;
    private float lookThumbY = 0;
    private float lastLookX = 0;
    private float lastLookY = 0;
    private float lookAccumX = 0f;
    private float lookAccumY = 0f;
    private float lookSensitivity = 2.2f;

    // Buttons
    public static class TouchButton {
        String label;
        RectF bounds;
        int pointerId = -1;
        Runnable onDown;
        Runnable onUp;

        TouchButton(String label, float x, float y, float r, Runnable onDown, Runnable onUp) {
            this.label = label;
            this.bounds = new RectF(x - r, y - r, x + r, y + r);
            this.onDown = onDown;
            this.onUp = onUp;
        }

        boolean contains(float x, float y) {
            return bounds.contains(x, y);
        }
    }

    private TouchButton btnFire;
    private TouchButton btnJump;
    private TouchButton btnNextWeap;
    private TouchButton btnPrevWeap;
    private TouchButton btnMenu;
    private TouchButton btnConsole;
    private TouchButton btnKeyboard;
    private TouchButton[] buttons;

    // Peripheral mode (when external gamepad/keyboard/mouse is connected)
    private boolean isPeripheralMode = false;

    public void setPeripheralMode(boolean peripheralMode) {
        if (this.isPeripheralMode != peripheralMode) {
            this.isPeripheralMode = peripheralMode;
            resetAllTouches();
            invalidate();
        }
    }

    public boolean isPeripheralMode() {
        return isPeripheralMode;
    }

    public TouchOverlayView(Context context) {
        super(context);
        paint.setStyle(Paint.Style.FILL);

        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(2.5f);
        linePaint.setColor(Color.argb(80, 255, 255, 255));

        textPaint.setColor(Color.WHITE);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        float minDim = Math.min(w, h);
        joystickRadius = minDim * 0.18f;
        aimPadRadius = minDim * 0.16f;

        // Position default movement joystick anchor on bottom-left
        joystickCenterX = w * 0.18f;
        joystickCenterY = h * 0.72f;
        joystickThumbX = joystickCenterX;
        joystickThumbY = joystickCenterY;

        // Position visual Aim / Look pad anchor in center-right
        aimPadCenterX = w * 0.60f;
        aimPadCenterY = h * 0.54f;
        lookThumbX = aimPadCenterX;
        lookThumbY = aimPadCenterY;

        // Action Buttons layout (right side)
        float btnR = minDim * 0.085f;
        textPaint.setTextSize(btnR * 0.42f);

        // Primary FIRE button (large, easily accessible by right thumb)
        // Fix cursor jumping to (0,0): relative=true ensures no mouse position reset
        // Fix sticking: state=0 passed on ACTION_UP to release mouse button 1
        btnFire = new TouchButton("FIRE", w * 0.86f, h * 0.65f, btnR * 1.35f,
            () -> SDLActivity.onNativeMouse(1, MotionEvent.ACTION_DOWN, 0, 0, true),
            () -> SDLActivity.onNativeMouse(0, MotionEvent.ACTION_UP, 0, 0, true)
        );

        // JUMP button (bottom right)
        btnJump = new TouchButton("JUMP", w * 0.72f, h * 0.82f, btnR * 1.05f,
            () -> SDLActivity.onNativeKeyDown(KeyEvent.KEYCODE_SPACE),
            () -> SDLActivity.onNativeKeyUp(KeyEvent.KEYCODE_SPACE)
        );

        // Next / Prev Weapon buttons (upper right)
        btnNextWeap = new TouchButton("WEAP >", w * 0.88f, h * 0.36f, btnR * 0.8f,
            () -> SDLActivity.onNativeKeyDown(KeyEvent.KEYCODE_RIGHT_BRACKET),
            () -> SDLActivity.onNativeKeyUp(KeyEvent.KEYCODE_RIGHT_BRACKET)
        );

        btnPrevWeap = new TouchButton("< WEAP", w * 0.76f, h * 0.36f, btnR * 0.8f,
            () -> SDLActivity.onNativeKeyDown(KeyEvent.KEYCODE_LEFT_BRACKET),
            () -> SDLActivity.onNativeKeyUp(KeyEvent.KEYCODE_LEFT_BRACKET)
        );

        // ESC / MENU button (top left corner)
        btnMenu = new TouchButton("ESC", w * 0.07f, h * 0.12f, btnR * 0.7f,
            () -> SDLActivity.onNativeKeyDown(KeyEvent.KEYCODE_ESCAPE),
            () -> SDLActivity.onNativeKeyUp(KeyEvent.KEYCODE_ESCAPE)
        );

        // Dedicated CONSOLE (~) button (top left, between ESC and KEYB)
        btnConsole = new TouchButton("~", w * 0.16f, h * 0.12f, btnR * 0.7f,
            () -> {
                SDLActivity.onNativeKeyDown(KeyEvent.KEYCODE_GRAVE);
                SDLActivity.onNativeKeyUp(KeyEvent.KEYCODE_GRAVE);
            },
            () -> {}
        );

        // Soft Keyboard Toggle button (top left, next to ~)
        btnKeyboard = new TouchButton("KEYB", w * 0.25f, h * 0.12f, btnR * 0.7f,
            () -> SDLActivity.showTextInput(0, 0, 0, 0),
            () -> {}
        );

        buttons = new TouchButton[] { btnFire, btnJump, btnNextWeap, btnPrevWeap, btnMenu, btnConsole, btnKeyboard };
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (isPeripheralMode) {
            // In peripheral mode, only the console (~) button intercepts touch events.
            // Other screen touches pass through to the underlying game surface.
            if (btnConsole == null) return false;

            int action = event.getActionMasked();
            int actionIndex = event.getActionIndex();
            int pointerId = event.getPointerId(actionIndex);
            float x = event.getX(actionIndex);
            float y = event.getY(actionIndex);

            if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
                if (btnConsole.contains(x, y) && btnConsole.pointerId == -1) {
                    btnConsole.pointerId = pointerId;
                    if (btnConsole.onDown != null) btnConsole.onDown.run();
                    invalidate();
                    return true;
                }
            } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP || action == MotionEvent.ACTION_CANCEL) {
                if (btnConsole.pointerId == pointerId) {
                    btnConsole.pointerId = -1;
                    if (btnConsole.onUp != null) btnConsole.onUp.run();
                    invalidate();
                    return true;
                }
            }
            return false;
        }

        int action = event.getActionMasked();
        int actionIndex = event.getActionIndex();
        int pointerId = event.getPointerId(actionIndex);
        float x = event.getX(actionIndex);
        float y = event.getY(actionIndex);

        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                handlePointerDown(pointerId, x, y);
                break;

            case MotionEvent.ACTION_MOVE:
                for (int i = 0; i < event.getPointerCount(); i++) {
                    int pid = event.getPointerId(i);
                    handlePointerMove(pid, event.getX(i), event.getY(i));
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_CANCEL:
                handlePointerUp(pointerId);
                if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                    resetAllTouches();
                }
                break;
        }

        invalidate();
        return true;
    }

    private void handlePointerDown(int pid, float x, float y) {
        // Check buttons first
        if (buttons != null) {
            for (TouchButton b : buttons) {
                if (b.contains(x, y) && b.pointerId == -1) {
                    b.pointerId = pid;
                    if (b.onDown != null) b.onDown.run();
                    return;
                }
            }
        }

        // Left half: Virtual Joystick (Movement)
        if (x < getWidth() * 0.44f) {
            if (joystickPointerId == -1) {
                joystickPointerId = pid;
                joystickCenterX = x;
                joystickCenterY = y;
                joystickThumbX = x;
                joystickThumbY = y;
                updateJoystickDirection(0, 0);
            }
        } else {
            // Right half: Look / Aim (Head turning)
            if (lookPointerId == -1) {
                lookPointerId = pid;
                lastLookX = x;
                lastLookY = y;
                lookThumbX = x;
                lookThumbY = y;
                lookAccumX = 0f;
                lookAccumY = 0f;
            }
        }
    }

    private void handlePointerMove(int pid, float x, float y) {
        if (pid == joystickPointerId) {
            float dx = x - joystickCenterX;
            float dy = y - joystickCenterY;
            float dist = (float) Math.sqrt(dx * dx + dy * dy);

            if (dist > joystickRadius) {
                dx = (dx / dist) * joystickRadius;
                dy = (dy / dist) * joystickRadius;
            }
            joystickThumbX = joystickCenterX + dx;
            joystickThumbY = joystickCenterY + dy;
            updateJoystickDirection(dx, dy);
        } else if (pid == lookPointerId) {
            float rawDx = (x - lastLookX) * lookSensitivity;
            float rawDy = (y - lastLookY) * lookSensitivity;
            lastLookX = x;
            lastLookY = y;
            lookThumbX = x;
            lookThumbY = y;

            lookAccumX += rawDx;
            lookAccumY += rawDy;

            int intDx = (int) lookAccumX;
            int intDy = (int) lookAccumY;

            if (intDx != 0 || intDy != 0) {
                lookAccumX -= intDx;
                lookAccumY -= intDy;
                SDLActivity.onNativeMouse(0, MotionEvent.ACTION_MOVE, intDx, intDy, true);
            }
        }
    }

    private void handlePointerUp(int pid) {
        if (buttons != null) {
            for (TouchButton b : buttons) {
                if (b.pointerId == pid) {
                    b.pointerId = -1;
                    if (b.onUp != null) b.onUp.run();
                }
            }
        }

        if (pid == joystickPointerId) {
            joystickPointerId = -1;
            joystickThumbX = joystickCenterX;
            joystickThumbY = joystickCenterY;
            updateJoystickDirection(0, 0);
        } else if (pid == lookPointerId) {
            lookPointerId = -1;
            lookThumbX = aimPadCenterX;
            lookThumbY = aimPadCenterY;
            lookAccumX = 0f;
            lookAccumY = 0f;
        }
    }

    private void resetAllTouches() {
        if (buttons != null) {
            for (TouchButton b : buttons) {
                if (b.pointerId != -1) {
                    b.pointerId = -1;
                    if (b.onUp != null) b.onUp.run();
                }
            }
        }
        if (joystickPointerId != -1) {
            joystickPointerId = -1;
            joystickThumbX = joystickCenterX;
            joystickThumbY = joystickCenterY;
            updateJoystickDirection(0, 0);
        }
        if (lookPointerId != -1) {
            lookPointerId = -1;
            lookThumbX = aimPadCenterX;
            lookThumbY = aimPadCenterY;
            lookAccumX = 0f;
            lookAccumY = 0f;
        }
    }

    private void updateJoystickDirection(float dx, float dy) {
        boolean newW = dy < -joystickDeadZone;
        boolean newS = dy > joystickDeadZone;
        boolean newA = dx < -joystickDeadZone;
        boolean newD = dx > joystickDeadZone;

        if (newW != movingW) {
            movingW = newW;
            if (movingW) SDLActivity.onNativeKeyDown(KeyEvent.KEYCODE_W);
            else SDLActivity.onNativeKeyUp(KeyEvent.KEYCODE_W);
        }
        if (newS != movingS) {
            movingS = newS;
            if (movingS) SDLActivity.onNativeKeyDown(KeyEvent.KEYCODE_S);
            else SDLActivity.onNativeKeyUp(KeyEvent.KEYCODE_S);
        }
        if (newA != movingA) {
            movingA = newA;
            if (movingA) SDLActivity.onNativeKeyDown(KeyEvent.KEYCODE_A);
            else SDLActivity.onNativeKeyUp(KeyEvent.KEYCODE_A);
        }
        if (newD != movingD) {
            movingD = newD;
            if (movingD) SDLActivity.onNativeKeyDown(KeyEvent.KEYCODE_D);
            else SDLActivity.onNativeKeyUp(KeyEvent.KEYCODE_D);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float textOffset = (textPaint.descent() + textPaint.ascent()) / 2f;

        if (isPeripheralMode) {
            // Draw only the ~ (console) toggle button
            if (btnConsole != null) {
                boolean pressed = (btnConsole.pointerId != -1);
                paint.setColor(pressed ? Color.argb(180, 255, 80, 80) : Color.argb(85, 255, 255, 255));
                float r = btnConsole.bounds.width() / 2f;
                canvas.drawCircle(btnConsole.bounds.centerX(), btnConsole.bounds.centerY(), r, paint);

                linePaint.setColor(pressed ? Color.argb(240, 255, 120, 120) : Color.argb(110, 255, 255, 255));
                canvas.drawCircle(btnConsole.bounds.centerX(), btnConsole.bounds.centerY(), r, linePaint);

                textPaint.setColor(Color.WHITE);
                canvas.drawText(btnConsole.label, btnConsole.bounds.centerX(), btnConsole.bounds.centerY() - textOffset, textPaint);
            }
            return;
        }

        // ----------------------------------------------------
        // 1. Draw Movement Joystick (Left Side)
        // ----------------------------------------------------
        paint.setColor(Color.argb(55, 255, 255, 255));
        canvas.drawCircle(joystickCenterX, joystickCenterY, joystickRadius, paint);

        linePaint.setColor(Color.argb(80, 255, 255, 255));
        canvas.drawCircle(joystickCenterX, joystickCenterY, joystickRadius, linePaint);

        // Movement Joystick center label
        textPaint.setColor(Color.argb(120, 255, 255, 255));
        canvas.drawText("MOVE", joystickCenterX, joystickCenterY - textOffset, textPaint);

        // Movement Joystick thumb knob
        paint.setColor(joystickPointerId != -1 ? Color.argb(160, 255, 255, 255) : Color.argb(90, 255, 255, 255));
        canvas.drawCircle(joystickThumbX, joystickThumbY, joystickRadius * 0.36f, paint);

        // ----------------------------------------------------
        // 2. Draw Aim / Look Zone (Right Side)
        // ----------------------------------------------------
        boolean isLooking = (lookPointerId != -1);
        paint.setColor(isLooking ? Color.argb(45, 100, 200, 255) : Color.argb(35, 255, 255, 255));
        canvas.drawCircle(aimPadCenterX, aimPadCenterY, aimPadRadius, paint);

        linePaint.setColor(isLooking ? Color.argb(140, 100, 200, 255) : Color.argb(70, 255, 255, 255));
        canvas.drawCircle(aimPadCenterX, aimPadCenterY, aimPadRadius, linePaint);
        canvas.drawCircle(aimPadCenterX, aimPadCenterY, aimPadRadius * 0.45f, linePaint);

        // Crosshairs in aim pad
        float arm = aimPadRadius * 0.3f;
        canvas.drawLine(aimPadCenterX - arm, aimPadCenterY, aimPadCenterX + arm, aimPadCenterY, linePaint);
        canvas.drawLine(aimPadCenterX, aimPadCenterY - arm, aimPadCenterX, aimPadCenterY + arm, linePaint);

        // Aim pad label
        textPaint.setColor(isLooking ? Color.argb(220, 100, 200, 255) : Color.argb(120, 255, 255, 255));
        canvas.drawText("AIM / LOOK", aimPadCenterX, aimPadCenterY - aimPadRadius * 0.65f - textOffset, textPaint);

        // Aim thumb reticle
        if (isLooking) {
            paint.setColor(Color.argb(160, 100, 200, 255));
            canvas.drawCircle(lookThumbX, lookThumbY, aimPadRadius * 0.28f, paint);
            linePaint.setColor(Color.argb(220, 255, 255, 255));
            canvas.drawCircle(lookThumbX, lookThumbY, aimPadRadius * 0.28f, linePaint);
        }

        // ----------------------------------------------------
        // 3. Draw Action Buttons
        // ----------------------------------------------------
        if (buttons != null) {
            for (TouchButton b : buttons) {
                boolean pressed = (b.pointerId != -1);
                paint.setColor(pressed ? Color.argb(180, 255, 80, 80) : Color.argb(75, 255, 255, 255));
                float r = b.bounds.width() / 2f;
                canvas.drawCircle(b.bounds.centerX(), b.bounds.centerY(), r, paint);

                linePaint.setColor(pressed ? Color.argb(240, 255, 120, 120) : Color.argb(100, 255, 255, 255));
                canvas.drawCircle(b.bounds.centerX(), b.bounds.centerY(), r, linePaint);

                textPaint.setColor(Color.WHITE);
                canvas.drawText(b.label, b.bounds.centerX(), b.bounds.centerY() - textOffset, textPaint);
            }
        }
    }
}
