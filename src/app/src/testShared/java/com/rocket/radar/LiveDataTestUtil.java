package com.rocket.radar;

import java.util.concurrent.atomic.AtomicReference;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.test.platform.app.InstrumentationRegistry;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class LiveDataTestUtil {
    public static <T> T getOrAwaitValue(final LiveData<T> liveData)
            throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<T> data = new AtomicReference<>();

        Observer<T> observer = new Observer<T>() {
            @Override
            public void onChanged(T t) {
                data.set(t);
                latch.countDown();
                liveData.removeObserver(this);
            }
        };

        InstrumentationRegistry.getInstrumentation().runOnMainSync(
                () -> liveData.observeForever(observer)
        );

        if (!latch.await(5, TimeUnit.SECONDS)) {
            throw new AssertionError("LiveData value was never set.");
        }

        return data.get();
    }
}
