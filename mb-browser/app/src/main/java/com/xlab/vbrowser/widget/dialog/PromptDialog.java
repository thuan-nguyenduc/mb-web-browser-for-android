package com.xlab.vbrowser.widget.dialog;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

import com.xlab.vbrowser.R;
import com.xlab.vbrowser.events.IPromptDialogResult;

public class PromptDialog extends DialogFragment {
    public static final String FRAGMENT_TAG = "prompt-dialog";
    private static final String TITLE = "title";
    private static final String ACTION_OK = "actionOk";
    private static final String ACTION_CANCEL = "actionCancel";
    private static final String TEXT_HINT = "textHint";

    public IPromptDialogResult promptDialogResult;

    public static PromptDialog newInstance(final String textHint, final String title, final String actionOk, final String actionCancel
            , IPromptDialogResult promptDialogResult) {
        PromptDialog frag = new PromptDialog();
        final Bundle args = new Bundle();
        args.putString(TITLE, title);
        args.putString(ACTION_OK, actionOk);
        args.putString(ACTION_CANCEL, actionCancel);
        args.putString(TEXT_HINT, textHint);

        frag.promptDialogResult = promptDialogResult;

        frag.setArguments(args);
        return frag;
    }

    @NonNull
    @Override
    public AlertDialog onCreateDialog(Bundle bundle) {
        final String title = getArguments().getString(TITLE);
        final String actionOk = getArguments().getString(ACTION_OK);
        final String actionCancel = getArguments().getString(ACTION_CANCEL);
        final String textHint = getArguments().getString(TEXT_HINT);

        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.DialogStyle);
        builder.setCancelable(true);
        builder.setTitle(title);

        final LayoutInflater inflater = getActivity().getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.prompt_dialog, null);
        builder.setView(dialogView);

        final Button cancelButton = dialogView.findViewById(R.id.prompt_dialog_cancel);
        final Button okButton = dialogView.findViewById(R.id.prompt_dialog_ok);
        okButton.setText(actionOk);
        okButton.setEnabled(false);
        okButton.setAlpha(0.5f);
        cancelButton.setText(actionCancel);

        final EditText editableTitle = dialogView.findViewById(R.id.edit_title);
        editableTitle.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                boolean isEmptyText = TextUtils.isEmpty(editable.toString().trim());
                okButton.setEnabled(!isEmptyText);
                okButton.setAlpha(isEmptyText ? 0.5f : 1f);
            }
        });

        editableTitle.setHint(textHint);

        cancelButton.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                promptDialogResult.onCancel();
                dismiss();
            }});

        okButton.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                promptDialogResult.onOk(editableTitle.getText().toString());
                dismiss();
            }});

        return builder.create();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        final Dialog dialog = getDialog();
        if (dialog != null) {
            final Window window = dialog.getWindow();
            if (window != null) {
                window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
            }
        }
    }
}
