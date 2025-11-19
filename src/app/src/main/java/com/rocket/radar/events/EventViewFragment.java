// C:/Users/bwood/Cmput301/rocket-radar/src/app/src/main/java/com/rocket/radar/events/EventViewFragment.java
package com.rocket.radar.events;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.Blob;
import com.google.firebase.firestore.GeoPoint;
import com.rocket.radar.MainActivity;
import com.rocket.radar.R;
import com.rocket.radar.profile.ProfileModel;
import com.rocket.radar.profile.ProfileViewModel;

import java.io.ByteArrayOutputStream;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;

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
    EventRepository repo = EventRepository.getInstance();


    // 2. ADD isOrganizer aS A MEMBER VARIABLE
    private boolean isOrganizer;

    private ActivityResultLauncher<PickVisualMediaRequest> pickMedia;
    private ImageView eventImageView;

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

        // Find views
        Button backButton = view.findViewById(R.id.back_button);
        Button joinAndLeaveWaitlistButton = view.findViewById(R.id.join_and_leave_waitlist_button);
        // 4. DEFINE manageEntrantsButton
        Button manageEntrantsButton = view.findViewById(R.id.manage_entrants);
        eventImageView = view.findViewById(R.id.event_image);
        TextView eventTitle = view.findViewById(R.id.event_title);
        TextView eventDate = view.findViewById(R.id.event_date);
        TextView eventDescription = view.findViewById(R.id.event_desc);
        TextView eventWaitlistSize = view.findViewById(R.id.waitlist_size);

        // Populate static event data
        if (event != null) {
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

            repo.getWaitlistSize(event, new EventRepository.WaitlistSizeListener() {
                @Override
                public void onSizeReceived(int size) {
                    // This code runs when the size is successfully fetched.
                    // Update the UI on the main thread.
                    if (isAdded()) { // Ensure fragment is still attached
                        Log.d("EventViewFragment", "Waitlist size received: " + size);
                        eventWaitlistSize.setText("People on waitlist: " + size);
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
            joinAndLeaveWaitlistButton.setText("Edit");
            // joinAndLeaveWaitlistButton.setOnClickListener(v -> handleEditEvent()); // Add your edit logic here

            // 3. Allow organizer to click banner image to change it
            eventImageView.setClickable(true);
            eventImageView.setOnClickListener(v -> {
                pickMedia.launch(new PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                        .build()
                );
            });

        } else {
            // Regular User View

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
    private void navigateBack() {
        if (getActivity() != null) {
            getActivity().getSupportFragmentManager().popBackStack();
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

        if (onWaitlist) {
            // Logic for leaving a waitlist (remains unchanged)
            currentProfile.removeOnWaitlistEventId(event.getEventId());
            currentProfile.removeOnMyEventId(event.getEventId());
            // This needs to be updated to use the correct subcollection name if you changed it
            // For now assuming the logic in removeUserFromWaitlist is correct
            repo.removeUserFromWaitlist(event, currentProfile.getUid());
            navigateBack();
            Toast.makeText(getContext(), "Removed from waitlist!", Toast.LENGTH_SHORT).show();
        } else {
            // Logic for joining a waitlist
            currentProfile.addOnWaitlistEventId(event.getEventId());

            // 1. Get the location from the user's profile.
            GeoPoint lastKnownLocation = currentProfile.getLastKnownLocation();

            // 2. Pass the user ID and location to the repository method.
            repo.addUserToWaitlist(event, currentProfile.getUid(), lastKnownLocation);

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
            repo.createEvent(event);
            Toast.makeText(getContext(), "Event banner updated!", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Log.e(TAG, "Error processing image", e);
            Toast.makeText(getContext(), "Failed to process image", Toast.LENGTH_SHORT).show();
        }
    }
}
