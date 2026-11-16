package org.telegram.messenger;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.view.ViewGroup;

import org.telegram.messenger.regular.BuildConfig;
import org.telegram.novagram.updater.NovaUpdateController;
import org.telegram.novagram.updater.NovaUpdateDialog;
import org.telegram.novagram.updater.NovaUpdateLayout;
import org.telegram.ui.Components.AlertsCreator;
import org.telegram.ui.IUpdateLayout;

import java.io.File;

public class ApplicationLoaderImpl extends ApplicationLoader {
    @Override
    protected String onGetApplicationId() {
        return BuildConfig.APPLICATION_ID;
    }

    @Override
    public boolean checkApkInstallPermissions(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !ApplicationLoader.applicationContext.getPackageManager().canRequestPackageInstalls()) {
            AlertsCreator.createApkRestrictedDialog(context, null).show();
            return false;
        }
        return true;
    }

    @Override
    public boolean isCustomUpdate() {
        return true;
    }

    @Override
    public BetaUpdate getUpdate() {
        return NovaUpdateController.getInstance().getUpdateAsBetaUpdate();
    }

    @Override
    public void checkUpdate(boolean force, Runnable whenDone) {
        NovaUpdateController.getInstance().checkForUpdates(force, whenDone);
    }

    @Override
    public void downloadUpdate() {
        NovaUpdateController.getInstance().downloadUpdate();
    }

    @Override
    public void cancelDownloadingUpdate() {
        NovaUpdateController.getInstance().cancelDownloading();
    }

    @Override
    public boolean isDownloadingUpdate() {
        return NovaUpdateController.getInstance().isDownloading();
    }

    @Override
    public float getDownloadingUpdateProgress() {
        return NovaUpdateController.getInstance().getDownloadingProgress();
    }

    @Override
    public File getDownloadedUpdateFile() {
        return NovaUpdateController.getInstance().getDownloadedFile();
    }

    @Override
    public IUpdateLayout takeUpdateLayout(Activity activity, ViewGroup sideMenuContainer) {
        return new NovaUpdateLayout(activity, sideMenuContainer);
    }

    @Override
    public boolean showCustomUpdateAppPopup(Context context, BetaUpdate update, int account) {
        NovaUpdateController.ReleaseInfo pending = NovaUpdateController.getInstance().getPendingRelease();
        if (pending != null) {
            NovaUpdateDialog.show(context, pending);
            return true;
        }
        return false;
    }
}
