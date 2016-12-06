package com.app.mxvideoplayer;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

public class AutoTinyWindowActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auto_tiny);
        Button autoList = (Button) findViewById(R.id.button_auto_list);
        autoList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AutoTinyWindowActivity.this, ListAutoActivity.class);
                startActivity(intent);
            }
        });


        Button autoInsertList = (Button) findViewById(R.id.button_insert_list);
        autoInsertList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AutoTinyWindowActivity.this, ListAutoInsertActivity.class);
                startActivity(intent);
            }
        });
    }

}
