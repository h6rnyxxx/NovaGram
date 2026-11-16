package org.telegram.novagram.updater;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.AlertDialog;

import java.io.File;

public class NovaUpdateDialog {

    public static void show(Context context, NovaUpdateController.ReleaseInfo info) {
        if (context == null || info == null) {
            return;
        }
        NovaUpdateController controller = NovaUpdateController.getInstance();
        File downloaded = controller.getDownloadedFile();

        StringBuilder msg = new StringBuilder();
        msg.append(LocaleController.formatString(R.string.NovaUpdateVersionLine, info.version));
        if (info.apkSize > 0) {
            msg.append("\n").append(LocaleController.formatString(R.string.NovaUpdateSizeLine, AndroidUtilities.formatFileSize(info.apkSize)));
        }
        if (!TextUtils.isEmpty(info.changelog)) {
            msg.append("\n\n").append(info.changelog.trim());
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(TextUtils.isEmpty(info.title)
                ? LocaleController.getString(R.string.NovaUpdateTitle)
                : info.title);
        builder.setMessage(msg.toString());

        if (downloaded != null) {
            builder.setPositiveButton(LocaleController.getString(R.string.AppUpdateNow), (d, w) -> {
                if (context instanceof Activity) {
                    controller.installDownloadedApk((Activity) context, downloaded);
                }
            });
        } else if (controller.isDownloading()) {
            builder.setPositiveButton(LocaleController.getString(R.string.Cancel), (d, w) -> controller.cancelDownloading());
        } else {
            builder.setPositiveButton(LocaleController.getString(R.string.NovaUpdateDownload), (d, w) -> controller.downloadUpdate());
        }

        if (!TextUtils.isEmpty(info.releaseUrl)) {
            builder.setNeutralButton("GitHub", (d, w) -> {
                try {
                    Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(info.releaseUrl));
                    context.startActivity(i);
                } catch (Throwable ignored) {
                }
            });
        }
        builder.setNegativeButton(LocaleController.getString(R.string.Later), null);
        builder.show();
    }
}
