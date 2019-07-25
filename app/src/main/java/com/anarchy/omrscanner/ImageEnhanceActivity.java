package com.anarchy.omrscanner;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;

import com.anarchy.omrscanner.Utils.Constants;
import com.anarchy.omrscanner.libraries.NativeClass;

public class ImageEnhanceActivity extends AppCompatActivity {

    ImageView imageView;
    Bitmap selectedImageBitmap;
    NativeClass nativeClass;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_enhance);

        initializeElement();
        initializeImage();
    }

    private void initializeElement() {

        nativeClass = new NativeClass();
        imageView = findViewById(R.id.imageView);
    }

    private void initializeImage() {

        selectedImageBitmap = Constants.selectedImageBitmap;
        Constants.selectedImageBitmap = null;

        imageView.setImageBitmap(selectedImageBitmap);

    }
}
