package com.rocket.radar;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentFactory;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * A custom FragmentFactory for tests. This allows us to inject dependencies
 * (like a specific FirebaseFirestore instance) into our Fragments when they
 * are created by FragmentScenario.
 */
public class TestFragmentFactory extends FragmentFactory {

    private final FirebaseFirestore db;

    public TestFragmentFactory(FirebaseFirestore db) {
        this.db = db;
    }

    @NonNull
    @Override
    public Fragment instantiate(@NonNull ClassLoader classLoader, @NonNull String className) {
        // Get the class of the fragment being created
        Class<? extends Fragment> fragmentClass = loadFragmentClass(classLoader, className);

        // If it's our NotificationFragment, create it with our Firestore instance.
        if (fragmentClass == NotificationFragment.class) {
            // This assumes NotificationFragment has a constructor that accepts NotificationRepository
            NotificationRepository repository = new NotificationRepository(db);
            return new NotificationFragment(repository);
        }

        // For any other fragment, let the default factory handle it.
        return super.instantiate(classLoader, className);
    }
}
