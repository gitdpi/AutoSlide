package com.autoslide;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import androidx.core.app.NotificationCompat;

public class AutoSlideService extends Service {

    private WindowManager windowManager;
    private View floatingView;
    private WindowManager.LayoutParams params;
    private boolean isSliding = false;
    private Handler handler;
    private Runnable slideRunnable;

    @Override
    public void onCreate() {
        super.onCreate();

        try {
            // 创建通知渠道
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel("auto_slide_channel", "Auto Slide", NotificationManager.IMPORTANCE_LOW);
                NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                if (manager != null) {
                    manager.createNotificationChannel(channel);
                }
            }

            // 显示前台通知
            Notification notification = new NotificationCompat.Builder(this, "auto_slide_channel")
                    .setContentTitle("AutoSlide")
                    .setContentText("自动滑动服务运行中")
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .build();
            startForeground(1, notification);

            // 初始化悬浮窗
            initFloatingView();

            // 初始化自动滑动
            handler = new Handler(Looper.getMainLooper());
            slideRunnable = new Runnable() {
                @Override
                public void run() {
                    if (isSliding) {
                        performSlide();
                        // 随机延迟时间，2-5秒之间
                        int delay = 2000 + (int)(Math.random() * 3000);
                        handler.postDelayed(this, delay);
                    }
                }
            };
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initFloatingView() {
        try {
            windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
            floatingView = LayoutInflater.from(this).inflate(R.layout.floating_view, null);

            params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                            WindowManager.LayoutParams.TYPE_PHONE,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                    PixelFormat.TRANSLUCENT
            );

            params.gravity = Gravity.TOP | Gravity.LEFT;
            params.x = 100;
            params.y = 100;

            windowManager.addView(floatingView, params);

            // 悬浮窗点击事件
            ImageView icon = floatingView.findViewById(R.id.floatingIcon);
            icon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    toggleSliding();
                }
            });

            // 悬浮窗拖动事件
            icon.setOnTouchListener(new View.OnTouchListener() {
                private int initialX;
                private int initialY;
                private float initialTouchX;
                private float initialTouchY;
                private boolean isDragging = false;

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            initialX = params.x;
                            initialY = params.y;
                            initialTouchX = event.getRawX();
                            initialTouchY = event.getRawY();
                            isDragging = false;
                            return true;
                        case MotionEvent.ACTION_MOVE:
                            int dx = (int) (event.getRawX() - initialTouchX);
                            int dy = (int) (event.getRawY() - initialTouchY);
                            // 判断是否是拖动操作
                            if (Math.abs(dx) > 5 || Math.abs(dy) > 5) {
                                isDragging = true;
                                params.x = initialX + dx;
                                params.y = initialY + dy;
                                windowManager.updateViewLayout(floatingView, params);
                            }
                            return true;
                        case MotionEvent.ACTION_UP:
                            // 如果不是拖动，则触发点击事件
                            if (!isDragging) {
                                v.performClick();
                            }
                            return true;
                        default:
                            return false;
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void toggleSliding() {
        try {
            isSliding = !isSliding;
            if (floatingView != null) {
                ImageView icon = floatingView.findViewById(R.id.floatingIcon);
                if (isSliding) {
                    icon.setImageResource(android.R.drawable.ic_media_play); // 切换图标表示正在滑动
                    handler.post(slideRunnable);
                } else {
                    icon.setImageResource(android.R.drawable.ic_media_pause); // 切换图标表示已停止
                    handler.removeCallbacks(slideRunnable);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void performSlide() {
        try {
            // 获取屏幕尺寸
            if (windowManager != null) {
                DisplayMetrics metrics = new DisplayMetrics();
                windowManager.getDefaultDisplay().getMetrics(metrics);
                int screenHeight = metrics.heightPixels;
                int screenWidth = metrics.widthPixels;

                // 使用辅助功能服务执行滑动操作
                AutoSlideAccessibilityService accessibilityService = AutoSlideAccessibilityService.getInstance();
                if (accessibilityService != null) {
                    // 模拟从屏幕底部向上滑动
                    int startX = screenWidth / 2;
                    int startY = screenHeight * 3 / 4;
                    int endX = screenWidth / 2;
                    int endY = screenHeight / 4;
                    int duration = 500; // 滑动持续时间
                    accessibilityService.performSwipe(startX, startY, endX, endY, duration);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            if (floatingView != null && windowManager != null) {
                windowManager.removeView(floatingView);
            }
            if (handler != null) {
                handler.removeCallbacks(slideRunnable);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}