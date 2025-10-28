package com.rocket.radar;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.rocket.radar.databinding.NavBarBinding;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private NavBarBinding navBarBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        RecyclerView recyclerView = findViewById(R.id.event_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        List<Event> eventList = new ArrayList<>();
        // Add sample data
        List<User> waitlist1 = new ArrayList<>();
        waitlist1.add(new User("Alice"));
        waitlist1.add(new User("Bob"));
        eventList.add(new Event(System.currentTimeMillis(), "Rocket Launch", R.drawable.ic_notification_24dp, waitlist1));

        List<User> waitlist2 = new ArrayList<>();
        waitlist2.add(new User("Charlie"));
        eventList.add(new Event(System.currentTimeMillis() + 3600000, "Moon Landing", R.drawable.ic_patient_list_fill_24dp, waitlist2));


        EventAdapter adapter = new EventAdapter(eventList);
        recyclerView.setAdapter(adapter);
    }
}
