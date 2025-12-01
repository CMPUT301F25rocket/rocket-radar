package com.rocket.radar.qr;

import android.graphics.Bitmap;
import android.graphics.Color;
import com.rocket.radar.uml.UmlNavigate;
import io.nayuki.qrcodegen.QrCode;

/**
 * A utility class for generating QR codes.
 * This class provides a static method to create a QR code {@link Bitmap} image from a given
 * event ID. It formats the event ID into a URL that can be scanned to check in or view event details.
 */
public class QRGenerator {
    @UmlNavigate(selfCard = "1", label = "generates", otherCard = "1")
    private Bitmap bitmap;

    /**
     * Formats an event ID into a shareable URL for QR code generation.
     * @param eventId The unique identifier of the event.
     * @return A complete URL string that includes the event ID as a query parameter.
     */
    public static String formatLink(String eventId) {
        // TRIM probably unneded but I'm going crazy
        return "https://radar-65b66.web.app/?eventId=" + eventId.trim();
    }

    /**
     * Generates a QR code image for a specified event.
     * The method takes an event ID, constructs a URL, and encodes it into a QR code.
     * The resulting image is a black and transparent bitmap, suitable for overlaying on different backgrounds.
     *
     * @param content The text to be encoded.
     * @return A {@link Bitmap} object representing the generated QR code.
     */
    public static Bitmap generate(String content) {
        QrCode code = QrCode.encodeText(content, QrCode.Ecc.MEDIUM);

        // Copy the code into a bitmap.
        Bitmap image = Bitmap.createBitmap(code.size, code.size, Bitmap.Config.ALPHA_8);
        for (int x = 0; x < code.size; ++x)
            for (int y = 0; y < code.size; ++y)
                image.setPixel(x, y, code.getModule(x, y) ? Color.BLACK : Color.TRANSPARENT);

        return image;
    }
}
