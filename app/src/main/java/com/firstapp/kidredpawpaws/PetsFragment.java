package com.firstapp.kidredpawpaws;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class PetsFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_pets, container, false);

        View.OnClickListener comingSoon = v -> 
            Toast.makeText(getContext(), "Feature coming soon", Toast.LENGTH_SHORT).show();

        // Add listeners to "View Details" or cards if needed
        // For prototype feel, making the notifications and hero card clickable
        view.findViewById(R.id.iv_app_logo_mini).setOnClickListener(comingSoon);

        return view;
    }
}