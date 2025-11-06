package com.rocket.radar.events;

import android.content.Context;
import android.widget.Toast;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Handles the logic for running an event lottery on the client-side.
 * This class encapsulates fetching data, selecting winners, and updating Firestore.
 */
public class LotteryManager {

    private final FirebaseFirestore db;
    private final Context context;

    public interface LotteryListener {
        void onLotteryComplete(boolean success, String message);
    }

    public LotteryManager(Context context) {
        this.db = FirebaseFirestore.getInstance();
        this.context = context;
    }

    /**
     * Executes the lottery for a given event.
     * @param event The event to run the lottery for.
     * @param listener A callback to notify the caller of the result.
     */
    public void runLottery(Event event, LotteryListener listener) {
        if (event == null || event.getEventId() == null) {
            listener.onLotteryComplete(false, "Error: Event data not available.");
            return;
        }

        String eventId = event.getEventId();
        int numberOfWinners = event.getNumberOfWinners();

        if (numberOfWinners <= 0) {
            listener.onLotteryComplete(false, "Lottery not run. Number of winners must be greater than 0.");
            return;
        }

        // Get all users on the waitlist for this event
        db.collection("events").document(eventId).collection("checkins")
                .whereEqualTo("status", "waitlist")
                .get()
                .addOnSuccessListener(waitlistSnapshot -> {
                    if (waitlistSnapshot.isEmpty()) {
                        listener.onLotteryComplete(true, "No users on the waitlist to select.");
                        return;
                    }

                    List<QueryDocumentSnapshot> waitlistUsers = new ArrayList<>(waitlistSnapshot.getDocuments());

                    // Randomly select the winners
                    Collections.shuffle(waitlistUsers); // Randomize the list
                    List<QueryDocumentSnapshot> winners = waitlistUsers.subList(0, Math.min(numberOfWinners, waitlistUsers.size()));

                    // Update the status of winners using a batch write for atomicity
                    WriteBatch batch = db.batch();
                    for (QueryDocumentSnapshot winner : winners) {
                        batch.update(winner.getReference(), "status", "attending");
                    }

                    batch.commit().addOnSuccessListener(aVoid -> {
                        listener.onLotteryComplete(true, "Lottery successful! " + winners.size() + " winners selected.");
                    }).addOnFailureListener(e -> {
                        listener.onLotteryComplete(false, "Error updating winners: " + e.getMessage());
                    });

                }).addOnFailureListener(e -> {
                    listener.onLotteryComplete(false, "Error fetching waitlist: " + e.getMessage());
                });
    }
}
