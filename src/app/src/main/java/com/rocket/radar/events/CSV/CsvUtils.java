package com.rocket.radar.events.CSV;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.FileWriter;
import java.util.List;

public class CsvUtils {

    public static void exportToCsv(Context context, List<String> entrantNames, String filename) {
        try {
            // 1. Create the file in the app's cache directory
            File file = new File(context.getCacheDir(), filename + ".csv");
            FileWriter writer = new FileWriter(file);

            // 2. Add Header
            writer.append("Entrant Name\n");

            // 3. Add Data
            for (String name : entrantNames) {
                writer.append(name).append("\n");
            }
            writer.flush();
            writer.close();

            // 4. Share the file
            shareFile(context, file);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void shareFile(Context context, File file) {
        // Get URI using FileProvider (requires setup in AndroidManifest)
        Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".provider", file);

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/csv");
        intent.putExtra(Intent.EXTRA_SUBJECT, "Entrant List Export");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        context.startActivity(Intent.createChooser(intent, "Export CSV using..."));
    }
}
