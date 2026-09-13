package biz.shikuro.rucoyfloat;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final int REQ_OVERLAY = 1001;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(24), dp(24), dp(24), dp(24));
        root.setBackgroundColor(Color.rgb(245, 245, 245));

        TextView title = new TextView(this);
        title.setText("Rucoy Float");
        title.setTextSize(28);
        title.setTextColor(Color.BLACK);
        title.setGravity(Gravity.CENTER);

        TextView info = new TextView(this);
        info.setText(
                "Menjalankan Rucoy di virtual display lalu menampilkannya sebagai floating window.\n\n" +
                "Butuh:\n• Root (su)\n• Izin tampil di atas aplikasi lain\n• Rucoy Online terpasang"
        );
        info.setTextSize(15);
        info.setTextColor(Color.DKGRAY);
        info.setGravity(Gravity.CENTER);
        info.setPadding(0, dp(18), 0, dp(18));

        Button start = new Button(this);
        start.setText("START RUCOY FLOAT");
        start.setOnClickListener(v -> startFloating());

        Button stop = new Button(this);
        stop.setText("STOP");
        stop.setOnClickListener(v ->
                stopService(new Intent(this, FloatingRucoyService.class)));

        root.addView(title, new LinearLayout.LayoutParams(-1, -2));
        root.addView(info, new LinearLayout.LayoutParams(-1, -2));
        root.addView(start, new LinearLayout.LayoutParams(-1, -2));
        root.addView(stop, new LinearLayout.LayoutParams(-1, -2));

        setContentView(root);
    }

    private void startFloating() {
        if (!RootShell.available()) {
            Toast.makeText(this,
                    "Root shell gagal. Pastikan Rucoy Float diberi akses root.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        if (!Settings.canDrawOverlays(this)) {
            Intent i = new Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(i, REQ_OVERLAY);
            return;
        }

        startForegroundService(new Intent(this, FloatingRucoyService.class));
        Toast.makeText(this, "Rucoy Float dimulai.", Toast.LENGTH_SHORT).show();
        moveTaskToBack(true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_OVERLAY && Settings.canDrawOverlays(this)) {
            startFloating();
        }
    }

    private int dp(int value) {
        float d = getResources().getDisplayMetrics().density;
        return (int) (value * d + 0.5f);
    }
}
