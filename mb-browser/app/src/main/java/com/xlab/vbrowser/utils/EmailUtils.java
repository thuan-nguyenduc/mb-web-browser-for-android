package com.xlab.vbrowser.utils;

import android.content.Context;
import android.content.Intent;

/**
 * Created by nguyenducthuan on 2/7/18.
 */

public class EmailUtils {
    public static void sendEmail(Context context, String [] to, String subject, String message, String chooserMsg) {
        Intent email = new Intent(Intent.ACTION_SEND);
        email.putExtra(Intent.EXTRA_EMAIL, to);
        email.putExtra(Intent.EXTRA_SUBJECT, subject);
        email.putExtra(Intent.EXTRA_TEXT, message);

        //need this to prompts email client only
        email.setType("message/rfc822");

        context.startActivity(Intent.createChooser(email, chooserMsg));
    }
}
