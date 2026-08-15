package com.yusuf.sarjkilidi.v5;
import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;

public class AdminReceiver extends DeviceAdminReceiver {
    @Override public CharSequence onDisableRequested(Context c, Intent i) {
        return "Bu izin kapatılırsa şarj koruması ve otomatik ekran kilidi devre dışı kalır.";
    }
}

