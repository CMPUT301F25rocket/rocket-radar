package com.rocket.radar.qr;

import android.graphics.Bitmap;
import android.graphics.Color;
import io.nayuki.qrcodegen.QrCode;

public class QRGenerator {
    public static Bitmap generate(String eventId) {
        String content = "android-app://com.rocket.radar#Intent;action=com.rocket.radar.events.VIEW_EVENT;S.eventId=" + eventId + ";end";
        QrCode code = QrCode.encodeText(content, QrCode.Ecc.MEDIUM);

        // Copy the code into a bitmap
        Bitmap image = Bitmap.createBitmap(code.size, code.size, Bitmap.Config.RGB_565);
        for (int x = 0; x < code.size; ++x)
            for (int y = 0; y < code.size; ++y)
                image.setPixel(x, y, code.getModule(x, y) ? Color.BLACK : Color.WHITE);

        return image;
    }
}
