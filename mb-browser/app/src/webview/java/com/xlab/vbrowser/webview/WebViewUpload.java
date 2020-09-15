package com.xlab.vbrowser.webview;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import com.xlab.vbrowser.R;
import com.xlab.vbrowser.fragment.BrowserFragment;
import com.xlab.vbrowser.utils.FileUtils;

import java.io.File;
import java.io.IOException;

import static android.app.Activity.RESULT_OK;

public class WebViewUpload {
    /*
     *Support upload file
     */
    private final static int FCR=2501;
    private static final String TAG = BrowserFragment.class.getSimpleName();
    private String mCM;
    private ValueCallback<Uri[]> mUMA;
    private ValueCallback<Uri> mUM;
    private boolean multiple_files = true;

    private Fragment fragment;

    public WebViewUpload(Fragment fragment) {
        this.fragment = fragment;
    }

    public boolean onShowFileChooser(WebView webView, final ValueCallback<Uri[]> filePathCallback, WebChromeClient.FileChooserParams fileChooserParams) {
        if (mUMA != null) {
            mUMA.onReceiveValue(null);
        }
        mUMA = filePathCallback;
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(this.fragment.getActivity().getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = FileUtils.createImageFile();
                takePictureIntent.putExtra("PhotoPath", mCM);
            } catch (IOException ex) {
                Log.e(TAG, "Image file creation failed", ex);
            }
            if (photoFile != null) {
                mCM = "file:" + photoFile.getAbsolutePath();
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
            } else {
                takePictureIntent = null;
            }
        }
        Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
        contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
        contentSelectionIntent.setType("*/*");
        if (multiple_files) {
            contentSelectionIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        }
        Intent[] intentArray;
        if (takePictureIntent != null) {
            intentArray = new Intent[]{takePictureIntent};
        } else {
            intentArray = new Intent[0];
        }

        Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
        chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
        chooserIntent.putExtra(Intent.EXTRA_TITLE, this.fragment.getActivity().getString(R.string.file_chooser));
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);
        this.fragment.startActivityForResult(chooserIntent, FCR);

        return true;
    }

    public void handleChooseFileResult(int requestCode, int resultCode, Intent intent) {
        if(Build.VERSION.SDK_INT >= 21){
            if(null == mUMA){
                return;
            }

            Uri[] results = null;
            //checking if response is positive
            if(resultCode== RESULT_OK){
                if(requestCode == FCR){
                    if(intent == null || intent.getData() == null){
                        if(mCM != null){
                            results = new Uri[]{Uri.parse(mCM)};
                        }
                    }else{
                        String dataString = intent.getDataString();
                        if(dataString != null){
                            results = new Uri[]{Uri.parse(dataString)};
                        } else {
                            if(multiple_files) {
                                if (intent.getClipData() != null) {
                                    final int numSelectedFiles = intent.getClipData().getItemCount();
                                    results = new Uri[numSelectedFiles];
                                    for (int i = 0; i < numSelectedFiles; i++) {
                                        results[i] = intent.getClipData().getItemAt(i).getUri();
                                    }
                                }
                            }
                        }
                    }
                }
            }
            mUMA.onReceiveValue(results);
            mUMA = null;
        }else{
            if(requestCode == FCR){
                if(null == mUM) return;
                Uri result = intent == null || resultCode != RESULT_OK ? null : intent.getData();
                mUM.onReceiveValue(result);
                mUM = null;
            }
        }
    }
}
