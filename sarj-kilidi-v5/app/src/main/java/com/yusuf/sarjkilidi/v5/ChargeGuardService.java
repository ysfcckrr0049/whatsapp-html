package com.yusuf.sarjkilidi.v5;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.admin.DevicePolicyManager;
import android.content.*;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import java.util.Locale;

public class ChargeGuardService extends Service {
    private static final String CHANNEL="sarj_kilidi_v5";
    private static final int NID=715;
    private static final long LIMIT=60000L;

    private final Handler h=new Handler(Looper.getMainLooper());
    private WindowManager wm;
    private View overlay;
    private TextView timeText,sentenceText;
    private boolean plugged=true,registered=false;
    private long deadline=0L;

    private final Runnable ticker=new Runnable(){
        @Override public void run(){
            if(plugged || deadline==0L)return;
            long left=deadline-SystemClock.elapsedRealtime();
            if(left<=0L){ timeoutLock(); return; }
            updateCountdown(left);
            h.postDelayed(this,200L);
        }
    };

    private final BroadcastReceiver receiver=new BroadcastReceiver(){
        @Override public void onReceive(Context c,Intent i){
            String a=i.getAction();
            if(Intent.ACTION_USER_PRESENT.equals(a)){
                plugged=readPlugged();
                if(plugged) pluggedState();
                else startCountdown(true);   // HER KİLİT AÇILIŞINDA TAM 60 SN
                return;
            }

            boolean now;
            if(Intent.ACTION_BATTERY_CHANGED.equals(a))
                now=i.getIntExtra(BatteryManager.EXTRA_PLUGGED,0)!=0;
            else now=readPlugged();

            if(now!=plugged){
                plugged=now;
                if(plugged) pluggedState(); else startCountdown(true);
            }else if(!plugged && deadline==0L){
                startCountdown(true);
            }
        }
    };

    @Override public void onCreate(){
        super.onCreate();
        channel();
        startForeground(NID,notification("Koruma başlatılıyor…"));
        wm=(WindowManager)getSystemService(WINDOW_SERVICE);
        registerNow();
        plugged=readPlugged();
        if(plugged) pluggedState(); else startCountdown(true);
    }

    @Override public int onStartCommand(Intent i,int f,int id){
        plugged=readPlugged();
        if(plugged) pluggedState(); else if(deadline==0L) startCountdown(true);
        return START_STICKY;
    }

    @Override public void onDestroy(){
        h.removeCallbacksAndMessages(null);
        hide();
        if(registered){ try{unregisterReceiver(receiver);}catch(Exception ignored){} registered=false; }
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent i){ return null; }

    private void registerNow(){
        if(registered)return;
        IntentFilter f=new IntentFilter();
        f.addAction(Intent.ACTION_BATTERY_CHANGED);
        f.addAction(Intent.ACTION_POWER_CONNECTED);
        f.addAction(Intent.ACTION_POWER_DISCONNECTED);
        f.addAction(Intent.ACTION_USER_PRESENT);
        registerReceiver(receiver,f);
        registered=true;
    }

