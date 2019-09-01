package com.android.systemui.egg;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import com.android.systemui.R;

public class MLandActivity extends Activity {
    MLand mLand;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mland);
        this.mLand = (MLand) findViewById(R.id.world);
        this.mLand.setScoreFieldHolder((ViewGroup) findViewById(R.id.scores));
        this.mLand.setSplash(findViewById(R.id.welcome));
        int numControllers = this.mLand.getGameControllers().size();
        if (numControllers > 0) {
            this.mLand.setupPlayers(numControllers);
        }
    }

    public void updateSplashPlayers() {
        int N = this.mLand.getNumPlayers();
        View minus = findViewById(R.id.player_minus_button);
        View plus = findViewById(R.id.player_plus_button);
        if (N == 1) {
            minus.setVisibility(4);
            plus.setVisibility(0);
            plus.requestFocus();
            return;
        }
        MLand mLand2 = this.mLand;
        if (N == 6) {
            minus.setVisibility(0);
            plus.setVisibility(4);
            minus.requestFocus();
            return;
        }
        minus.setVisibility(0);
        plus.setVisibility(0);
    }

    public void onPause() {
        this.mLand.stop();
        super.onPause();
    }

    public void onResume() {
        super.onResume();
        this.mLand.onAttachedToWindow();
        updateSplashPlayers();
        this.mLand.showSplash();
    }

    public void playerMinus(View v) {
        this.mLand.removePlayer();
        updateSplashPlayers();
    }

    public void playerPlus(View v) {
        this.mLand.addPlayer();
        updateSplashPlayers();
    }

    public void startButtonPressed(View v) {
        findViewById(R.id.player_minus_button).setVisibility(4);
        findViewById(R.id.player_plus_button).setVisibility(4);
        this.mLand.start(true);
    }
}
