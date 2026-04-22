package com.artemis.glyphtoy;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Intent to open the Nothing system Glyph Toys manager
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(
                "com.nothing.thirdparty",
                "com.nothing.thirdparty.matrix.toys.manager.ToysManagerActivity"
        ));

        try {
            startActivity(intent);
        } catch (Exception e) {
            Log.e("ArtemisLauncher", "Could not open Glyph Toys manager", e);
            Toast.makeText(this, "Glyph Toys manager not found", Toast.LENGTH_SHORT).show();
        }

        // Close the activity immediately as it's just a redirector
        finish();
    }
}
