package com.rocket.radar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@RunWith(AndroidJUnit4.class)
public class SimpleFirestoreTest {

    private FirebaseFirestore db;

    @Before
    public void setUp() {
        Context targetContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        if (FirebaseApp.getApps(targetContext).isEmpty()) {
            FirebaseApp.initializeApp(targetContext);
        }

        db = FirebaseFirestore.getInstance();
        db.setFirestoreSettings(new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(false)
                .build());
        db.useEmulator("10.0.2.2", 8080);
    }

    @Test
    public void writeAndReadFromEmulator_isSuccessful() throws ExecutionException, InterruptedException {
        // 1. Arrange: Create some data
        Map<String, Object> testData = new HashMap<>();
        testData.put("message", "Hello, Emulator!");

        // 2. Act: Write the data to the emulator
        Tasks.await(db.collection("simple_test").document("doc1").set(testData));

        // 3. Assert: Read the data back and verify it
        DocumentSnapshot snapshot = Tasks.await(db.collection("simple_test").document("doc1").get());
        assertTrue(snapshot.exists());
        assertEquals("Hello, Emulator!", snapshot.getString("message"));
    }
}
    