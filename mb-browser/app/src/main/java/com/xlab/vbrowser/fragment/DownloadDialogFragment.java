/*Copyright by MonnyLab*/

package com.xlab.vbrowser.fragment;

import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.text.Html;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.xlab.vbrowser.web.Download;

/**
 * Fragment displaying a download dialog
 */
public class DownloadDialogFragment extends DialogFragment {
    public static final String FRAGMENT_TAG = "should-download-prompt-dialog";

    public static DownloadDialogFragment newInstance(Download download, String fileName) {
        DownloadDialogFragment frag = new DownloadDialogFragment();
        final Bundle args = new Bundle();
        args.putString("fileName", fileName);
        args.putParcelable("download", download);
        frag.setArguments(args);
        return frag;
    }

    public AlertDialog onCreateDialog(Bundle bundle) {
        final String fileName = getArguments().getString("fileName");
        final Download pendingDownload = getArguments().getParcelable("download");

        final AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), com.xlab.vbrowser.R.style.DialogStyle);
        builder.setCancelable(true);
        builder.setTitle(getString(com.xlab.vbrowser.R.string.download_dialog_title));

        final LayoutInflater inflater = getActivity().getLayoutInflater();
        final View dialogView = inflater.inflate(com.xlab.vbrowser.R.layout.download_dialog, null);
        builder.setView(dialogView);

        final ImageView downloadDialogIcon = (ImageView) dialogView.findViewById(com.xlab.vbrowser.R.id.download_dialog_icon);
        final TextView downloadDialogMessage = (TextView) dialogView.findViewById(com.xlab.vbrowser.R.id.download_dialog_file_name);
        final Button downloadDialogCancelButton = (Button) dialogView.findViewById(com.xlab.vbrowser.R.id.download_dialog_cancel);
        final Button downloadDialogDownloadButton = (Button) dialogView.findViewById(com.xlab.vbrowser.R.id.download_dialog_download);

        downloadDialogIcon.setImageResource(com.xlab.vbrowser.R.drawable.ic_download);
        downloadDialogMessage.setText(fileName);
        downloadDialogCancelButton.setText(getString(com.xlab.vbrowser.R.string.download_dialog_action_cancel));
        downloadDialogDownloadButton.setText(getString(com.xlab.vbrowser.R.string.download_dialog_action_download));

        final AlertDialog alert = builder.create();

        setButtonOnClickListener(downloadDialogCancelButton, pendingDownload, false, fileName);
        setButtonOnClickListener(downloadDialogDownloadButton, pendingDownload, true, fileName);

        return alert;
    }

    private void setButtonOnClickListener(Button button, final Download pendingDownload, final boolean shouldDownload, final String fileName) {
        button.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        sendDownloadDialogButtonClicked(pendingDownload, shouldDownload, fileName);
                        dismiss();
                    }
                });
    }

    public void sendDownloadDialogButtonClicked(Download download, boolean shouldDownload, String fileName) {
        final DownloadDialogListener listener = (DownloadDialogListener) getTargetFragment();
        if (listener != null) {
            listener.onFinishDownloadDialog(download, shouldDownload, fileName);
        }
        dismiss();
    }

    public Spanned getSpannedTextFromHtml(int text, int replaceString) {
        if (Build.VERSION.SDK_INT >= 24) {
            return (Html.fromHtml(String
                    .format(getText(text)
                            .toString(), getString(replaceString)), Html.FROM_HTML_MODE_LEGACY));
        } else {
            return (Html.fromHtml(String
                    .format(getText(text)
                            .toString(), getString(replaceString))));
        }
    }

    public interface DownloadDialogListener {
        void onFinishDownloadDialog(Download download, boolean shouldDownload, String fileName);
    }

}
