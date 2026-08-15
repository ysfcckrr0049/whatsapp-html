package com.yusuf.sarjkilidi.v5;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class MainActivity extends Activity {
    private TextView plugStatus, overlayStatus, adminStatus;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        buildUi();
        startGuardService();
    }

    @Override protected void onResume() {
        super.onResume();
        refreshStatus();
        startGuardService();
    }

    private void buildUi() {
        FrameLayout frame = new FrameLayout(this);

        ImageView bg = new ImageView(this);
        bg.setImageResource(R.drawable.bg_baby);
        bg.setScaleType(ImageView.ScaleType.CENTER_CROP);
        frame.addView(bg, new FrameLayout.LayoutParams(-1,-1));

        View dim = new View(this);
        dim.setBackgroundColor(Color.argb(165,0,0,0));
        frame.addView(dim, new FrameLayout.LayoutParams(-1,-1));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        frame.addView(scroll, new FrameLayout.LayoutParams(-1,-1));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(24),dp(20),dp(24),dp(20));
        scroll.addView(root, new ScrollView.LayoutParams(-1,-2));

        TextView title = tv("ŞARJ KİLİDİ — 60 SANİYE", 25, true, Color.WHITE);
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        TextView info = tv(
            "Fiş çıkarılınca 60 saniye geri sayım başlar. Şarj akımı gerekmez; fişin takılı görünmesi yeterlidir. " +
            "Kilit açıldıktan sonra fiş hâlâ takılı değilse sayaç her seferinde tekrar tam 60 saniyeden başlar.",
            16,false,Color.WHITE);
        info.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(-1,-2);
        ip.topMargin=dp(10); ip.bottomMargin=dp(14);
        root.addView(info, ip);

        plugStatus = tv("Fiş durumu kontrol ediliyor…",18,true,Color.WHITE);
        plugStatus.setGravity(Gravity.CENTER); root.addView(plugStatus);

        overlayStatus = tv("Ekran üstü izni kontrol ediliyor…",15,false,Color.WHITE);
        overlayStatus.setGravity(Gravity.CENTER); root.addView(overlayStatus);

        adminStatus = tv("Kilit izni kontrol ediliyor…",15,false,Color.WHITE);
        adminStatus.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(-1,-2);
        ap.bottomMargin=dp(12); root.addView(adminStatus,ap);

        Button overlay = btn("1 — Ekran Üstü Uyarı İznini Aç");
        overlay.setOnClickListener(v -> {
            Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:"+getPackageName()));
            startActivity(i);
        });
        root.addView(overlay, bp());

        Button admin = btn("2 — Kilit İznini Aç (Cihaz Yöneticisi)");
        admin.setOnClickListener(v -> {
            ComponentName c = new ComponentName(this, AdminReceiver.class);
            Intent i = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            i.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN,c);
            i.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "Fiş 60 saniye takılı olmazsa ekranı kilitlemek için gereklidir.");
            startActivity(i);
        });
        root.addView(admin,bp());

        Button start = btn("3 — Koruma Servisini Başlat / Yenile");
        start.setOnClickListener(v -> { startGuardService(); refreshStatus(); });
        root.addView(start,bp());

        Button battery = btn("4 — Pil Optimizasyonu Ayarlarını Aç");
        battery.setOnClickListener(v -> {
            try { startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)); }
            catch(Exception ignored){}
        });
        root.addView(battery,bp());

        setContentView(frame);
    }

    private void startGuardService() {
        Intent i = new Intent(this, ChargeGuardService.class);
        try {
            if(Build.VERSION.SDK_INT>=26) startForegroundService(i); else startService(i);
        } catch(Exception ignored){}
    }

    private void refreshStatus() {
        boolean p = isPlugged(this);
        plugStatus.setText(p ? "✓ FİŞ TAKILI — sayaç çalışmaz" : "⚠ FİŞ TAKILI DEĞİL — 60 sn sayaç");
        plugStatus.setTextColor(p ? Color.rgb(130,255,130) : Color.rgb(255,180,180));

        boolean ov = Build.VERSION.SDK_INT<23 || Settings.canDrawOverlays(this);
        overlayStatus.setText(ov ? "✓ Ekran üstü uyarı izni açık" : "⚠ Ekran üstü uyarı izni kapalı");

        DevicePolicyManager dpm=(DevicePolicyManager)getSystemService(DEVICE_POLICY_SERVICE);
        boolean adm=dpm!=null && dpm.isAdminActive(new ComponentName(this,AdminReceiver.class));
        adminStatus.setText(adm ? "✓ Cihaz yöneticisi / kilit izni aktif" : "⚠ Cihaz yöneticisi / kilit izni kapalı");
    }

    static boolean isPlugged(Context c) {
        Intent b=c.registerReceiver(null,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        return b!=null && b.getIntExtra(BatteryManager.EXTRA_PLUGGED,0)!=0;
    }

    private TextView tv(String s,int sp,boolean bold,int color){
        TextView v=new TextView(this); v.setText(s); v.setTextSize(sp); v.setTextColor(color);
        if(bold)v.setTypeface(Typeface.DEFAULT_BOLD); return v;
    }
    private Button btn(String s){ Button b=new Button(this); b.setText(s); b.setAllCaps(false); b.setTextSize(16); return b; }
    private LinearLayout.LayoutParams bp(){ LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(52)); p.topMargin=dp(8); return p; }
    private int dp(int v){ return (int)(v*getResources().getDisplayMetrics().density+0.5f); }
}

