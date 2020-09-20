package com.yeon.omrscanner;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import com.yeon.omrscanner.Utils.Constants;
import com.yeon.omrscanner.libraries.CameraPreview;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class CameraActivity extends AppCompatActivity implements CvCameraViewListener2 {

    private static final int PERMISSIONS_REQUEST_CODE = 100;
    String[] REQUIRED_PERMISSIONS = {Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private static final int CAMERA_FACING = Camera.CameraInfo.CAMERA_FACING_BACK; // Camera.CameraInfo.CAMERA_FACING_FRONT

    Button GalleryBtn;
    Button CaptureBtn;
    private CameraPreview mCameraPreview;
    private Mat mRgba;
    private View mLayout;

    Uri selectedImage;
    Bitmap selectedBitmap;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    mCameraPreview.enableView();
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_camera);
        initializeElement();
        initializeEvent();
    }

    private void initializeElement() {
        this.mLayout = findViewById(R.id.layout_main);
        this.mCameraPreview = findViewById(R.id.camera_preview_main);
        this.mCameraPreview.setVisibility(View.VISIBLE);
        this.mCameraPreview.setCvCameraViewListener(this);
        this.GalleryBtn = findViewById(R.id.GalleryBtn);
        this.CaptureBtn = findViewById(R.id.CaptureBtn);
    }

    private void initializeEvent() {
        this.CaptureBtn.setOnClickListener(this.CaptureBtnClick);
        this.GalleryBtn.setOnClickListener(this.GalleryBtnClick);

        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {

            int cameraPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
            int writeExternalStoragePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);


            if (cameraPermission == PackageManager.PERMISSION_GRANTED
                    && writeExternalStoragePermission == PackageManager.PERMISSION_GRANTED) {
                mCameraPreview.enableView();
            } else {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[0])
                        || ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[1])) {

                    Snackbar.make(mLayout, "이 앱을 실행하려면 카메라와 외부 저장소 접근 권한이 필요합니다.",
                            Snackbar.LENGTH_INDEFINITE).setAction("확인", new View.OnClickListener() {

                        @Override
                        public void onClick(View view) {

                            ActivityCompat.requestPermissions(CameraActivity.this, REQUIRED_PERMISSIONS,
                                    PERMISSIONS_REQUEST_CODE);
                        }
                    }).show();
                } else {
                    // 2. 사용자가 퍼미션 거부를 한 적이 없는 경우에는 퍼미션 요청을 바로 합니다.
                    // 요청 결과는 onRequestPermissionResult에서 수신됩니다.
                    ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS,
                            PERMISSIONS_REQUEST_CODE);
                }
            }
        } else {

            final Snackbar snackbar = Snackbar.make(mLayout, "디바이스가 카메라를 지원하지 않습니다.",
                    Snackbar.LENGTH_INDEFINITE);
            snackbar.setAction("확인", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    snackbar.dismiss();
                }
            });
            snackbar.show();
        }
    }

    private View.OnClickListener GalleryBtnClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            startActivityForResult(intent, Constants.GALLERY_IMAGE_LOADED);
        }
    };

    private View.OnClickListener CaptureBtnClick = new View.OnClickListener() {

        @SuppressLint("SimpleDateFormat")
        @Override
        public void onClick(View v) {

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
            String currentDateandTime = sdf.format(new Date());
            String fileName = currentDateandTime + ".jpg";
            File path = new File(Environment.getExternalStorageDirectory().getAbsolutePath() +
                    "/OMRScanner");
            if (!path.exists()) path.mkdirs();

            mCameraPreview.takePicture(path, fileName);
            //   Toast.makeText(getApplicationContext(), path + " saved", Toast.LENGTH_SHORT).show();
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grandResults) {
        if (requestCode == PERMISSIONS_REQUEST_CODE && grandResults.length == REQUIRED_PERMISSIONS.length) {
            boolean check_result = true;

            for (int result : grandResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    check_result = false;
                    break;
                }
            }
            if (check_result) {
                mCameraPreview.enableView();
            } else {

                if (ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[0])
                        || ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[1])) {

                    Snackbar.make(mLayout, "퍼미션이 거부되었습니다. 앱을 다시 실행하여 퍼미션을 허용해주세요. ",
                            Snackbar.LENGTH_INDEFINITE).setAction("확인", new View.OnClickListener() {

                        @Override
                        public void onClick(View view) {

                            finish();
                        }
                    }).show();

                } else {
                    Snackbar.make(mLayout, "설정(앱 정보)에서 퍼미션을 허용해야 합니다. ",
                            Snackbar.LENGTH_INDEFINITE).setAction("확인", new View.OnClickListener() {

                        @Override
                        public void onClick(View view) {

                            finish();
                        }
                    }).show();
                }
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mCameraPreview != null)
            mCameraPreview.disableView();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            // Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
            Camera.Size resolution = mCameraPreview.getResolutionList().get(0);
            mCameraPreview.setResolution(resolution);
        } else {
            //  Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (mCameraPreview != null)
            mCameraPreview.disableView();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constants.GALLERY_IMAGE_LOADED && resultCode == RESULT_OK && data != null) {
            selectedImage = data.getData();
            this.loadImage();

            Constants.selectedImageBitmap = selectedBitmap;

            //create new intent to start process image
            Intent intent = new Intent(getApplicationContext(), ImageCropActivity.class);
            startActivity(intent);
        }
    }

    private void loadImage() {
        try {
            InputStream inputStream = getContentResolver().openInputStream(this.selectedImage);
            selectedBitmap = BitmapFactory.decodeStream(inputStream);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void onCameraViewStarted(int width, int height) {

    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();
        int diffX = 50;
        int diffY = 50;
        int widthRatio = 900;
        int heightRatio = 750;
        float resultWidth = ((float) mRgba.height() / heightRatio) * widthRatio;
        float resultHeight = mRgba.height();

        // main area
        // Imgproc.rectangle(mRgba, new Point( (mRgba.width() - resultWidth) / 2, 10), new Point(mRgba.width() - 1 - (mRgba.width() - resultWidth) / 2, mRgba.height() - 1 - 10), new Scalar(0, 255, 0, 255), 1);


      // Rect lt = new Rect(new Point((mRgba.width() - resultWidth) / 2, 0), new Point((mRgba.width() - resultWidth) / 2 + diffX, diffY));
      // Rect rt = new Rect(new Point(mRgba.width() - 1 - (mRgba.width() - resultWidth) / 2 - diffX, 0), new Point(mRgba.width() - 1 - (mRgba.width() - resultWidth) / 2, diffY));
      // Rect lb = new Rect(new Point((mRgba.width() - resultWidth) / 2, mRgba.height() - 1 - diffY), new Point((mRgba.width() - resultWidth) / 2 + diffX, mRgba.height() - 1));
      // Rect rb = new Rect(new Point(mRgba.width() - 1 - (mRgba.width() - resultWidth) / 2 - diffX, mRgba.height() - 1 - diffY), new Point(mRgba.width() - 1 - (mRgba.width() - resultWidth) / 2, mRgba.height() - 1));

      // Mat ltMat = new Mat(mRgba, lt);
      // Mat rtMat = new Mat(mRgba, rt);
      // Mat lbMat = new Mat(mRgba, lb);
      // Mat rbMat = new Mat(mRgba, rb);

      // getPoint(ltMat, mRgba, lt.tl());
      // getPoint(rtMat, mRgba, rt.tl());
      // getPoint(lbMat, mRgba, lb.tl());
      // getPoint(rbMat, mRgba, rb.tl());

      // //// left-top
      // Imgproc.rectangle(mRgba, lt.tl(), lt.br(), new Scalar(0, 255, 0, 255), 1);
      // // right-top
      // Imgproc.rectangle(mRgba, rt.tl(), rt.br(), new Scalar(0, 255, 0, 255), 1);
      // // left-bottom
      // Imgproc.rectangle(mRgba, lb.tl(), lb.br(), new Scalar(0, 255, 0, 255), 1);
      // // right-bottom
      // Imgproc.rectangle(mRgba, rb.tl(), rb.br(), new Scalar(0, 255, 0, 255), 1);


        return mRgba;
    }

    public void getR(Mat areaMat, Mat drawing, Point tl)
    {
        Mat img_edges = new Mat();
        Imgproc.cvtColor(areaMat, areaMat, Imgproc.COLOR_BGR2GRAY, 1);
        Imgproc.Canny(areaMat, img_edges, 80, 100);

        Mat element = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(2 + 1, 2 + 1), new Point(1, 1));
        Imgproc.dilate(img_edges, img_edges, element);

        //Shape detection
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(img_edges, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE, new Point(0, 0));

        List<MatOfPoint> hulls = new ArrayList<MatOfPoint>();

        for (int i = 0; i < contours.size(); i++) {
            MatOfInt hull_temp = new MatOfInt();
            Imgproc.convexHull(contours.get(i), hull_temp);
            int[] arrIndex = hull_temp.toArray();
            Point[] arrContour = contours.get(i).toArray();
            Point[] arrPoints = new Point[arrIndex.length];

            for (int k = 0; k < arrIndex.length; k++)
                arrPoints[k] = arrContour[arrIndex[k]];

            MatOfPoint temp = new MatOfPoint();
            temp.fromArray(arrPoints);

            //Filter outliers
            if (Imgproc.contourArea(temp) > 30 && Imgproc.contourArea(temp) < 200)
                hulls.add(temp);
        }

        List<MatOfPoint2f> hull2f = new ArrayList<MatOfPoint2f>();
        for (int i = 0; i < hulls.size(); i++) {
            MatOfPoint2f newPoint = new MatOfPoint2f(hulls.get(i).toArray());
            hull2f.add(newPoint);
        }

        for (int i = 0; i < hulls.size(); i++) {
            //Approximate polygon
            MatOfPoint2f approx = new MatOfPoint2f();
            Imgproc.approxPolyDP(hull2f.get(i), approx, 0.01 * Imgproc.arcLength(hull2f.get(i), true), true);
            List<Point> approx_polygon = approx.toList();

            double area = Imgproc.contourArea(approx);
            //Log.d(TAG, "getPoint: " + area);
            if (area > 70 && area < 100) {
                drawShape(drawing, tl, approx_polygon, new Scalar(0, 255, 0), 1);

                //Center of mass
                int cx = 0,
                        cy = 0;
                for (int k = 0; k < approx_polygon.size(); k++)
                {
                    cx += (int)approx_polygon.get(k).x;
                    cy += (int)approx_polygon.get(k).y;
                }
                cx /= approx_polygon.size();
                cy /= approx_polygon.size();
                Imgproc.circle(drawing, plusPoints(tl, new Point(cx, cy)), 5, new Scalar(255, 0, 0), -1);
            }
        }
    }

    public void getPoint(Mat areaMat, Mat drawing, Point tl)
    {
        Mat img_edges = new Mat();
        Imgproc.cvtColor(areaMat, areaMat, Imgproc.COLOR_BGR2GRAY, 1);
        Imgproc.Canny(areaMat, img_edges, 80, 100);

        Mat element = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(2 + 1, 2 + 1), new Point(1, 1));
        Imgproc.dilate(img_edges, img_edges, element);

        //Shape detection
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(img_edges, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE, new Point(0, 0));

        List<MatOfPoint> hulls = new ArrayList<MatOfPoint>();

        for (int i = 0; i < contours.size(); i++) {
            MatOfInt hull_temp = new MatOfInt();
            Imgproc.convexHull(contours.get(i), hull_temp);
            int[] arrIndex = hull_temp.toArray();
            Point[] arrContour = contours.get(i).toArray();
            Point[] arrPoints = new Point[arrIndex.length];

            for (int k = 0; k < arrIndex.length; k++)
                arrPoints[k] = arrContour[arrIndex[k]];

            MatOfPoint temp = new MatOfPoint();
            temp.fromArray(arrPoints);

            //Filter outliers
            if (Imgproc.contourArea(temp) > 30 && Imgproc.contourArea(temp) < 200)
                hulls.add(temp);
        }

        List<MatOfPoint2f> hull2f = new ArrayList<MatOfPoint2f>();
        for (int i = 0; i < hulls.size(); i++) {
            MatOfPoint2f newPoint = new MatOfPoint2f(hulls.get(i).toArray());
            hull2f.add(newPoint);
        }

        for (int i = 0; i < hulls.size(); i++) {
            //Approximate polygon
            MatOfPoint2f approx = new MatOfPoint2f();
            Imgproc.approxPolyDP(hull2f.get(i), approx, 0.01 * Imgproc.arcLength(hull2f.get(i), true), true);
            List<Point> approx_polygon = approx.toList();

            double area = Imgproc.contourArea(approx);
          //Log.d(TAG, "getPoint: " + area);
            if (area > 70 && area < 100) {
                drawShape(drawing, tl, approx_polygon, new Scalar(0, 255, 0), 1);

                //Center of mass
                int cx = 0,
                        cy = 0;
                for (int k = 0; k < approx_polygon.size(); k++)
                {
                    cx += (int)approx_polygon.get(k).x;
                    cy += (int)approx_polygon.get(k).y;
                }
                cx /= approx_polygon.size();
                cy /= approx_polygon.size();
                Imgproc.circle(drawing, plusPoints(tl, new Point(cx, cy)), 5, new Scalar(255, 0, 0), -1);
            }
        }
    }

    public void drawShape(Mat img, Point tl, List<Point> shape, Scalar color, int thickness) {
        //Display filtered polygons
        for (int k = 0; k < shape.size() - 1; k++) {
            Imgproc.line(img, plusPoints(shape.get(k), tl), plusPoints(shape.get(k + 1), tl), color, thickness);
            Imgproc.circle(img, plusPoints(shape.get(k), tl), 5, color, thickness);
        }
        Imgproc.line(img, plusPoints(shape.get(0), tl), plusPoints(shape.get(shape.size() - 1), tl), color, thickness);
        Imgproc.circle(img,  plusPoints(shape.get(shape.size() - 1), tl), 5, color, thickness);
    }

    public Point plusPoints(Point a, Point b) {
        return new Point(a.x + b.x, a.y + b.y);
    }
}
