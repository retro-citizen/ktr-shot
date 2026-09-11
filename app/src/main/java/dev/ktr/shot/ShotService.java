package dev.ktr.shot;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.SharedPreferences;
import android.media.MediaActionSound;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Display;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ShotService extends AccessibilityService {
    static ShotService instance;
    String inputStatus = "等待连接";
    String shotStatus = "尚未截图";
    String lastEvent = "尚未收到按键";
    boolean inputReady;
    int learning;
    private boolean enabled;
    private boolean pending;
    private long lastCapture = -1000;
    private int learningSession;
    private KeyChord chord;
    private int firstCode;
    private int secondCode;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService writer = Executors.newSingleThreadExecutor();
    private MediaActionSound shutterSound;

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            shutterSound = new MediaActionSound();
            shutterSound.load(MediaActionSound.SHUTTER_CLICK);
        } catch (RuntimeException failure) {
            android.util.Log.w("KtrShot", "Shutter sound unavailable", failure);
        }
    }

    @Override
    protected void onServiceConnected() {
        instance = this;
        reload();
    }

    void reload() {
        main.post(() -> {
            if (instance != this) return;
            SharedPreferences prefs = Config.prefs(this);
            enabled = prefs.getBoolean("enabled", true);
            learning = 0;
            inputReady = false;
            firstCode = prefs.getInt("first", 139);
            secondCode = prefs.getBoolean("single", false) ? 0 : prefs.getInt("second", 314);
            chord = new KeyChord(firstCode, secondCode);
            AccessibilityServiceInfo info = getServiceInfo();
            info.flags &= ~AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS;
            if (enabled) info.flags |= AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS;
            setServiceInfo(info);
            inputStatus = enabled ? "无障碍按键模式 · 全局监听中" : "快捷键已暂停";
        });
    }

    void learn(int slot) {
        int current = ++learningSession;
        learning = slot;
        chord.reset();
        lastEvent = "等待按下第 " + slot + " 个键…";
        main.postDelayed(() -> {
            if (learning == slot && current == learningSession) {
                learning = 0;
                lastEvent = "学习超时，未修改按键";
            }
        }, 12000);
    }

    private void acceptKey(int code, int value) {
        if (!enabled || code <= 0) return;
        if (value == 1 || value == 0) {
            lastEvent = Config.keyName(code) + (value == 1 ? " · 按下" : " · 松开");
        }
        if (learning != 0) {
            if (value != 1) return;
            SharedPreferences prefs = Config.prefs(this);
            String key = learning == 1 ? "first" : "second";
            String otherKey = learning == 1 ? "second" : "first";
            int other = prefs.getInt(otherKey, learning == 1 ? 314 : 139);
            SharedPreferences.Editor edit = prefs.edit().putInt(key, code);
            if (code == other) {
                edit.putInt(otherKey, prefs.getInt(key, learning == 1 ? 139 : 314));
            }
            learning = 0;
            edit.apply();
            reload();
            return;
        }
        if (chord.accept(code, value)) requestShot(Config.prefs(this).getInt("delay", 80));
    }

    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        int code = event.getScanCode();
        if (!enabled || code <= 0 || (learning == 0 && code != firstCode && code != secondCode)) return false;
        if (event.isCanceled()) {
            chord.reset();
            return false;
        }
        if (event.getAction() != KeyEvent.ACTION_DOWN && event.getAction() != KeyEvent.ACTION_UP) return false;
        inputReady = true;
        acceptKey(code, event.getAction() == KeyEvent.ACTION_UP ? 0 : event.getRepeatCount() > 0 ? 2 : 1);
        return false;
    }

    void requestShot(int delay) {
        if (pending || SystemClock.uptimeMillis() - lastCapture < 800) return;
        pending = true;
        shotStatus = "正在截图…";
        main.postDelayed(() -> {
            if (instance != this) { pending = false; return; }
            lastCapture = SystemClock.uptimeMillis();
            try {
                takeScreenshot(Display.DEFAULT_DISPLAY, getMainExecutor(), new TakeScreenshotCallback() {
                    @Override
                    public void onSuccess(ScreenshotResult result) {
                        writer.execute(() -> {
                            try {
                                Uri uri = ScreenshotStore.save(ShotService.this, result);
                                Config.prefs(ShotService.this).edit().putString("lastUri", uri.toString()).apply();
                                main.post(() -> {
                                    if (instance != ShotService.this) return;
                                    pending = false;
                                    shotStatus = "已保存到 Pictures/KTRShots · "
                                            + java.time.LocalTime.now().withNano(0);
                                    playShutterSound();
                                });
                            } catch (Exception failure) {
                                main.post(() -> failed("保存失败：" + failure.getMessage()));
                            }
                        });
                    }

                    @Override
                    public void onFailure(int errorCode) {
                        failed(errorCode == ERROR_TAKE_SCREENSHOT_SECURE_WINDOW
                                ? "当前窗口禁止截图（安全/DRM 内容）"
                                : "系统截图失败，错误码：" + errorCode);
                    }
                });
            } catch (Exception failure) {
                failed("截图失败：" + failure.getMessage());
            }
        }, delay);
    }

    private void playShutterSound() {
        if (shutterSound == null) return;
        try {
            shutterSound.play(MediaActionSound.SHUTTER_CLICK);
        } catch (RuntimeException failure) {
            android.util.Log.w("KtrShot", "Unable to play shutter sound", failure);
        }
    }

    private void failed(String message) {
        pending = false;
        shotStatus = message;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {}

    @Override
    public void onInterrupt() {
        if (chord != null) chord.reset();
    }

    @Override
    public void onDestroy() {
        if (instance == this) instance = null;
        main.removeCallbacksAndMessages(null);
        writer.shutdown();
        if (shutterSound != null) {
            shutterSound.release();
            shutterSound = null;
        }
        super.onDestroy();
    }
}