package org.telegram.novagram.updater;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;

import java.io.File;

/**
 * In-app OTA update dialog with live streaming progress bar (no browser redirect required).
 */
public class NovaUpdateDialog {

    public static void show(Context context, NovaUpdateController.ReleaseInfo info) {
        if (context == null || info == null) {
            return;
        }
        NovaUpdateController controller = NovaUpdateController.getInstance();
        File downloaded = controller.getDownloadedFile();

        StringBuilder baseMsg = new StringBuilder();
        baseMsg.append(LocaleController.formatString(R.string.NovaUpdateVersionLine, info.version));
        if (info.apkSize > 0) {
            baseMsg.append("\n").append(LocaleController.formatString(R.string.NovaUpdateSizeLine, AndroidUtilities.formatFileSize(info.apkSize)));
        }
        if (!TextUtils.isEmpty(info.changelog)) {
            baseMsg.append("\n\n").append(info.changelog.trim());
        }

        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dp(24), dp(4), dp(24), dp(4));

        TextView msgView = new TextView(context);
        msgView.setTextSize(15);
        msgView.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        msgView.setText(baseMsg.toString());
        container.addView(msgView);

        TextView statusView = new TextView(context);
        statusView.setTextSize(13);
        statusView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText));
        statusView.setPadding(0, dp(12), 0, dp(6));
        container.addView(statusView);

        ProgressBar progressBar = new ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(1000);
        container.addView(progressBar, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        Runnable updateStatusUi = () -> {
            File ready = controller.getDownloadedFile();
            if (ready != null) {
                statusView.setVisibility(android.view.View.VISIBLE);
                statusView.setText(LocaleController.getString(R.string.NovaUpdateReadyInstall));
                progressBar.setVisibility(android.view.View.VISIBLE);
                progressBar.setProgress(1000);
            } else if (controller.isDownloading()) {
                statusView.setVisibility(android.view.View.VISIBLE);
                progressBar.setVisibility(android.view.View.VISIBLE);
                float p = controller.getDownloadingProgress();
                long dl = controller.getDownloadedBytes();
                long tot = controller.getTotalBytes();
                int pct = (int) (p * 100f);
                progressBar.setProgress((int) (p * 1000f));
                if (tot > 0) {
                    statusView.setText(LocaleController.formatString(
                            R.string.NovaUpdateDownloadingProgress,
                            pct,
                            AndroidUtilities.formatFileSize(dl),
                            AndroidUtilities.formatFileSize(tot)
                    ));
                } else {
                    statusView.setText(LocaleController.formatString(R.string.AppUpdateDownloading, pct));
                }
            } else {
                statusView.setVisibility(android.view.View.GONE);
                progressBar.setVisibility(android.view.View.GONE);
            }
        };
        updateStatusUi.run();

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(TextUtils.isEmpty(info.title)
                ? LocaleController.getString(R.string.NovaUpdateTitle)
                : info.title);
        builder.setView(container);

        final AlertDialog[] dialogRef = new AlertDialog[1];
        final NovaUpdateController.DownloadProgressListener listener = (p, dl, tot, finished, error) -> {
            updateStatusUi.run();
            if (finished && error == null && dialogRef[0] != null && dialogRef[0].isShowing()) {
                try {
                    dialogRef[0].dismiss();
                } catch (Throwable ignored) {
                }
            }
        };

        if (downloaded != null) {
            builder.setPositiveButton(LocaleController.getString(R.string.AppUpdateNow), (d, w) -> {
                if (context instanceof Activity) {
                    controller.installDownloadedApk((Activity) context, downloaded);
                }
            });
        } else if (controller.isDownloading()) {
            builder.setPositiveButton(LocaleController.getString(R.string.Cancel), null);
        } else {
            builder.setPositiveButton(LocaleController.getString(R.string.NovaUpdateDownload), null);
        }

        if (!TextUtils.isEmpty(info.releaseUrl)) {
            builder.setNeutralButton("GitHub", (d, w) -> {
                try {
                    context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(info.releaseUrl)));
                } catch (Throwable ignored) {
                }
            });
        }
        builder.setNegativeButton(LocaleController.getString(R.string.NovaUpdateLater), null);

        AlertDialog dialog = builder.create();
        dialogRef[0] = dialog;
        controller.addProgressListener(listener);
        dialog.setOnDismissListener(d -> controller.removeProgressListener(listener));
        dialog.show();

        // Keep dialog open while streaming cloud update so user sees live progress right inside Telegram
        if (downloaded == null && dialog.getButton(AlertDialog.BUTTON_POSITIVE) != null) {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                if (controller.getDownloadedFile() != null && context instanceof Activity) {
                    controller.installDownloadedApk((Activity) context, controller.getDownloadedFile());
                } else if (controller.isDownloading()) {
                    controller.cancelDownloading();
                    updateStatusUi.run();
                    ((TextView) dialog.getButton(AlertDialog.BUTTON_POSITIVE)).setText(LocaleController.getString(R.string.NovaUpdateDownload));
                } else {
                    controller.downloadUpdate();
                    updateStatusUi.run();
                    ((TextView) dialog.getButton(AlertDialog.BUTTON_POSITIVE)).setText(LocaleController.getString(R.string.Cancel));
                }
            });
        }
    }
}
