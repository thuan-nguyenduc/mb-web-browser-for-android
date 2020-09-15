package com.xlab.vbrowser.utils;

import android.os.Bundle;
import android.os.Parcel;

public class BundleUtils {
    public static byte[] convertBundleToBytes(Bundle bundle) {
        final Parcel parcel = Parcel.obtain();
        bundle.writeToParcel(parcel, 0);
        byte[] bundleBytes = parcel.marshall();
        parcel.recycle();

        return bundleBytes;
    }

    public static Bundle convertBytesToBundle(byte[] bundleBytes) {
        final Parcel parcel = Parcel.obtain();
        parcel.unmarshall(bundleBytes, 0, bundleBytes.length);
        parcel.setDataPosition(0);
        Bundle bundle = (Bundle) parcel.readBundle();
        parcel.recycle();

        return bundle;
    }
}
