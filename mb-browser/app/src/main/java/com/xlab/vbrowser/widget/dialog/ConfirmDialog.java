package com.xlab.vbrowser.widget.dialog;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import com.xlab.vbrowser.R;
import com.xlab.vbrowser.events.IConfirmDialogResult;

public class ConfirmDialog extends DialogFragment {
    private static final String MESSAGE = "message";
    private static final String TITLE = "title";
    private static final String ACTION_OK = "actionOk";
    private static final String ACTION_CANCEL = "actionCancel";

    public IConfirmDialogResult confirmDialogResult;

    public static ConfirmDialog newInstance(final String title, final String message, final String actionOk, final String actionCancel
            , IConfirmDialogResult confirmDialogResult) {
        ConfirmDialog frag = new ConfirmDialog();
        final Bundle args = new Bundle();
        args.putString(MESSAGE, message);
        args.putString(TITLE, title);
        args.putString(ACTION_OK, actionOk);
        args.putString(ACTION_CANCEL, actionCancel);

        frag.confirmDialogResult = confirmDialogResult;

        frag.setArguments(args);
        return frag;
    }

    @NonNull
    @Override
    public AlertDialog onCreateDialog(Bundle bundle) {
        final String title = getArguments().getString(TITLE);
        final String message = getArguments().getString(MESSAGE);
        final String actionOk = getArguments().getString(ACTION_OK);
        final String actionCancel = getArguments().getString(ACTION_CANCEL);

        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.DialogStyle);
        builder.setIcon(R.drawable.ic_warning);
        builder.setCancelable(true);
        builder.setMessage(message);
        builder.setTitle(title);

        final LayoutInflater inflater = getActivity().getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.confirm_dialog, null);
        builder.setView(dialogView);

        final Button cancelButton = dialogView.findViewById(R.id.prompt_dialog_cancel);
        final Button okButton = dialogView.findViewById(R.id.prompt_dialog_ok);
        okButton.setText(actionOk);
        cancelButton.setText(actionCancel);

        cancelButton.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmDialogResult.onCancel();
                dismiss();
            }});

        okButton.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmDialogResult.onOk();
                dismiss();
            }});

        return builder.create();
    }
}
