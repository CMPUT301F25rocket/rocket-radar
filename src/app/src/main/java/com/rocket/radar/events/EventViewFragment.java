package com.rocket.radar.events;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.Blob;
import com.google.firebase.firestore.GeoPoint;
import com.rocket.radar.MainActivity;
import com.rocket.radar.R;
import com.rocket.radar.admin.AdminModeManager;
import com.rocket.radar.admin.AdminRepository;
import com.rocket.radar.lottery.LotteryLogic;
import com.rocket.radar.notifications.NotificationRepository;
import com.rocket.radar.profile.ProfileModel;
import com.rocket.radar.profile.ProfileRepository;
import com.rocket.radar.profile.ProfileViewModel;
import com.rocket.radar.qr.QRDialog;

import java.io.ByteArrayOutputStream;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;
import androidx.transition.Fade;
import androidx.transition.ChangeBounds;
import androidx.transition.ChangeImageTransform;
import androidx.transition.ChangeTransform;
import androidx.transition.TransitionSet;

import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
/**
 * A fragment that displays the details of a specific event.
 * This view adapts its functionality based on whether the current user is the event organizer
 * or a regular user. Organizers get options to manage entrants and edit the event, while
 * regular users can join or leave the event's waitlist.
 * Outstanding Issues: The "Edit" functionality for organizers is not yet implemented.
 */
public class EventViewFragment extends Fragment {
    public static final String TAG = EventViewFragment.class.getSimpleName();

    private static final String ARG_EVENT = "event";
    // 1. ADD ARG_IS_ORGANIZER CONSTANT
    private static final String ARG_IS_ORGANIZER = "is_organizer";
    private Event event;
    private ProfileViewModel profileViewModel;
    private EventRepository eventRepo = new EventRepository();
    private LotteryLogic lottery;

    // 2. ADD isOrganizer aS A MEMBER VARIABLE
    private boolean isOrganizer;

    private ActivityResultLauncher<PickVisualMediaRequest> pickMedia;
    private ImageView eventImageView;
    private ImageView statusBarImage;

    private AdminModeManager adminModeManager;

    /**
     * Required empty public constructor for fragment instantiation.
     */
    public EventViewFragment() {
        // Required empty public constructor
    }

    /**
     * Creates a new instance of EventViewFragment for a regular user.
     * @param event The event to display.
     * @return A new instance of EventViewFragment.
     */
    // This newInstance is for regular users
    public static EventViewFragment newInstance(Event event) {
        // Call the other newInstance, passing 'false' for the organizer flag
        return newInstance(event, false);
    }

