package com.pulse.fitness.ui;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.os.Build;

/** Permission handling belongs to the UI; repositories and ViewModels retain no Activity. */
public abstract class BackendActivity extends Activity {
    private static final int LOCAL_NETWORK_REQUEST = 7307;
    private Runnable pendingBackendAction;

    /** The configured gateway may be on this computer or the user's local network. */
    public final void runWithBackendAccess(Runnable action) {
        if (Build.VERSION.SDK_INT < 37
                || checkSelfPermission(Manifest.permission.ACCESS_LOCAL_NETWORK)
                        == PackageManager.PERMISSION_GRANTED) {
            action.run();
            return;
        }
        if (pendingBackendAction != null) return;
        pendingBackendAction = action;
        requestPermissions(
                new String[] {Manifest.permission.ACCESS_LOCAL_NETWORK}, LOCAL_NETWORK_REQUEST);
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != LOCAL_NETWORK_REQUEST) return;
        Runnable action = pendingBackendAction;
        pendingBackendAction = null;
        if (isFinishing() || isDestroyed()) return;
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (action != null) action.run();
            else Ui.toast(this, "权限已允许，请重新点击刚才的操作");
        } else {
            new AlertDialog.Builder(this)
                    .setTitle("尚未允许访问本地服务")
                    .setMessage(
                            "连接电脑上的 AI 或登录服务，需要允许访问本地网络（附近的设备）。请重新操作并允许权限；若系统不再询问，可到系统设置 → 应用 → 脉动 PULSE → 权限 → 附近的设备中允许。本地计划和游客体验仍可使用。")
                    .setPositiveButton("知道了", null)
                    .show();
        }
    }

    @Override
    protected void onDestroy() {
        pendingBackendAction = null;
        super.onDestroy();
    }
}