    private boolean readPlugged(){
        Intent b=registerReceiver(null,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        return b!=null && b.getIntExtra(BatteryManager.EXTRA_PLUGGED,0)!=0;
    }

    private void pluggedState(){
        deadline=0L;
        h.removeCallbacks(ticker);
        hide();
        updateNotification("Fiş takılı — koruma beklemede");
    }

    private void startCountdown(boolean reset){
        if(plugged)return;
        if(reset || deadline==0L) deadline=SystemClock.elapsedRealtime()+LIMIT;
        ensureOverlay();
        updateCountdown(deadline-SystemClock.elapsedRealtime());
        h.removeCallbacks(ticker); h.post(ticker);
        updateNotification("Fiş takılı değil — 60 saniye sonra Tablet uyku moduna girer");
    }

    private void updateCountdown(long leftMs){
        long total=Math.max(0L,(leftMs+999L)/1000L);
        long min=total/60L, sec=total%60L;
        ensureOverlay();
        if(timeText!=null) timeText.setText(String.format(Locale.US,"%02d:%02d",min,sec));
        if(sentenceText!=null) sentenceText.setText(total+" saniye sonra Tablet uyku moduna girer.");
    }

    private void timeoutLock(){
        h.removeCallbacks(ticker);
        deadline=0L;
        ensureOverlay();
        if(timeText!=null)timeText.setText("00:00");
        if(sentenceText!=null)sentenceText.setText("Süre doldu. Tablet uyku moduna giriyor.");

        DevicePolicyManager dpm=(DevicePolicyManager)getSystemService(DEVICE_POLICY_SERVICE);
        ComponentName c=new ComponentName(this,AdminReceiver.class);
        if(dpm!=null && dpm.isAdminActive(c)){
            try{ dpm.lockNow(); updateNotification("Süre doldu — ekran kilitlendi"); }
            catch(Exception e){ updateNotification("Ekran kilidi uygulanamadı"); }
        }else{
            updateNotification("Kilit izni kapalı — uygulamadan Cihaz Yöneticisi iznini aç");
        }
    }

    private void ensureOverlay(){
        if(overlay!=null)return;
        if(Build.VERSION.SDK_INT>=23 && !Settings.canDrawOverlays(this)){
            updateNotification("Ekran üstü izni gerekli — uygulamayı açıp izin ver");
            return;
        }

        FrameLayout frame=new FrameLayout(this);

        ImageView bg=new ImageView(this);
        bg.setImageResource(R.drawable.bg_baby);
        bg.setScaleType(ImageView.ScaleType.CENTER_CROP);
        frame.addView(bg,new FrameLayout.LayoutParams(-1,-1));

        View dark=new View(this);
        dark.setBackgroundColor(Color.argb(155,0,0,0));
        frame.addView(dark,new FrameLayout.LayoutParams(-1,-1));

        LinearLayout center=new LinearLayout(this);
        center.setOrientation(LinearLayout.VERTICAL);
        center.setGravity(Gravity.CENTER);
        center.setPadding(dp(24),dp(10),dp(24),dp(10));
        frame.addView(center,new FrameLayout.LayoutParams(-1,-1));

        GifView gif=new GifView(this,R.drawable.warning_gif);
        center.addView(gif,new LinearLayout.LayoutParams(dp(200),dp(200)));

        TextView title=new TextView(this);
        title.setText("ŞARJ FİŞİ ÇIKARILDI");
        title.setTextColor(Color.rgb(255,82,82));
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextSize(25);
        title.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(-1,-2);
        tp.topMargin=dp(12); center.addView(title,tp);

        timeText=new TextView(this);
        timeText.setText("01:00");
        timeText.setTextColor(Color.WHITE);
        timeText.setTypeface(Typeface.MONOSPACE,Typeface.BOLD);
        timeText.setTextSize(58);
        timeText.setGravity(Gravity.CENTER);
        timeText.setClickable(false);
        timeText.setLongClickable(false);
        timeText.setFocusable(false);
        LinearLayout.LayoutParams ttp=new LinearLayout.LayoutParams(-1,-2);
        ttp.topMargin=dp(2); center.addView(timeText,ttp);

        sentenceText=new TextView(this);
        sentenceText.setText("60 saniye sonra Tablet uyku moduna girer.");
        sentenceText.setTextColor(Color.WHITE);
        sentenceText.setTypeface(Typeface.DEFAULT_BOLD);
        sentenceText.setTextSize(19);
        sentenceText.setGravity(Gravity.CENTER);
        center.addView(sentenceText,new LinearLayout.LayoutParams(-1,-2));

        int type=Build.VERSION.SDK_INT>=26
            ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            : WindowManager.LayoutParams.TYPE_PHONE;

        WindowManager.LayoutParams p=new WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT);
        p.gravity=Gravity.TOP|Gravity.START;
        p.screenOrientation=ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE;

        try{ wm.addView(frame,p); overlay=frame; }
        catch(Exception e){
            overlay=null; timeText=null; sentenceText=null;
            updateNotification("Uyarı ekranı açılamadı — overlay iznini kontrol et");
        }
    }

    private void hide(){
        if(overlay!=null){
            try{wm.removeView(overlay);}catch(Exception ignored){}
            overlay=null; timeText=null; sentenceText=null;
        }
    }

    private void channel(){
        if(Build.VERSION.SDK_INT>=26){
            NotificationChannel c=new NotificationChannel(CHANNEL,"Şarj Kilidi",NotificationManager.IMPORTANCE_LOW);
            c.setDescription("Şarj fişi koruma servisi");
            ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(c);
        }
    }

    private Notification notification(String text){
        Intent open=new Intent(this,MainActivity.class);
        int flags=PendingIntent.FLAG_UPDATE_CURRENT;
        if(Build.VERSION.SDK_INT>=23)flags|=PendingIntent.FLAG_IMMUTABLE;
        PendingIntent pi=PendingIntent.getActivity(this,0,open,flags);
        Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,CHANNEL):new Notification.Builder(this);
        return b.setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setContentTitle("Şarj Kilidi")
            .setContentText(text)
            .setContentIntent(pi)
            .setOngoing(true)
            .setShowWhen(false)
            .build();
    }

    private void updateNotification(String t){
        ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).notify(NID,notification(t));
    }

    private int dp(int v){ return (int)(v*getResources().getDisplayMetrics().density+0.5f); }
}

