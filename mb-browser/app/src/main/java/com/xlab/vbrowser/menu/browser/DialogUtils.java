package com.xlab.vbrowser.menu.browser;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.widget.SeekBar;

import com.xlab.vbrowser.R;
import com.xlab.vbrowser.prefs.Constants;
import com.xlab.vbrowser.utils.Settings;

public class DialogUtils {
    public static void showNightmodeConfigDialog(final Context context, final boolean isCancelable) {
        final Settings settings = Settings.getInstance(context);
        final AlertDialog.Builder popDialog = new AlertDialog.Builder(context, R.style.DialogStyle);

        final SeekBar seek = new SeekBar(context);
        seek.setMax(Constants.NIGHTMODE_BRIGHTNESS_MAX);
        seek.setProgress(settings.getNightModeBrighness());
        seek.setPadding(30, 20, 30, 0);

        final String title = context.getResources().getString(R.string.change_browser_brightness);
        popDialog.setTitle(title);
        popDialog.setView(seek);
        popDialog.setCancelable(isCancelable);

        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser){
                settings.setNightModeBrightness(progress);
            }

            public void onStartTrackingTouch(SeekBar arg0) {
                // TODO Auto-generated method stub

            }

            public void onStopTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub

            }
        });


        // Button OK
        popDialog.setPositiveButton(context.getResources().getString(R.string.action_ok),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }

                });


        final AlertDialog dialog = popDialog.create();
        dialog.getWindow().setDimAmount(0);

        dialog.setOnShowListener( new DialogInterface.OnShowListener() {
             @Override
             public void onShow(DialogInterface arg0) {
                 dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(context, R.color.darkPositiveButtonAccent));
             }
        });

        dialog.show();
    }
}
