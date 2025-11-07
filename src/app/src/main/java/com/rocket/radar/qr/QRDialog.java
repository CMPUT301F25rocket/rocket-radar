package com.rocket.radar.qr;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.rocket.radar.databinding.DialogQrcodeBinding;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;


/**
 * A {@link DialogFragment} that displays a QR code for a specific event.
 * This dialog is responsible for generating a QR code from an event ID, displaying it
 * in a material dialog, and providing options to either share the QR code image or
 * dismiss the dialog.
 */
public class QRDialog extends DialogFragment {
    public static final String TAG = QRDialog.class.getSimpleName();
    private final Bitmap bitmap;
    private final BitmapDrawable drawable;

    /**
     * Constructs a new QRDialog.
     * The constructor generates the QR code bitmap based on the provided eventId
     * and prepares it for display.
     *
     * @param context The context used to access resources.
     * @param eventId The unique identifier for the event, which will be encoded in the QR code.
     */
    public QRDialog(Context context, String eventId) {
        bitmap = QRGenerator.generate(eventId);
        drawable = new BitmapDrawable(context.getResources(), bitmap);
        drawable.getPaint().setFilterBitmap(false);
    }

    /**
     * Creates and configures the dialog box to be shown.
     * This method inflates the layout, sets the generated QR code image, and
     * configures the "Cancel" and "Share" buttons.
     *
     * @param savedInstanceState The last saved instance state of the Fragment,
     *                           or null if this is a freshly created Fragment.
     * @return A new Dialog instance to be displayed by the Fragment.
     */
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Activity activity = requireActivity();
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(activity);
        DialogQrcodeBinding binding = DialogQrcodeBinding.inflate(activity.getLayoutInflater());
        binding.qrCodeImageView.setImageDrawable(drawable);

        return builder.setView(binding.getRoot())
                .setNegativeButton("Cancel", this::buttonClick)
                .setPositiveButton("Share", this::buttonClick)
                .create();
    }

    /**
     * Handles click events for the dialog's positive and negative buttons.
     *
     * @param dialog The dialog that received the click.
     * @param which The button that was clicked (e.g., {@link DialogInterface#BUTTON_POSITIVE}).
     */
    private void buttonClick(DialogInterface dialog, int which) {
        switch (which) {
            case DialogInterface.BUTTON_POSITIVE:
                shareQrCode();
                break;

            case DialogInterface.BUTTON_NEGATIVE:
                dialog.dismiss();
                break;

            default:
                break;
        }
    }

    /**
     * Open the chooser menu on Android with the QR code attached so that the user can share the image
     * to other applications which support {@code image/png}.
     */
    private void shareQrCode() {
        Intent intent = new Intent(Intent.ACTION_SEND);
        // NOTE: Would do svg but I don't know if the default message client support it.
        // https://stackoverflow.com/a/73547282 (non doc help)
        File stored;
        try {
            stored = File.createTempFile("com.rocket.radar.qrcode",".png");
        } catch (IOException e) {
            Log.e(TAG, "Failed to create temporary file for QR code");
            return;
        }

        // REMINDER: The try resource handles the close automagically.
        try (FileOutputStream fileStream = new FileOutputStream(stored)) {
            var byteStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 50, byteStream);
            fileStream.write(byteStream.toByteArray());
            fileStream.flush();
        } catch (FileNotFoundException ignored) {
            // I mean we should have just created it. This should be unreachable.
            Log.e(TAG, "Something is very wrong");
            return;
        } catch (IOException e) {
            Log.e(TAG, e.toString());
            return;
        }

        intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(stored));
        intent.setType("image/png");
        startActivity(Intent.createChooser(intent, "Share QR code"));
    }
}
