package com.xlab.vbrowser.events;

public interface IPromptDialogResult {
    void onCancel();
    void onOk(String result);
}