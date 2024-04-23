package com.example.vehiclespeeddetection;

import static com.google.android.gms.common.moduleinstall.ModuleInstallStatusUpdate.InstallState.STATE_CANCELED;
import static com.google.android.gms.common.moduleinstall.ModuleInstallStatusUpdate.InstallState.STATE_COMPLETED;
import static com.google.android.gms.common.moduleinstall.ModuleInstallStatusUpdate.InstallState.STATE_FAILED;

import android.widget.ProgressBar;

import com.google.android.gms.common.moduleinstall.InstallStatusListener;
import com.google.android.gms.common.moduleinstall.ModuleInstallClient;
import com.google.android.gms.common.moduleinstall.ModuleInstallStatusUpdate;

public class ModuleInstallProgressListener implements InstallStatusListener {
    private final ProgressBar progressBar;
    private final ModuleInstallClient moduleInstallClient;

    public ModuleInstallProgressListener(ProgressBar progressBar, ModuleInstallClient moduleInstallClient) {
        this.progressBar = progressBar;
        this.moduleInstallClient = moduleInstallClient;
    }

    @Override
    public void onInstallStatusUpdated(ModuleInstallStatusUpdate update) {
        ModuleInstallStatusUpdate.ProgressInfo progressInfo = update.getProgressInfo();
        if (progressInfo != null) {
            int progress = (int) (progressInfo.getBytesDownloaded() * 100 / progressInfo.getTotalBytesToDownload());
            progressBar.setProgress(progress);
        }

        if (isTerminateState(update.getInstallState())) {
            // Unregister listener when installation is completed, failed, or canceled
            moduleInstallClient.unregisterListener(this);
        }
    }

    private boolean isTerminateState(@ModuleInstallStatusUpdate.InstallState int state) {
        return state == STATE_CANCELED || state == STATE_COMPLETED || state == STATE_FAILED;
    }
}
