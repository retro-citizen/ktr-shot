package dev.ktr.shot;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private LinearLayout content;
    private TextView status;
    private TextView events;
    private TextView capture;
    private Button first;
    private Button second;
    private final Handler main = new Handler();
    private final Runnable refresh = new Runnable() {
        @Override public void run() { renderStatus(); main.postDelayed(this, 500); }
    };

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(245, 247, 246));
        getWindow().setNavigationBarColor(Color.rgb(245, 247, 246));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(24), dp(18), dp(24), dp(24));
        content.setBackgroundColor(Color.rgb(245, 247, 246));
        scroll.addView(content);
        setContentView(scroll);
        text("KTR Shot", 26, Color.rgb(20, 70, 52));
        status = text("", 15, Color.DKGRAY);
        button("打开无障碍设置", () -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        text("快捷键", 19, Color.BLACK);
        SharedPreferences prefs = Config.prefs(this);
        Switch enabled = new Switch(this);
        enabled.setText("启用截图快捷键");
        enabled.setMinHeight(dp(48));
        enabled.setChecked(prefs.getBoolean("enabled", true));
        content.addView(enabled);
        enabled.setOnCheckedChangeListener((view, checked) -> update("enabled", checked));
        radios(new String[]{"组合键", "单键"}, prefs.getBoolean("single", false) ? 1 : 0,
                index -> update("single", index == 1));
        first = button("", () -> learn(1));
        second = button("", () -> learn(2));
        events = text("", 14, Color.DKGRAY);
        button("恢复 MENU + SELECT", () -> {
            prefs.edit().putInt("first", 139).putInt("second", 314).putBoolean("single", false).apply();
            if (ShotService.instance != null) ShotService.instance.reload();
            recreate();
        });
        TextView delay = text("截图延迟：" + prefs.getInt("delay", 80) + " ms", 15, Color.BLACK);
        SeekBar slider = new SeekBar(this);
        slider.setMax(500);
        slider.setProgress(prefs.getInt("delay", 80));
        content.addView(slider);
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onStartTrackingTouch(SeekBar view) {}
            @Override public void onProgressChanged(SeekBar view, int progress, boolean fromUser) {
                delay.setText("截图延迟：" + progress + " ms");
            }
            @Override public void onStopTrackingTouch(SeekBar view) {
                prefs.edit().putInt("delay", view.getProgress()).apply();
            }
        });
        text("截图", 19, Color.BLACK);
        capture = text("", 14, Color.DKGRAY);
        button("测试截图（2 秒后）", () -> {
            if (ShotService.instance == null) toast("请先开启 KTR Shot 无障碍服务");
            else ShotService.instance.requestShot(2000);
        }).setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_camera, 0, 0, 0);
        button("打开最近截图", () -> {
            String saved = prefs.getString("lastUri", "");
            if (saved.isEmpty()) { toast("尚无截图"); return; }
            try {
                startActivity(new Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse(saved), "image/png")
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION));
            } catch (Exception failure) { toast("无法打开图片，请在相册查看 Pictures/KTRShots"); }
        }).setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_gallery, 0, 0, 0);
    }

    private void learn(int slot) {
        if (ShotService.instance == null) toast("请先开启无障碍服务");
        else if (!Config.prefs(this).getBoolean("enabled", true)) toast("请先启用快捷键");
        else ShotService.instance.learn(slot);
    }

    private void update(String key, boolean value) {
        Config.prefs(this).edit().putBoolean(key, value).apply();
        if (ShotService.instance != null) ShotService.instance.reload();
    }

    private void renderStatus() {
        if (status == null) return;
        ShotService service = ShotService.instance;
        status.setText("无障碍：" + (service == null ? "未开启" : "已连接")
                + "\n" + (service == null ? "等待截图服务" : service.inputStatus));
        events.setText(service == null ? "尚未收到按键" : service.lastEvent);
        capture.setText(service == null ? "Pictures/KTRShots" : service.shotStatus);
        SharedPreferences prefs = Config.prefs(this);
        if (first != null && second != null) {
            first.setText("学习按键 1：" + Config.keyName(prefs.getInt("first", 139)));
            second.setText("学习按键 2：" + Config.keyName(prefs.getInt("second", 314)));
            second.setVisibility(prefs.getBoolean("single", false) ? View.GONE : View.VISIBLE);
        }
    }

    private TextView text(String value, int size, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setPadding(0, dp(10), 0, dp(8));
        content.addView(view);
        return view;
    }

    private Button button(String label, Runnable action) {
        Button view = new Button(this);
        view.setText(label);
        view.setAllCaps(false);
        view.setMinHeight(dp(48));
        view.setOnClickListener(ignored -> action.run());
        content.addView(view, new LinearLayout.LayoutParams(-1, -2));
        return view;
    }

    private void radios(String[] labels, int selected, java.util.function.IntConsumer callback) {
        RadioGroup group = new RadioGroup(this);
        group.setOrientation(LinearLayout.VERTICAL);
        for (int index = 0; index < labels.length; index++) {
            RadioButton choice = new RadioButton(this);
            choice.setId(View.generateViewId());
            choice.setText(labels[index]);
            choice.setMinHeight(dp(44));
            group.addView(choice);
            if (index == selected) group.check(choice.getId());
        }
        group.setOnCheckedChangeListener((parent, id) -> callback.accept(parent.indexOfChild(parent.findViewById(id))));
        content.addView(group);
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private void toast(String message) { Toast.makeText(this, message, Toast.LENGTH_LONG).show(); }

    @Override protected void onResume() { super.onResume(); main.post(refresh); }
    @Override protected void onPause() {
        main.removeCallbacks(refresh);
        if (ShotService.instance != null) ShotService.instance.learning = 0;
        super.onPause();
    }
}