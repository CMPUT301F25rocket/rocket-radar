package com.rocket.radar.events;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.work.Worker;import androidx.work.WorkerParameters;

import com.google.android.gms.common.api.Result;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.concurrent.ExecutionException;

/**
 * A background worker managed by WorkManager to run the event lottery.
 * This worker is designed to be scheduled to run at a specific time.
 */
public class LotteryWorker extends Worker {

    public static final String KEY_EVENT_ID = "EVENT_ID";
    private static final String TAG = "LotteryWorker";

    public LotteryWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        // Retrieve the event ID passed to the worker
        String eventId = getInputData().getString(KEY_EVENT_ID);
        if (eventId == null || eventId.isEmpty()) {
            Log.e(TAG, "Work failed: Event ID was null or empty.");
            return Result.failure();
        }

        Log.d(TAG, "Starting lottery work for event: " + eventId);

        // This logic runs on a background thread provided by WorkManager.
        // We need to fetch the Event object from Firestore synchronously.
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        try {
            // Block and wait for the Firestore fetch to complete.
            DocumentSnapshot eventDoc = Tasks.await(db.collection("events").document(eventId).get());
            if (!eventDoc.exists()) {
                Log.e(TAG, "Work failed: Event document " + eventId + " does not exist.");
                // If the event was deleted, no need to retry.
                return Result.success();
            }

            Event event = eventDoc.toObject(Event.class);
            if (event == null) {
                Log.e(TAG, "Work failed: Could not deserialize event object.");
                return Result.failure();
            }

            // Use the LotteryManager to run the actual lottery logic
            LotteryManager lotteryManager = new LotteryManager(getApplicationContext());

            // Since LotteryManager is asynchronous, we need a way to wait for its result.
            // A simple (but not ideal) way is to use a manual lock.
            final Object lock = new Object();
            final boolean[] successState = {false};

            lotteryManager.runLottery(event, (success, message) -> {
                Log.d(TAG, "Lottery completion message for event " + eventId + ": " + message);
                successState[0] = success;
                synchronized (lock) {
                    lock.notify(); // Notify the waiting main thread of the worker
                }
            });

            // Wait for the lottery to complete
            synchronized (lock) {
                lock.wait(30000); // Wait for a maximum of 30 seconds
            }

            if (successState[0]) {
                Log.d(TAG, "Work successful for event: " + eventId);
                return Result.success();
            } else {
                Log.e(TAG, "Work failed for event: " + eventId + ". See LotteryManager logs.");
                // Retry might be appropriate if it was a network error.
                return Result.retry();
            }

        } catch (ExecutionException | InterruptedException e) {
            Log.e(TAG, "Error while running lottery work for event " + eventId, e);
            Thread.currentThread().interrupt(); // Restore the interrupted status
            // The task was interrupted or failed. Retry.
            return Result.retry();
        }
    }
}
