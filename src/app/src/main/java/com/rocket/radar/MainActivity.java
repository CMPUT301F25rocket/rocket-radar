package com.rocket.radar;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import com.rocket.radar.databinding.NavBarBinding;

public class MainActivity extends AppCompatActivity {
    private NavBarBinding navBarBinding;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        navBarBinding = NavBarBinding.inflate(getLayoutInflater());
        setContentView(navBarBinding.getRoot());

    }
}
