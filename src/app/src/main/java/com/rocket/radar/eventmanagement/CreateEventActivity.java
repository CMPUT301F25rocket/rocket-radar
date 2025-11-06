package com.rocket.radar.eventmanagement;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.maxkeppeler.sheets.calendar.CalendarSheet;
import com.maxkeppeler.sheets.calendar.SelectionMode;
import com.maxkeppeler.sheets.clock.ClockSheet;
import com.maxkeppeler.sheets.color.ColorSheet;
import com.maxkeppeler.sheets.core.SheetStyle;
import com.rocket.radar.MainActivity;
import com.rocket.radar.R;
import com.rocket.radar.databinding.ActivityCreateEventBinding;
import com.rocket.radar.events.Event;
import com.rocket.radar.events.EventRepository;

import org.w3c.dom.Text;

import java.util.Date;
import java.util.NoSuchElementException;
import java.util.Optional;

import kotlin.Unit;

/**
 * Activity that walks a user through filling out the various pieces of information needed to create
 * an event. This activity makes heavy use of view and databinding. Here are some relevant pieces of 
 * information:
 * - https://developer.android.com/reference/com/google/android/material/bottomsheet/BottomSheetBehavior
 * - https://stackoverflow.com/questions/55682256/android-set-bottom-sheet-state-in-xml
 * - https://medium.com/@mananwason/bottoms-sheets-in-android-280c03280072
 */
public class CreateEventActivity extends AppCompatActivity implements BottomSheetProvider {
    public static final String TAG = CreateEventActivity.class.getSimpleName();
    ActivityCreateEventBinding binding;
    CreateEventModel model;
    EventRepository eventRepository;

    private EventGeneralFragment eventGeneralFragment;
    private EventDateTimeFragment eventDateTimeFragment;
    private EventDeadlinesFragment eventDeadlinesFragment;
    private EventLotteryFragment eventLotteryFragment;
    private EventStyleFragment eventStyleFragment;

    private Fragment fragment;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        eventRepository = new EventRepository();

        // These three lines took way too long to write. ʕノ•ᴥ•ʔノ ︵ ┻━┻
        // WARN: Make sure when you create variables you call setMyVarName(...) on the binding.
        binding = ActivityCreateEventBinding.inflate(getLayoutInflater());
        model = new CreateEventModel();

        // NOTE: may be better to lazy load these but I don't want to.
        eventGeneralFragment = new EventGeneralFragment();
        eventDateTimeFragment = new EventDateTimeFragment();
        eventDeadlinesFragment = new EventDeadlinesFragment();
        eventLotteryFragment = new EventLotteryFragment();
        eventStyleFragment = new EventStyleFragment();

        // Main navigation buttons
        binding.createEventWizardNavLeftButton.setOnClickListener(btn -> {
            model.prevSection();
        });
        binding.createEventWizardNavRightButton.setOnClickListener(btn -> {
            if (model.getSection().getValue() == Section.lastSection) {
                try {
                    String uuid = model.createEvent(getContentResolver(), eventRepository);
                    Intent intent = new Intent(CreateEventActivity.this, MainActivity.class);
                    intent.setAction(getString(R.string.intent_action_show_qr));
                    intent.putExtra("eventId", uuid);
                    startActivity(intent);
                } catch (Exception e) {
                    Log.e(TAG, "Create event failure: ", e);
                    MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(CreateEventActivity.this);
                    builder.setTitle("Something went wrong")
                            .setMessage(e.toString())
                            .setNeutralButton("Ok", (dialogInterface, which) -> {
                                dialogInterface.dismiss();
                            })
                            .create()
                            .show();
                }
            } else {
                if (fragment instanceof InputFragment) {
                    model.nextSection((InputFragment) fragment);
                } else {
                    Log.e(TAG, "Fragment " + fragment.getTag() + " is not an input fragment");
                }
            }
        });

        setContentView(binding.getRoot());
        EdgeToEdge.enable(this);


        // Bind the model to the views.
        binding.setCreateEvent(model);
        binding.setLifecycleOwner(this);

        // Observe section changes and swap fragments accordingly
        model.getSection().observe(this, section -> {
            navigateToSection(section);
        });
    }

    /**
     * Navigates to the appropriate fragment based on the current section.
     *
     * @param section The section to navigate to
     */
    private void navigateToSection(Section section) {

        switch (section) {
            case GENERAL:
                fragment = eventGeneralFragment;
                break;
            case DATETIME:
                fragment = eventDateTimeFragment;
                break;
            case DEADLINES:
                fragment = eventDeadlinesFragment;
                break;
            case LOTTERY:
                fragment = eventLotteryFragment;
                break;
            case STYLE:
                fragment = eventStyleFragment;
                break;
            default:
                Log.e(TAG, "Unknown section: " + section);
                return;
        }

        getSupportFragmentManager().beginTransaction()
                .replace(binding.createEventWizardSectionInputHolder.getId(), fragment)
                .commit();
    }

    /**
     * This is a helper function that opens a date picker on the activity.
     * @param sink The MutableLiveData to update with the selected date
     * @param view The view that triggered the picker (for focus management)
     */
    @Override
    public void openCalendarBottomSheet(MutableLiveData<Optional<Date>> sink, View view) {
        CalendarSheet calendarSheet = new CalendarSheet();
        calendarSheet.show(CreateEventActivity.this, null, sheet -> {
            sheet.style(SheetStyle.BOTTOM_SHEET);
            sheet.rangeYears(7);
            sheet.selectionMode(SelectionMode.DATE);
            sheet.displayToolbar(false);
            sheet.onPositive((start, end) -> {
                Date date = start.getTime();
                sink.setValue(Optional.of(start.getTime()));
                view.clearFocus();
                return Unit.INSTANCE;
            });

            sheet.onNegative(() -> {
                view.clearFocus();
                return Unit.INSTANCE;
            });

            sheet.onClose(() -> {
                view.clearFocus();
                return Unit.INSTANCE;
            });

            // Well the sheet library is written in kotlin so...
            return Unit.INSTANCE;
        });
    }

    /**
     * This is a helper function that displays a time picker on the activity.
     * @param sink The MutableLiveData to update with the selected time
     * @param view The view that triggered the picker (for focus management)
     */
    @Override
    public void openTimeBottomSheet(MutableLiveData<Optional<Time>> sink, View view) {
        ClockSheet clockSheet = new ClockSheet();
        clockSheet.show(CreateEventActivity.this, null, sheet -> {
            sheet.style(SheetStyle.BOTTOM_SHEET);
            sheet.displayToolbar(false);

            sheet.onPositive((epoch, hours, minutes) -> {
                sink.setValue(Optional.of(new Time(hours, minutes)));
                view.clearFocus();
                return Unit.INSTANCE;
            });

            sheet.onNegative(() -> {
                view.clearFocus();
                return Unit.INSTANCE;
            });

            sheet.onClose(() -> {
                view.clearFocus();
                return Unit.INSTANCE;
            });

            return Unit.INSTANCE;
        });
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
