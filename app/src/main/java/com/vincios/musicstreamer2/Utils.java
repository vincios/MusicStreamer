package com.vincios.musicstreamer2;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;

public class Utils {

    public static final class CONSTANTS{
        public static final int CURRENT_SDK_VERSION = Build.VERSION.SDK_INT;
        public static final String BUFFERED_POSITION = "buffered_position";
        public static String ACTION_UPDATE_BUFFERED_POSITION = "com.vincios.musicstreamer2.action.UPDATE_BUFFERED_POSITION";
        public static String ACTION_SEARCH = "com.vincios.musicstreamer2.action.SEARCH";
    }

    //private static final float BLUR_RADIUS = 25f;

    public static Bitmap blur(Context ctx, Bitmap image, float radius) {
        if (null == image) return null;

        Bitmap outputBitmap = Bitmap.createBitmap(image);
        final RenderScript renderScript = RenderScript.create(ctx);
        Allocation tmpIn = Allocation.createFromBitmap(renderScript, image);
        Allocation tmpOut = Allocation.createFromBitmap(renderScript, outputBitmap);

        //Intrinsic Gausian blur filter
        ScriptIntrinsicBlur theIntrinsic = ScriptIntrinsicBlur.create(renderScript, Element.U8_4(renderScript));
        theIntrinsic.setRadius(radius);
        theIntrinsic.setInput(tmpIn);
        theIntrinsic.forEach(tmpOut);
        tmpOut.copyTo(outputBitmap);
        return outputBitmap;
    }


}