    /**
     * Creates a new instance of EventViewFragment, specifying if the user is the organizer.
     * @param event The event to display.
     * @param isOrganizer True if the current user is the organizer of the event, false otherwise.
     * @return A new instance of EventViewFragment.
     */
    // This newInstance is for both organizers and regular users
    public static EventViewFragment newInstance(Event event, boolean isOrganizer) {
        EventViewFragment fragment = new EventViewFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_EVENT, event);
        args.putBoolean(ARG_IS_ORGANIZER, isOrganizer); // Add the flag to the bundle
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            event = (Event) getArguments().getSerializable(ARG_EVENT);
            // 3. RETRIEVE the isOrganizer flag from the bundle
            isOrganizer = getArguments().getBoolean(ARG_IS_ORGANIZER, false); // Default to false
        }

        Log.d(TAG, "onCreate: setting shared element transitions");

        // Shared element transition for image + title
        TransitionSet sharedSet = new TransitionSet()
                .addTransition(new ChangeBounds())
                .addTransition(new ChangeTransform())
                .addTransition(new ChangeImageTransform());
        sharedSet.setDuration(200); // 5 seconds, no way you miss this
        sharedSet.setInterpolator(new AccelerateDecelerateInterpolator());

        setSharedElementEnterTransition(sharedSet);
        setSharedElementReturnTransition(sharedSet);

        // Create a Fade transition for the non-shared elements
        Fade fade = new Fade();
        fade.setDuration(250);

        // DELAY: This makes the rest of the screen wait 200ms before appearing,
        // giving the image/title time to move first.
        fade.setStartDelay(100);

        // EXCLUDE: Vital! We must tell the Fade NOT to touch the views that are flying in.
        // If we don't do this, the image will try to fade in AND fly at the same time, looking glitchy.
        fade.excludeTarget(R.id.event_image, true);
        fade.excludeTarget(R.id.event_title, true);
        //fade.excludeTarget(R.id.event_date, true);
        // Also exclude the background/navigation bars to prevent system UI flickering
        fade.excludeTarget(android.R.id.statusBarBackground, true);
        fade.excludeTarget(android.R.id.navigationBarBackground, true);

        setEnterTransition(fade);

        // Register for activity result (must be done before onCreateView)
        pickMedia = registerForActivityResult(
                new ActivityResultContracts.PickVisualMedia(),
                uri -> {
                    if (uri != null && event != null) {
                        updateEventBannerImage(uri);
                    }
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.event_view, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        profileViewModel = new ViewModelProvider(requireActivity()).get(ProfileViewModel.class);
        adminModeManager = AdminModeManager.getInstance(getContext());

        // Find views
        Button backButton = view.findViewById(R.id.back_button);
        Button deleteButton = view.findViewById(R.id.delete_button);
        Button shareButton = view.findViewById(R.id.share_button);
        Button joinAndLeaveWaitlistButton = view.findViewById(R.id.join_and_leave_waitlist_button);

        // 4. DEFINE manageEntrantsButton
        Button manageEntrantsButton = view.findViewById(R.id.manage_entrants);
        eventImageView = view.findViewById(R.id.event_image);
        statusBarImage = view.findViewById(R.id.status_bar_image);
        TextView eventTitle = view.findViewById(R.id.event_title);
        TextView eventDate = view.findViewById(R.id.event_date);
        TextView eventDescription = view.findViewById(R.id.event_desc);
        TextView eventWaitlistSize = view.findViewById(R.id.waitlist_size);

        // handle image pizza bar logic logic
        if (System.currentTimeMillis() < event.getRegistrationStartDate().getTime()) {
            // set the image to pre-registration status

        } else if (System.currentTimeMillis() >= event.getRegistrationStartDate().getTime() && System.currentTimeMillis() < event.getRegistrationEndDate().getTime()) {
            // display the image for registration period
            statusBarImage.setImageResource(R.drawable.progress_bar_reg_period);
        } else  {
            // set the image to the post-registration status
            statusBarImage.setImageResource(R.drawable.progress_bar_selection_final);
        }

        // Populate static event data
        if (event != null) {
                // These must match the strings we created in the Adapter exactly
                ViewCompat.setTransitionName(eventImageView, "img_" + event.getEventId());
                ViewCompat.setTransitionName(eventTitle, "title_" + event.getEventId());
                ViewCompat.setTransitionName(eventDate, "date_" + event.getEventId());

            lottery = new LotteryLogic(event);
            eventTitle.setText(event.getEventTitle());
            if (event.getEventStartDate() != null) { // Check event.getDate() for null
                String FormattedDate = DateFormat.getDateInstance(DateFormat.FULL).format(event.getEventStartDate());
                eventDate.setText(FormattedDate);
            }
            eventDescription.setText(event.getDescription());

            // Load and display the event banner image
            if (event.getBannerImageBlob() != null) {
                Bitmap bannerBitmap = event.getBannerImageBitmap();
                if (bannerBitmap != null) {
                    eventImageView.setImageBitmap(bannerBitmap);
                }
            }

            eventRepo.getWaitlistSize(event, new EventRepository.WaitlistSizeListener() {
                @Override
                public void onSizeReceived(int size) {
                    // This code runs when the size is successfully fetched.
                    if (isAdded()) { // Ensure fragment is still attached
                        Log.d("EventViewFragment", "Waitlist size received: " + size);

                        // 1. Display the current size
                        eventWaitlistSize.setText("People on waitlist: " + size);

                        if (isOrganizer) {
                            return;
                        }
                        // 2. Check Capacity Logic
                        int capacity = 0;
                        try {
                            // If getWaitlistCapacity() returns Integer, this handles null safely
                            if (event.getWaitlistCapacity() != null) {
                                capacity = event.getWaitlistCapacity();
                            }
                        } catch (Exception e) {
                            capacity = -1; // Treat as unlimited if error occurs
                        }// Assuming getter exists in Event model

                        // Only apply logic if there is a limit (capacity > 0) AND user is not already on the list
                        if (capacity > 0 && size >= capacity && !isOnWaitlist(profileViewModel.getProfileLiveData().getValue())) {
                            joinAndLeaveWaitlistButton.setEnabled(false);
                            joinAndLeaveWaitlistButton.setText("Waitlist Full");
                            // Optional: Change background color to grey explicitly if standard disabled state isn't enough
                            joinAndLeaveWaitlistButton.setBackgroundColor(getResources().getColor(android.R.color.darker_gray, null));
                        } else if (System.currentTimeMillis() < event.getRegistrationStartDate().getTime()){
                            joinAndLeaveWaitlistButton.setEnabled(false);
                            joinAndLeaveWaitlistButton.setText("Registration Not Started");
                            // Optional: Change background color to grey explicitly if standard disabled state isn't enough
                            joinAndLeaveWaitlistButton.setBackgroundColor(getResources().getColor(android.R.color.darker_gray, null));
                        } else if (System.currentTimeMillis() > event.getRegistrationEndDate().getTime() && /*not on waitlist */ (!isOnWaitlist(profileViewModel.getProfileLiveData().getValue()) || /*is theyre on the waitlist but the invited list isnt empty, then they didnt get selected so this staement should evaluate to true*/ !event.getEventInvitedIds().isEmpty())){
                            joinAndLeaveWaitlistButton.setEnabled(false);
                            joinAndLeaveWaitlistButton.setText("Registration Closed");
                            // Optional: Change background color to grey explicitly if standard disabled state isn't enough
                            joinAndLeaveWaitlistButton.setBackgroundColor(getResources().getColor(android.R.color.darker_gray, null));
                        } else {
                            // Ensure button is enabled if space is available (or if already on list so they can leave)
                            // NOTE: The specific text (Join/Leave) is handled in the button setup logic further down in your file
                            joinAndLeaveWaitlistButton.setEnabled(true);
                        }
                    }
                }

                @Override
                public void onWaitlistEntrantsFetched(List<String> userIds) {

                }

                @Override
                public void onError(Exception e) {
                    // This code runs on failure.
                    Log.e("EventViewFragment", "Error getting waitlist size.", e);
                    if (isAdded()) {
                        eventWaitlistSize.setText("Waitlist size unavailable");
                    }
                }
            });
        } else {
            Toast.makeText(getContext(), "Error: Event data missing.", Toast.LENGTH_SHORT).show();
            navigateBack();
            return;
        }

        ProfileModel currentProfile = profileViewModel.getProfileLiveData().getValue();
        boolean isOnWaitlist = isOnWaitlist(currentProfile);
        ArrayList<String> onInvitedEventIds = currentProfile.getOnInvitedEventIds();
        Log.d(TAG, "onInvitedEventIds: " + onInvitedEventIds);
        boolean isInvited = onInvitedEventIds.contains(event.getEventId());
        Log.d(TAG, "isInvited: " + isInvited);
        ArrayList<String> onAttendingEventIds = currentProfile.getAttendingEventIds();
        boolean isAttending = onAttendingEventIds.contains(event.getEventId());
        Log.d(TAG, "isAttending: " + isAttending);

        shareButton.setOnClickListener(v -> {
            new QRDialog(requireContext(), event.getEventId()).show(requireActivity().getSupportFragmentManager(), QRDialog.TAG);
        });

        if (currentProfile.getRole() == ProfileModel.UserRole.ADMIN && adminModeManager.isAdminModeOn()) {
            deleteButton.setVisibility(View.VISIBLE);
            deleteButton.setOnClickListener(v -> {
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Delete this event?")
                        .setMessage("This will permanently remove this event and all associated data. This action cannot be undone.")
                        .setNegativeButton("Cancel", (dialog, which) -> {
                            dialog.dismiss();
                        })
                        .setPositiveButton("Delete Event", (dialog, which) -> {
                            AdminRepository adminRepository = new AdminRepository();
                            adminRepository.deleteEvent(event.getEventId(),
                                    new AdminRepository.DeleteEventCallback() {
                                        @Override
                                        public void onSuccess() {
                                            Log.d(TAG, "Navigating back from deleted event: " + event.getEventId());
                                            navigateBack();
                                        }

                                        @Override
                                        public void onError(Exception e) {
                                            Log.d(TAG, "Error deleting event: " + event.getEventId() + e.toString());
                                        }
                                    }
                            );
                        })
                        .show();
            });
        } else {
            deleteButton.setVisibility(View.GONE);
        }

        // 5. THE LOGIC BLOCK CAN NOW USE THE DEFINED VARIABLES
        if (isOrganizer) {
            // Organizer View

            // 1. Configure the "Manage Entrants" button
            manageEntrantsButton.setVisibility(View.VISIBLE);
            manageEntrantsButton.setOnClickListener(v -> {
                OrganizerEntrantsFragment organizerEntrantsFragment = OrganizerEntrantsFragment.newInstance(event);
                if (getActivity() != null) {
                    getActivity().getSupportFragmentManager().beginTransaction()
                            .replace(R.id.nav_host_fragment, organizerEntrantsFragment)
                            .addToBackStack(null)
                            .commit();
                }
            });

            // 2. Repurpose the other button as "Edit"
            joinAndLeaveWaitlistButton.setVisibility(View.VISIBLE); // Make sure it is VISIBLE

            long currentTime = System.currentTimeMillis();
            long deadlineTime = 0;

            // Assuming getRegistrationDeadline() returns a Date object.
            // If it returns null, we assume immediate access.
            if (event.getRegistrationEndDate() != null) {
                deadlineTime = event.getRegistrationEndDate().getTime();
            }

            if (currentTime < deadlineTime) {
                // Deadline has NOT passed yet
                joinAndLeaveWaitlistButton.setEnabled(false);
                joinAndLeaveWaitlistButton.setText("Lottery Locked");
                joinAndLeaveWaitlistButton.setBackgroundColor(getResources().getColor(android.R.color.darker_gray, null));

                // Optional: Add a small toast or click listener to explain why
                joinAndLeaveWaitlistButton.setOnClickListener(v ->
                        Toast.makeText(getContext(), "Waiting for registration deadline to pass.", Toast.LENGTH_SHORT).show()
                );
            } else {
                // Deadline HAS passed
                joinAndLeaveWaitlistButton.setEnabled(true);
                joinAndLeaveWaitlistButton.setText("Run Lottery");
                // Restore original listener
                joinAndLeaveWaitlistButton.setOnClickListener(v -> lottery.handleRunLottery(event));
            }

            joinAndLeaveWaitlistButton.setText("Run Lottery");
            joinAndLeaveWaitlistButton.setOnClickListener(v -> lottery.handleRunLottery(event));

            // 3. Allow organizer to click banner image to change it
            eventImageView.setClickable(true);
            eventImageView.setOnClickListener(v -> {
                pickMedia.launch(new PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                        .build()
                );
            });

        } else if (isAttending) {
            // user is attending event, hide manage entrants button, and set joinandleavebutton to a message
            manageEntrantsButton.setVisibility(View.GONE);
            joinAndLeaveWaitlistButton.setText("We'll see you there!");
            // joinandlaave button should be unclickable
            joinAndLeaveWaitlistButton.setClickable(false);

        } else if (isInvited) {
            // invited user view
            manageEntrantsButton.setVisibility(View.VISIBLE);
            manageEntrantsButton.setText("Accept Invitation");
            manageEntrantsButton.setOnClickListener(v -> {
                // TODO: Implement accept invitation

                // event side attending list
                // call to event eventRepo
                eventRepo.addUserToAttending(event, currentProfile.getUid());
                eventRepo.removeUserFromInvited(event, currentProfile.getUid());

                // client side list of attending events
                // call to profile model
                // Logic for joining a waitlist
                currentProfile.addAttendingEventId(event.getEventId());
                currentProfile.removeInvitedEventId(event.getEventId());
                currentProfile.removeOnWaitlistEventId(event.getEventId());

                // 1. Get the location from the user's profile.
                // 2. Pass the user ID and location to the eventRepository method.


                navigateBack();

                Toast.makeText(getContext(), "Invitation accepted", Toast.LENGTH_SHORT).show();
            });

            joinAndLeaveWaitlistButton.setVisibility(View.VISIBLE);
            joinAndLeaveWaitlistButton.setText("Reject Invitation");
            joinAndLeaveWaitlistButton.setOnClickListener(v -> {
                // TODO: Implement reject invitation
                // deal with backend stuff
                eventRepo.addUserToCancelled(event, currentProfile.getUid());
                eventRepo.removeUserFromInvited(event, currentProfile.getUid());

                // deal with client side logic
                currentProfile.addCancelledEventId(event.getEventId());
                currentProfile.removeInvitedEventId(event.getEventId());
                currentProfile.removeOnWaitlistEventId(event.getEventId());

                // automatically re-run the lottery for 1 person
                lottery.handleRunLottery(event, 1);

                navigateBack();
                Toast.makeText(getContext(), "Invitation rejected (not implemented)", Toast.LENGTH_SHORT).show();
            });

        } else {
            // regular (non invited/waitlisted) User View

            // Hide the organizer button
            manageEntrantsButton.setVisibility(View.GONE);

            // Configure the "Join/Leave Waitlist" button
            joinAndLeaveWaitlistButton.setVisibility(View.VISIBLE);
            joinAndLeaveWaitlistButton.setOnClickListener(v -> handleJoinLeaveWaitlist());
            profileViewModel.getProfileLiveData().observe(getViewLifecycleOwner(), profile -> {
                updateWaitlistButton(joinAndLeaveWaitlistButton, profile);
            });
        }

        // Setup listeners
        backButton.setOnClickListener(v -> navigateBack());
        // REMOVED redundant listeners from here as they are now correctly placed inside the if/else block
    }

    /**
     * Hides the main activity's bottom navigation bar when the fragment is resumed.
     */

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBottomNavigationVisibility(View.GONE);
        }
    }

    /**
     * Restores the main activity's bottom navigation bar when the fragment is stopped.
     */
    @Override
    public void onStop() {
        super.onStop();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBottomNavigationVisibility(View.VISIBLE);
        }
    }

    /**
     * Navigates back to the previous fragment in the back stack.
     */
    private void navigateBack() {        // We use the Navigation Controller to pop the stack.
        // This ensures we return to the previous screen (Profile or List)
        // exactly as we left it, maintaining the NavController connection.
        if (getView() != null) {
            androidx.navigation.Navigation.findNavController(getView()).popBackStack();
        }
    }

    /**
     * Handles the logic for a user joining or leaving the event's waitlist.
     * It updates the user's profile and the event's waitlist in the database.
     */
    private void handleJoinLeaveWaitlist() {
        ProfileModel currentProfile = profileViewModel.getProfileLiveData().getValue();
        if (currentProfile == null || event == null) {
            Toast.makeText(getContext(), "Error: Profile or event data not available.", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean onWaitlist = isOnWaitlist(currentProfile);
        boolean isInvited = currentProfile.getOnInvitedEventIds().contains(event.getEventId());

        if (onWaitlist) {
            // Logic for leaving a waitlist (remains unchanged)
            currentProfile.removeOnWaitlistEventId(event.getEventId());
            currentProfile.removeOnMyEventId(event.getEventId());
            // This needs to be updated to use the correct subcollection name if you changed it
            // For now assuming the logic in removeUserFromWaitlist is correct
            eventRepo.removeUserFromWaitlist(event, currentProfile.getUid());
            navigateBack();
            Toast.makeText(getContext(), "Removed from waitlist!", Toast.LENGTH_SHORT).show();
        } else {
            // Logic for joining a waitlist
            currentProfile.addOnWaitlistEventId(event.getEventId());

            // 1. Get the location from the user's profile.
            GeoPoint lastKnownLocation = currentProfile.getLastKnownLocation();

            // 2. Pass the user ID and location to the eventRepository method.
            eventRepo.addUserToWaitlist(event, currentProfile.getUid(), lastKnownLocation);

            navigateBack();
            Toast.makeText(getContext(), "Added to waitlist!", Toast.LENGTH_SHORT).show();
        }
        // After changing the profile, we must save it back to the ViewModel to persist the change
        profileViewModel.updateProfile(currentProfile);
    }

    /**
     * Updates the text and enabled state of the waitlist button based on the user's current status.
     * @param button The button to update.
     * @param profile The current user's profile.
     */
    private void updateWaitlistButton(Button button, ProfileModel profile) {
        if (event == null || profile == null) {
            button.setEnabled(false);
            return;
        }
        boolean onWaitlist = isOnWaitlist(profile);
        button.setText(onWaitlist ? "Leave Waitlist" : "Join Waitlist");
        button.setEnabled(true);
    }

//    private void updateInviteButton(Button button, ProfileModel profile){
//        if (event == null || profile == null) {
//            button.setEnabled(false);
//            return;
//        }
//        button.setText("Reject Invitiation");
//        button.setEnabled(true);
//    }

    /**
     * Checks if the current user is on the waitlist for the event.
     * @param profile The current user's profile.
     * @return True if the user is on the waitlist, false otherwise.
     */
    private boolean isOnWaitlist(ProfileModel profile) {
        if (profile == null || event == null || profile.getOnWaitlistEventIds() == null) {
            return false;
        }
        return profile.getOnWaitlistEventIds().contains(event.getEventId());
    }

    /**
     * Updates the event banner image in Firebase and refreshes the UI.
     * @param uri The URI of the selected image
     */
    private void updateEventBannerImage(Uri uri) {
        try {
            // Convert URI to Bitmap
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(requireActivity().getContentResolver(), uri);

            // Compress bitmap to Blob
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream);
            Blob imageBlob = Blob.fromBytes(outputStream.toByteArray());

            // Update the image in the UI immediately
            eventImageView.setImageBitmap(bitmap);

            // Update the event object
            event.setBannerImageBlob(imageBlob);

            // Save to Firebase using createEvent (which uses set() and will update if exists)
            eventRepo.createEvent(event);
            Toast.makeText(getContext(), "Event banner updated!", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Log.e(TAG, "Error processing image", e);
            Toast.makeText(getContext(), "Failed to process image", Toast.LENGTH_SHORT).show();
        }
    }
}
