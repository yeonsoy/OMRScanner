package com.anarchy.omrscanner;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;

public class MainActivity extends AppCompatActivity {

    Button CameraBtn;
    ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);
        initializeElement();
        initializeEvent();
    }

    private void initializeElement() {
        this.imageView = findViewById(R.id.imageView);
        this.CameraBtn = findViewById(R.id.CameraBtn);
    }

    private void initializeEvent() {
        this.CameraBtn.setOnClickListener(this.CameraBtnClick);
    }

    private View.OnClickListener CameraBtnClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(getApplicationContext(), CameraActivity.class);
            startActivity(intent);
        }
    };

}
