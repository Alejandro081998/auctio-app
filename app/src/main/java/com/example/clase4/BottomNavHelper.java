package com.example.clase4;

import android.app.Activity;
import android.content.Intent;
import android.view.View;

public class BottomNavHelper {

    public static void configurar(Activity activity) {
        View navHome = activity.findViewById(R.id.navHome);
        View navAuctions = activity.findViewById(R.id.navAuctions);
        View navActivity = activity.findViewById(R.id.navActivity);
        View navProfile = activity.findViewById(R.id.navProfile);

        if (navHome != null) {
            navHome.setOnClickListener(v -> {
                if (!(activity instanceof HomeActivity)) {
                    Intent intent = new Intent(activity, HomeActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    activity.startActivity(intent);
                }
            });
        }

        if (navAuctions != null) {
            navAuctions.setOnClickListener(v -> {
                if (activity instanceof HomeActivity) {
                    ((HomeActivity) activity).irASeccionSubastas();
                } else {
                    Intent intent = new Intent(activity, HomeActivity.class);
                    intent.putExtra("goToAuctions", true);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    activity.startActivity(intent);
                }
            });
        }

        if (navActivity != null) {
            navActivity.setOnClickListener(v -> {
                if (!(activity instanceof HistoryActivity)) {
                    Intent intent = new Intent(activity, HistoryActivity.class);
                    activity.startActivity(intent);
                }
            });
        }

        if (navProfile != null) {
            navProfile.setOnClickListener(v -> {
                if (!(activity instanceof ProfileActivity)) {
                    Intent intent = new Intent(activity, ProfileActivity.class);
                    activity.startActivity(intent);
                }
            });
        }
    }
}