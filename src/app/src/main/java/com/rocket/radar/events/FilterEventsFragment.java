package com.rocket.radar.events;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CalendarView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.rocket.radar.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * A Fragment that provides a UI for filtering events.
 *
 * <p>This fragment allows users to select specific categories via chips or a minimum date
 * via a calendar. It interacts with the shared {@link FilterModel} to apply these filters
 * to the global event stream observed by the {@link EventListFragment}.</p>
 *
 * <p>It also utilizes an internal {@link FilterEventsFragmentState} ViewModel to persist
 * the state of the UI controls (selected chips, selected date) across configuration changes.</p>
 *
 * <p><strong>Outstanding Issues:</strong>
 * <ul>
 *   <li>The "Confirm" button currently just closes the fragment, as filters are applied immediately
 *       inside the change listeners. The UI implies a transactional "Apply" action which isn't strictly true.</li>
 *   <li>The logic mapping Chip IDs to indices ({@code chipGroup.getChildAt(selectedChipId)}) is fragile
 *       and may break if the layout hierarchy changes.</li>
 * </ul>
 * </p>
 */
public class FilterEventsFragment extends Fragment {
    public final static String TAG = FilterEventsFragment.class.getSimpleName();
    private FilterModel filterModel;
    private ChipGroup chipGroup;

    /**
     * A ViewModel specifically for holding the transient UI state of the filter screen.
     * This ensures that selected chips and dates persist if the user rotates the screen
     * or momentarily navigates away.
     */
    public static class FilterEventsFragmentState extends ViewModel {
        public List<Integer> selectedChips = null;
        public Date selectedDate = null;

        public FilterEventsFragmentState() {}
    }

    // This persists the fragment state across creations/destruction.
    private FilterEventsFragmentState uiState;

    /**
     * Called to have the fragment instantiate its user interface view.
     *
     * @param inflater           The LayoutInflater object that can be used to inflate
     *                           any views in the fragment.
     * @param container          If non-null, this is the parent view that the fragment's
     *                           UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     *                           from a previous saved state as given here.
     * @return Return the View for the fragment's UI.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_filter_events, container, false);
    }

    /**
     * Called immediately after {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}
     * has returned, but before any saved state has been restored in to the view.
     * This method sets up the CalendarView, ChipGroup, and buttons, and binds them to
     * the {@link FilterModel} for logic and {@link FilterEventsFragmentState} for persistence.
     *
     * @param view               The View returned by {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     *                           from a previous saved state as given here.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Fetch our required views
        CalendarView calendarView = view.findViewById(R.id.calendar_view);
        Button cancelButton = view.findViewById(R.id.cancel_button);
        Button confirmButton = view.findViewById(R.id.confirm_button);
        chipGroup = view.findViewById(R.id.interests_chip_group);

        // Get the view models we need
        var provider = new ViewModelProvider(requireActivity());
        filterModel = provider.get(FilterModel.class);
        uiState = provider.get(FilterEventsFragmentState.class);

        if (uiState.selectedChips != null) {
            for (var selectedChipId : uiState.selectedChips) {
                View maybeChip = chipGroup.getChildAt(selectedChipId);
                if (maybeChip instanceof Chip) {
                    Chip chip = (Chip)maybeChip;
                    chip.setChecked(true);
                }
            }
        }

        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) ->  {
            filterModel.removeFiltersByCategory("category");
            uiState.selectedChips = checkedIds;
            for (var selectedChipId : checkedIds) {
                String category = ((Chip)chipGroup.findViewById(selectedChipId)).getText().toString();

                filterModel.addFilter(new FilterModel.EventFilter() {
                    @Override
                    public boolean filter(Event event) {
                        return event.getCategories().contains(category);
                    }

                    @NonNull
                    @Override
                    public String getFilterName() {
                        return category;
                    }

                    @Override
                    protected String getFilterCategory() {
                        return "category";
                    }
                });
            }
        });

        calendarView.setOnDateChangeListener((view1, year, month, dayOfMonth) -> {
            // Use Calendar to correctly build a Date object
            Calendar calendar = Calendar.getInstance();
            calendar.clear();
            calendar.set(year, month, dayOfMonth);
            Date selectedDate = calendar.getTime();
            uiState.selectedDate = selectedDate;

            // update filter model with the setDate method
            Log.d("FilterEventsFragment", "Date selected: " + selectedDate);
            final String dateCategory = "eventDate";
            filterModel.removeFiltersByCategory(dateCategory);
            filterModel.addFilter(new FilterModel.EventFilter() {
                final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");

                @Override
                public boolean filter(Event event) {
                    return event.getEventStartDate().after(selectedDate);
                }

                @NonNull
                @Override
                public String getFilterName() {
                    return formatter.format(selectedDate);
                }

                @Override
                protected String getFilterCategory() {
                    return dateCategory;
                }
            });
        });

        // Set OnClickListener for the cancel button
        cancelButton.setOnClickListener(v -> {
            // Pop the back stack to return to the previous fragment (EventListFragment)
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });

        // Set OnClickListener for the confirm button
        confirmButton.setOnClickListener(v -> {
            // For now, it will also just pop the back stack.
            // TODO: Implement filter logic before popping the stack.
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });
    }
}
