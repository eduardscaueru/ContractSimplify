package com.team.contractsimplify;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class Menu extends AppCompatActivity {
    private Button scan_contract;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        // Button Transition to TakePhoto layer
        scan_contract = (Button) findViewById(R.id.scan_button);
        scan_contract.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openTakePhoto();
            }
        });
    }

    public void openTakePhoto() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

}
