package com.abidstudio.webwrap;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

/**
 * Nampilin bubble bulat kecil yang mengambang di atas aplikasi lain.
 * Butuh izin SYSTEM_ALERT_WINDOW yang udah di-grant lewat AndroidBridge.requestOverlayPermission().
 * Bubble bisa di-drag, dan tap buat balik ke aplikasi.
 */
public class OverlayService extends Service {

    public static final String ACTION_SHOW = "SHOW";
    public static final String ACTION_HIDE = "HIDE";

    private WindowManager windowManager;
    private View bubbleView;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_HIDE.equals(intent.getAction())) {
            removeBubble();
            stopSelf();
        } else {
            showBubble();
        }
        return START_NOT_STICKY;
    }

    private void showBubble() {
        if (bubbleView != null) return;

        windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);

        int size = dp(56);
        TextView bubble = new TextView(this);
        bubble.setText("●");
        bubble.setTextColor(Color.WHITE);
        bubble.setTextSize(20);
        bubble.setGravity(Gravity.CENTER);
        bubble.setBackgroundColor(Color.parseColor("#7C6CFF"));
        bubbleView = bubble;

        int overlayType = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                size, size,
                overlayType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 0;
        params.y = 200;

        windowManager.addView(bubbleView, params);

        bubbleView.setOnTouchListener(new View.OnTouchListener() {
            private int initialX, initialY;
            private float initialTouchX, initialTouchY;
            private boolean moved = false;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        moved = false;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        int dx = (int) (event.getRawX() - initialTouchX);
                        int dy = (int) (event.getRawY() - initialTouchY);
                        if (Math.abs(dx) > 6 || Math.abs(dy) > 6) moved = true;
                        params.x = initialX + dx;
                        params.y = initialY + dy;
                        windowManager.updateViewLayout(bubbleView, params);
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (!moved) {
                            // Tap bubble -> balik ke aplikasi
                            Intent launch = getPackageManager().getLaunchIntentForPackage(getPackageName());
                            if (launch != null) {
                                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                                startActivity(launch);
                            }
                        }
                        return true;
                }
                return false;
            }
        });
    }

    private void removeBubble() {
        if (bubbleView != null && windowManager != null) {
            windowManager.removeView(bubbleView);
            bubbleView = null;
        }
    }

    private int dp(int value) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }

    @Override
    public void onDestroy() {
        removeBubble();
        super.onDestroy();
    }
}
