package com.rocket.radar.notifications;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.transition.Hold; // <--- The fix is here
import com.rocket.radar.R;
import com.rocket.radar.events.Event;
import com.rocket.radar.events.EventRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;


public class NotificationFragment extends Fragment {

    private RecyclerView notificationRecyclerView;
    private TextView emptyNotificationsTextView;
    private Button backButton;

    private NotificationAdapter adapter;
    private NotificationRepository notificationRepository;
    private RecyclerView.AdapterDataObserver adapterObserver;

    // Stores the position so the shared element knows where to return
    private int savedClickedPosition = -1;

    // NEW: Added onCreate to set the Exit Transition correctly
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // This prevents the list from disappearing immediately when you click an item.
        // It "Holds" the view in place while the shared element flies out.
        setExitTransition(new Hold());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.notification_list, container, false);
        notificationRepository = new NotificationRepository();
        notificationRecyclerView = view.findViewById(R.id.notification_recycler_view);
        emptyNotificationsTextView = view.findViewById(R.id.empty_notifications_text);
        backButton = view.findViewById(R.id.back_arrow);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Postpone the enter transition until we say so!
        postponeEnterTransition();

        setupRecyclerView();
        setupClickListeners();
        observeNotifications();

        // 2. Handle the RETURN transition mapping
        setExitSharedElementCallback(new androidx.core.app.SharedElementCallback() {
            @Override
            public void onMapSharedElements(java.util.List<String> names, java.util.Map<String, View> sharedElements) {
                if (savedClickedPosition < 0) return;

                RecyclerView.ViewHolder selectedViewHolder =
                        notificationRecyclerView.findViewHolderForAdapterPosition(savedClickedPosition);

                if (selectedViewHolder == null || selectedViewHolder.itemView == null) {
                    return;
                }

                if (selectedViewHolder instanceof NotificationAdapter.NotificationViewHolder) {
                    NotificationAdapter.NotificationViewHolder holder =
                            (NotificationAdapter.NotificationViewHolder) selectedViewHolder;

                    if (holder.eventImage != null && holder.eventImage.getTransitionName() != null) {
                        sharedElements.put(holder.eventImage.getTransitionName(), holder.eventImage);
                    }
                    if (holder.eventTitle != null && holder.eventTitle.getTransitionName() != null) {
                        sharedElements.put(holder.eventTitle.getTransitionName(), holder.eventTitle);
                    }
                }
            }
        });

        // Note: We do NOT add the PreDrawListener here anymore.
        // We add it only AFTER the images are fetched in observeNotifications.
    }

    private void setupRecyclerView() {
        EventRepository eventRepository = new EventRepository();

        adapter = new NotificationAdapter(
                getContext(),
                new ArrayList<>(),
                notificationRepository,
                eventRepository
        );

        adapter.setOnItemClickListener(position -> {
            this.savedClickedPosition = position;
        });

        notificationRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        notificationRecyclerView.setAdapter(adapter);

        adapterObserver = new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                checkEmpty();
            }
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                checkEmpty();
            }
            @Override
            public void onItemRangeRemoved(int positionStart, int itemCount) {
                super.onItemRangeRemoved(positionStart, itemCount);
                checkEmpty();
            }
            void checkEmpty() {
                if (adapter.getItemCount() == 0) {
                    emptyNotificationsTextView.setVisibility(View.VISIBLE);
                    notificationRecyclerView.setVisibility(View.GONE);
                } else {
                    emptyNotificationsTextView.setVisibility(View.GONE);
                    notificationRecyclerView.setVisibility(View.VISIBLE);
                }
            }
        };
        adapter.registerAdapterDataObserver(adapterObserver);
        checkEmpty();
    }

    private void setupClickListeners() {
        backButton.setOnClickListener(v -> {
            if (getParentFragmentManager() != null) {
                getParentFragmentManager().popBackStack();
            }
        });
    }

    private void observeNotifications() {
        notificationRepository.getMyNotifications().observe(getViewLifecycleOwner(), newNotifications -> {
            Log.d("NotificationFragment", "Data updated. " + newNotifications.size() + " notifications.");

            newNotifications.sort((n1, n2) -> {
                int readCompare = Boolean.compare(n1.isReadStatus(), n2.isReadStatus());
                if (readCompare != 0) return readCompare;
                if (n1.getTimestamp() != null && n2.getTimestamp() != null) {
                    return n2.getTimestamp().compareTo(n1.getTimestamp());
                }
                return 0;
            });

            // 1. PRE-FETCH: Identify all Event IDs needed
            List<String> eventIdsToFetch = new ArrayList<>();
            for (Notification n : newNotifications) {
                if (n.getEventId() != null && !eventIdsToFetch.contains(n.getEventId())) {
                    eventIdsToFetch.add(n.getEventId());
                }
            }

            if (eventIdsToFetch.isEmpty()) {
                // No images to load? Update list and start animation immediately.
                adapter.setNotifications(newNotifications);
                waitForLayoutAndStartTransition();
                return;
            }

            // 2. Bulk Fetch Events
            EventRepository eventRepo = new EventRepository();
            List<Event> loadedEvents = new ArrayList<>();
            AtomicInteger counter = new AtomicInteger(eventIdsToFetch.size());

            for (String id : eventIdsToFetch) {
                eventRepo.getEventById(id, new EventRepository.SingleEventListener() {
                    @Override
                    public void onEventLoaded(Event event) {
                        if (event != null) {
                            loadedEvents.add(event);
                        }
                        checkCompletion();
                    }

                    @Override
                    public void onError(Exception e) {
                        checkCompletion();
                    }

                    private void checkCompletion() {
                        if (counter.decrementAndGet() == 0) {
                            // All events fetched!
                            if (isAdded() && getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    // 3. Update Adapter with Cache AND Notifications
                                    adapter.updateEventCache(loadedEvents);
                                    adapter.setNotifications(newNotifications);

                                    // 4. NOW start the transition
                                    waitForLayoutAndStartTransition();
                                });
                            }
                        }
                    }
                });
            }
        });
    }

    private void waitForLayoutAndStartTransition() {
        notificationRecyclerView.getViewTreeObserver().addOnPreDrawListener(new android.view.ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                // If we are returning from an event detail, scroll to position
                if (savedClickedPosition != -1) {
                    RecyclerView.ViewHolder holder = notificationRecyclerView.findViewHolderForAdapterPosition(savedClickedPosition);
                    if (holder == null) {
                        notificationRecyclerView.scrollToPosition(savedClickedPosition);
                        return false; // Retry next frame
                    }
                }

                notificationRecyclerView.getViewTreeObserver().removeOnPreDrawListener(this);
                startPostponedEnterTransition(); // Start the animation now that images are ready!
                return true;
            }
        });
    }

    private void checkEmpty() {
        if (adapter != null && emptyNotificationsTextView != null && notificationRecyclerView != null) {
            if (adapter.getItemCount() == 0) {
                emptyNotificationsTextView.setVisibility(View.VISIBLE);
                notificationRecyclerView.setVisibility(View.GONE);
            } else {
                emptyNotificationsTextView.setVisibility(View.GONE);
                notificationRecyclerView.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (adapter != null && adapterObserver != null) {
            adapter.unregisterAdapterDataObserver(adapterObserver);
        }
    }
}
