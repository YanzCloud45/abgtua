package biz.shikuro.rucoyfloat;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

public class FloatingRucoyService extends Service {

    private static final String CHANNEL_ID = "rucoy_float";
    private static final int NOTIF_ID = 41;

    // Virtual display resolution.
    private static final int VD_W = 1280;
    private static final int VD_H = 720;
    private static final int VD_DPI = 160;

    private WindowManager wm;
    private WindowManager.LayoutParams lp;
    private FrameLayout floatingRoot;
    private SurfaceView surfaceView;
    private VirtualDisplay virtualDisplay;
    private int displayId = -1;

    private float downRawX, downRawY;
    private int downWinX, downWinY;

    private long touchDownTime;
    private float touchDownX, touchDownY;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotification();
        buildOverlay();
    }

    private void createNotification() {
        NotificationManager nm =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID,
                    "Rucoy Float",
                    NotificationManager.IMPORTANCE_LOW
            );
            nm.createNotificationChannel(ch);
        }

        Notification n = new Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setContentTitle("Rucoy Float aktif")
                .setContentText("Virtual display sedang berjalan")
                .setOngoing(true)
                .build();

        startForeground(NOTIF_ID, n);
    }

    private void buildOverlay() {
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);

        lp = new WindowManager.LayoutParams(
                dp(360),
                dp(240),
                Build.VERSION.SDK_INT >= 26
                        ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                        : WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
        );
        lp.gravity = Gravity.TOP | Gravity.START;
        lp.x = dp(12);
        lp.y = dp(90);

        floatingRoot = new FrameLayout(this);
        floatingRoot.setBackgroundColor(Color.BLACK);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        floatingRoot.addView(box, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));

        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(8), 0, dp(4), 0);
        bar.setBackgroundColor(Color.rgb(32, 32, 32));

        TextView title = new TextView(this);
        title.setText("Rucoy Float");
        title.setTextColor(Color.WHITE);
        title.setTextSize(14);
        title.setGravity(Gravity.CENTER_VERTICAL);

        TextView restart = makeBarButton("↻");
        TextView close = makeBarButton("✕");

        LinearLayout.LayoutParams titleLp =
                new LinearLayout.LayoutParams(0, dp(36), 1f);
        bar.addView(title, titleLp);
        bar.addView(restart, new LinearLayout.LayoutParams(dp(44), dp(36)));
        bar.addView(close, new LinearLayout.LayoutParams(dp(44), dp(36)));

        surfaceView = new SurfaceView(this);
        surfaceView.setBackgroundColor(Color.BLACK);

        box.addView(bar, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(36)));
        box.addView(surfaceView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        TextView resize = new TextView(this);
        resize.setText("◢");
        resize.setTextColor(Color.WHITE);
        resize.setTextSize(18);
        resize.setGravity(Gravity.CENTER);

        FrameLayout.LayoutParams resizeLp =
                new FrameLayout.LayoutParams(dp(34), dp(34), Gravity.BOTTOM | Gravity.END);
        floatingRoot.addView(resize, resizeLp);

        bar.setOnTouchListener((v, e) -> handleWindowDrag(e));
        resize.setOnTouchListener(new ResizeTouch());
        close.setOnClickListener(v -> stopSelf());
        restart.setOnClickListener(v -> launchRucoy());

        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                createVirtualDisplay(holder.getSurface());
            }

            @Override
            public void surfaceChanged(
                    SurfaceHolder holder, int format, int width, int height) {
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                releaseVirtualDisplay();
            }
        });

        surfaceView.setOnTouchListener((v, e) -> {
            handleRucoyTouch(e);
            return true;
        });

        wm.addView(floatingRoot, lp);
    }

    private TextView makeBarButton(String text) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextColor(Color.WHITE);
        v.setTextSize(20);
        v.setGravity(Gravity.CENTER);
        return v;
    }

    private boolean handleWindowDrag(MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downRawX = e.getRawX();
                downRawY = e.getRawY();
                downWinX = lp.x;
                downWinY = lp.y;
                return true;

            case MotionEvent.ACTION_MOVE:
                lp.x = downWinX + (int) (e.getRawX() - downRawX);
                lp.y = downWinY + (int) (e.getRawY() - downRawY);
                wm.updateViewLayout(floatingRoot, lp);
                return true;
        }
        return true;
    }

    private final class ResizeTouch implements View.OnTouchListener {
        float startX, startY;
        int startW, startH;

        @Override
        public boolean onTouch(View v, MotionEvent e) {
            if (e.getActionMasked() == MotionEvent.ACTION_DOWN) {
                startX = e.getRawX();
                startY = e.getRawY();
                startW = lp.width;
                startH = lp.height;
                return true;
            }

            if (e.getActionMasked() == MotionEvent.ACTION_MOVE) {
                int newW = startW + (int) (e.getRawX() - startX);
                int newH = startH + (int) (e.getRawY() - startY);

                lp.width = Math.max(dp(220), newW);
                lp.height = Math.max(dp(150), newH);
                wm.updateViewLayout(floatingRoot, lp);
                return true;
            }
            return true;
        }
    }

    private void createVirtualDisplay(Surface surface) {
        if (virtualDisplay != null) return;

        DisplayManager dm =
                (DisplayManager) getSystemService(Context.DISPLAY_SERVICE);

        int flags =
                DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC
                | DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY
                | DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION;

        virtualDisplay = dm.createVirtualDisplay(
                "RucoyFloatDisplay",
                VD_W,
                VD_H,
                VD_DPI,
                surface,
                flags
        );

        if (virtualDisplay == null || virtualDisplay.getDisplay() == null) {
            displayId = -1;
            return;
        }

        displayId = virtualDisplay.getDisplay().getDisplayId();

        // Give Android a moment to register the display, then launch Rucoy.
        new Thread(() -> {
            try {
                Thread.sleep(500);
            } catch (InterruptedException ignored) {}
            launchRucoy();
        }).start();
    }

    private void launchRucoy() {
        if (displayId < 0) return;

        new Thread(() -> {
            // Resolve launcher activity dynamically. Avoid hardcoding Rucoy's activity.
            String resolve =
                    "cmd package resolve-activity --brief " +
                    "-a android.intent.action.MAIN " +
                    "-c android.intent.category.LAUNCHER " +
                    "com.mmo.android | tail -n 1";

            String component = RootShell.exec(resolve).trim();

            if (component.contains("/")) {
                RootShell.exec(
                        "am force-stop com.mmo.android; " +
                        "am start --display " + displayId +
                        " -n " + shell(component)
                );
            } else {
                // Fallback.
                RootShell.exec(
                        "monkey --display " + displayId +
                        " -p com.mmo.android " +
                        "-c android.intent.category.LAUNCHER 1"
                );
            }
        }).start();
    }

    private void handleRucoyTouch(MotionEvent e) {
        if (displayId < 0) return;

        float vw = Math.max(1, surfaceView.getWidth());
        float vh = Math.max(1, surfaceView.getHeight());

        float x = clamp(e.getX() * VD_W / vw, 0, VD_W - 1);
        float y = clamp(e.getY() * VD_H / vh, 0, VD_H - 1);

        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                touchDownTime = System.currentTimeMillis();
                touchDownX = x;
                touchDownY = y;
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                long dur = Math.max(1, System.currentTimeMillis() - touchDownTime);
                float dx = Math.abs(x - touchDownX);
                float dy = Math.abs(y - touchDownY);

                final int fx = Math.round(x);
                final int fy = Math.round(y);
                final int sx = Math.round(touchDownX);
                final int sy = Math.round(touchDownY);
                final long fdur = Math.min(1500, dur);

                new Thread(() -> {
                    if (dx < 12 && dy < 12 && fdur < 350) {
                        RootShell.exec(
                                "input -d " + displayId +
                                " tap " + fx + " " + fy
                        );
                    } else {
                        RootShell.exec(
                                "input -d " + displayId +
                                " swipe " + sx + " " + sy +
                                " " + fx + " " + fy +
                                " " + fdur
                        );
                    }
                }).start();
                break;
        }
    }

    private String shell(String s) {
        return "'" + s.replace("'", "'\\''") + "'";
    }

    private float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }

    private void releaseVirtualDisplay() {
        if (virtualDisplay != null) {
            virtualDisplay.release();
            virtualDisplay = null;
        }
        displayId = -1;
    }

    @Override
    public void onDestroy() {
        releaseVirtualDisplay();
        if (floatingRoot != null && wm != null) {
            try {
                wm.removeView(floatingRoot);
            } catch (Throwable ignored) {
            }
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private int dp(int value) {
        float d = getResources().getDisplayMetrics().density;
        return (int) (value * d + 0.5f);
    }
}
