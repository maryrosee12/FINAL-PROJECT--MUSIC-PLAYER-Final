package com.example.musicplayer;

import android.Manifest;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;

import com.example.musicplayer.databinding.ActivityMainBinding;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private ActivityResultLauncher<String[]> requestPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher = registerForActivityResult(
                    new ActivityResultContracts.RequestMultiplePermissions(),
                    isGranted -> {
                        if (isGranted.containsValue(false)) {
                            Toast.makeText(
                                    MainActivity.this,
                                    "This app needs to access your media and audio files to function properly.",
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    });

            requestPermissionLauncher.launch(new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.READ_MEDIA_AUDIO,
                    Manifest.permission.READ_MEDIA_VIDEO,
                    Manifest.permission.POST_NOTIFICATIONS
            });

        } else {
            requestPermissionLauncher = registerForActivityResult(
                    new ActivityResultContracts.RequestMultiplePermissions(),
                    isGranted -> {
                        if (!isGranted.getOrDefault(Manifest.permission.READ_EXTERNAL_STORAGE, false)) {
                            Toast.makeText(
                                    MainActivity.this,
                                    "This app needs to access your media and audio files to function properly.",
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    });

            requestPermissionLauncher.launch(new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();


        BottomNavigationView navView = findViewById(R.id.bottomNavigationView);


        NavController navController = Navigation.findNavController(
                this,
                R.id.nav_host_fragment
        );


        NavigationUI.setupWithNavController(navView, navController);
    }
}
