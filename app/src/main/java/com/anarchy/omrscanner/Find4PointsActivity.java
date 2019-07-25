package com.anarchy.omrscanner;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;

import com.anarchy.omrscanner.Utils.Constants;
import com.anarchy.omrscanner.libraries.NativeClass;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

public class Find4PointsActivity extends AppCompatActivity {

    ImageView imageView;
    Bitmap selectedImageBitmap;
    NativeClass nativeClass;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_4points);

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

        int diffX = 60;
        int diffY = 60;

        Mat origin = new Mat();
        Utils.bitmapToMat(selectedImageBitmap, origin);

        Rect lt = new Rect(new Point(0, 0), new Point(diffX, diffY));
        Rect rt = new Rect(new Point(origin.width() - 1 - diffX, 0), new Point(origin.width() - 1, diffY));
        Rect lb = new Rect(new Point(0, origin.height() - 1 - diffY), new Point(diffX, origin.height() - 1));
        Rect rb = new Rect(new Point(origin.width() - 1 - diffX, origin.height() - 1 - diffY), new Point(origin.width() - 1, origin.height() - 1));

        //// left-top
        Imgproc.rectangle(origin, lt.tl(), lt.br(), new Scalar(0, 255, 0, 255), 1);
        // right-top
        Imgproc.rectangle(origin, rt.tl(), rt.br(), new Scalar(0, 255, 0, 255), 1);
        // left-bottom
        Imgproc.rectangle(origin, lb.tl(), lb.br(), new Scalar(0, 255, 0, 255), 1);
        // right-bottom
        Imgproc.rectangle(origin, rb.tl(), rb.br(), new Scalar(0, 255, 0, 255), 1);

        Utils.matToBitmap(origin, selectedImageBitmap);
        imageView.setImageBitmap(selectedImageBitmap);

    }
}
