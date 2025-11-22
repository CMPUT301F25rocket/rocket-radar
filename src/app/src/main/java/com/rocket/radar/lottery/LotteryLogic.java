package com.rocket.radar.lottery;

import android.util.Log;

import com.rocket.radar.events.Event;
import com.rocket.radar.events.EventRepository;
import com.rocket.radar.notifications.NotificationRepository;
import com.rocket.radar.profile.ProfileRepository;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles the core logic for running an event lottery.
 * <p>
 * This class is responsible for:
 * <ul>
 *     <li>Fetching the current waitlist for a specific event.</li>
 *     <li>Selecting a random subset of users based on event capacity or a specified sample number.</li>
 *     <li>Moving selected users from the waitlist to the invited list in the database.</li>
 *     <li>Sending notifications to both the winners (invited) and losers (remaining waitlisted).</li>
 * </ul>
 */
public class LotteryLogic {

    private final Event event;
    String TAG = "LotteryLogic";
    EventRepository eventRepository = new EventRepository();
    NotificationRepository notificationRepository = new NotificationRepository();
    private ProfileRepository profileRepository = new ProfileRepository();

    /**
     * Constructs a new LotteryLogic instance for a specific event.
     *
     * @param event The event for which the lottery will be run.
     */
    public LotteryLogic(Event event) {
        this.event = event;
    }

    /**
     * Runs the lottery using the default event capacity.
     * <p>
     * This is a convenience wrapper that calls {@link #handleRunLottery(Event, Integer)}
     * with a null sample number, indicating that the lottery should attempt to fill
     * all available spots up to the event's capacity.
     *
     * @param event The event object (redundant if passed in constructor, but kept for API consistency).
     */
    public void handleRunLottery(Event event) {
        handleRunLottery(event, null);
    }

    /**
     * Executes the lottery logic to select random winners from the waitlist.
     * <p>
     * This method performs the following steps asynchronously:
     * <ol>
     *     <li>Fetches the current waitlist entrants from the repository.</li>
     *     <li>Determines the number of users to invite based on the {@code sampleNum} or the event capacity.</li>
     *     <li>If the waitlist is smaller than the capacity, everyone is invited.</li>
     *     <li>If the waitlist exceeds capacity, random entrants are selected.</li>
     *     <li>Updates the database to move users to the "invited" list and remove them from the "waitlist".</li>
     *     <li>Sends push notifications to both the winners and the remaining waitlisted users.</li>
     * </ol>
     *
     * @param event     The event for which the lottery is being run.
     * @param sampleNum The specific number of users to select. If {@code null}, the method defaults
     *                  to filling the event's defined capacity.
     */
    public void handleRunLottery(Event event, @Nullable Integer sampleNum) {
        // samples a subset of Waitlisted users (which is the size of event capacity)
        // and adds them to invitedUsers by calling event repository
        Log.d(TAG, "Running Lottery!");

        ArrayList<String> waitlistedUsers = new ArrayList<>();


        eventRepository.getWaitlistSize(this.event, new EventRepository.WaitlistSizeListener() {
            @Override
            public void onSizeReceived(int size) {
                Log.d(TAG, "Waitlist size received: " + size);
            }

            @Override
            public void onWaitlistEntrantsFetched(List<String> userIds) {
                Log.d(TAG, "Waitlist entrants fetched: " + userIds);
                waitlistedUsers.addAll(userIds);
                ArrayList<String> invitedUsers = new ArrayList<>();

                if (sampleNum != null && sampleNum > waitlistedUsers.size()){
                    for (int i = 0; i < sampleNum; i++) {
                        int randomIndex = (int) (Math.random() * waitlistedUsers.size());
                        String chosenUserId = waitlistedUsers.get(randomIndex);
                        Log.d(TAG, "Chosen user ID: " + chosenUserId);

                        moveUserToInvited(chosenUserId, invitedUsers);
                        waitlistedUsers.remove(randomIndex);
                    }
                } else if (waitlistedUsers.size() < LotteryLogic.this.event.getEventCapacity()) {
                    // add everyone on waitlist to invited
                    Log.d(TAG, "Waitlist size is less than event capacity, Everyone is invited!");
                    for (String userId : waitlistedUsers) {
                        moveUserToInvited(userId, invitedUsers);
                    }
                } else {
                    Log.d(TAG, "Waitlist size is greater than event capacity, Lottery started!");
                    int numInvited = LotteryLogic.this.event.getEventCapacity();
                    for (int i = 0; i < numInvited; i++) {
                        int randomIndex = (int) (Math.random() * waitlistedUsers.size());
                        String chosenUserId = waitlistedUsers.get(randomIndex);
                        Log.d(TAG, "Chosen user ID: " + chosenUserId);

                        moveUserToInvited(chosenUserId, invitedUsers);
                        waitlistedUsers.remove(randomIndex);
                    }
                }

                eventRepository.setInvitedUserIds(LotteryLogic.this.event, invitedUsers);
                Log.d(TAG, "Added invited users" + invitedUsers + "to event " + LotteryLogic.this.event.getEventTitle() + "!");

                // send notification to users that won the lottery
                String title = LotteryLogic.this.event.getEventTitle();
                String body = "You won the lottery!";
                String eventId = LotteryLogic.this.event.getEventId();
                String groupCollection = "invitedUsers";
                notificationRepository.sendNotificationToGroup(title, body, eventId, groupCollection);
                Log.d(TAG, "WIN Notification sent to invited users" + invitedUsers + "to event " + LotteryLogic.this.event.getEventTitle() + "!");

                // send notification to users that lost the lottery
                String body2 = "You lost the lottery!";
                String groupCollection2 = "waitlistedUsers";
                notificationRepository.sendNotificationToGroup(title, body2, eventId, groupCollection2);
                Log.d(TAG, "LOST Notification sent to waitlisted users" + waitlistedUsers + "to event " + LotteryLogic.this.event.getEventTitle() + "!");
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error getting waitlist data.", e);
            }
        });
    }


    /**
     * Moves a user from the waitlist to the invited list.
     * <p>
     * This helper method updates the local list of invited users and triggers
     * database updates to remove the user from the waitlist and update their
     * individual profile status.
     *
     * @param userId       The ID of the user to move.
     * @param invitedUsers The list of invited user IDs to which the user will be added.
     */
    private void moveUserToInvited(String userId, List<String> invitedUsers) {
        invitedUsers.add(userId);
        eventRepository.removeUserFromWaitlist(event, userId);
        profileRepository.updateUserInvitedList(userId, event.getEventId());
    }
}
