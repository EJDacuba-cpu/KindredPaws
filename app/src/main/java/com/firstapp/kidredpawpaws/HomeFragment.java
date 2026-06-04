package com.firstapp.kidredpawpaws;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

public class HomeFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        CardView checkupCard = view.findViewById(R.id.card_book_checkup);
        CardView groomingCard = view.findViewById(R.id.card_book_grooming);
        TextView viewAllPets = view.findViewById(R.id.tv_view_all_pets);

        View.OnClickListener toBookListener = v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateToTab(R.id.nav_book);
            }
        };

        checkupCard.setOnClickListener(toBookListener);
        groomingCard.setOnClickListener(toBookListener);

        viewAllPets.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateToTab(R.id.nav_pets);
            }
        });

        // Other interactive elements
        view.findViewById(R.id.iv_notifications).setOnClickListener(v -> 
            Toast.makeText(getContext(), "Notifications coming soon", Toast.LENGTH_SHORT).show());

        return view;
    }
}