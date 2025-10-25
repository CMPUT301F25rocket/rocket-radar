package com.rocket.radar;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Firebase;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.rocket.radar.databinding.NavBarBinding;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private NavBarBinding navBarBinding;

    private static final String TAG = "MainActivity";
    private FirebaseAuth mAuth;

    /**
     * This method runs the first time the activity is created and only then.
     * @param savedInstanceState If the activity is being re-initialized after
     *     previously being shut down then this Bundle contains the data it most
     *     recently supplied in {@link #onSaveInstanceState}.  <b><i>Note: Otherwise it is null.</i></b>
     *
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mAuth = FirebaseAuth.getInstance(); // Initiaize Firebase Auth

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    /**
     * This method is called when the activity becomes visible to the user.
     */
    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null)
            signInAnonymously();
        else
            handleUserSignIn(currentUser);
    }

    /**
     * Signs in the user anonymously with Firebase Auth.
     * Calls updateUI() when the operation completes.
     */
    private void signInAnonymously() {
        mAuth.signInAnonymously()
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "signInAnonymously:success");
                        FirebaseUser user = mAuth.getCurrentUser();
                        handleUserSignIn(user);
                    } else {
                        Log.w(TAG, "signInAnonymously:failure", task.getException());
                        handleUserSignIn(null);
                    }
                });
    }

    private void handleUserSignIn(FirebaseUser user) {
        if (user == null) {
            Log.w(TAG, "No user signed in");
            return;
        }
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String uid = user.getUid();
        Map<String, Object> userMap = new HashMap<>();
        // userMap.put("createdAt", FieldValue.serverTimestamp()); this will get overwritten each time, so maybe implement it later
        userMap.put("lastLogin", FieldValue.serverTimestamp());

        db.collection("users")
                .document(uid)
                .set(userMap)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "User document created/updated"))
                .addOnFailureListener(e -> Log.w(TAG, "Error writing user document", e));
    }

}