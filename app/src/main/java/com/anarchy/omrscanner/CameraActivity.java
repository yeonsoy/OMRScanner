package com.anarchy.omrscanner;

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
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import com.anarchy.omrscanner.Utils.Constants;
import com.anarchy.omrscanner.Utils.MathUtils;
import com.anarchy.omrscanner.libraries.CameraPreview;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
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

    protected static final String TAG = "CameraActivity";
    private static final int PERMISSIONS_REQUEST_CODE = 100;
    String[] REQUIRED_PERMISSIONS = {Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private static final int CAMERA_FACING = Camera.CameraInfo.CAMERA_FACING_BACK; // Camera.CameraInfo.CAMERA_FACING_FRONT

    Button GalleryBtn;
    Button CaptureBtn;
    private CameraPreview mCameraPreview;
    private Mat mRgba;
    private View mLayout;
    private List<Point> nowRectPoints;

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
        this.nowRectPoints = new ArrayList<>();
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

            if (nowRectPoints.size() < 4) {
                Toast.makeText(getApplicationContext(), "사각형 영역을 캡처되기 시작한 뒤, 버튼을 눌러주세요", Toast.LENGTH_SHORT).show();
                return;
            }

            Mat align = new Mat();
            MatOfPoint2f srcQuad = new MatOfPoint2f(nowRectPoints.get(2), nowRectPoints.get(3), nowRectPoints.get(0), nowRectPoints.get(1));
            MatOfPoint2f dstQuad = new MatOfPoint2f(new Point(0, 0), new Point(1000, 0), new Point(1000, 773), new Point(0, 773));

            Mat M = Imgproc.getPerspectiveTransform(srcQuad, dstQuad);
            Imgproc.warpPerspective(mRgba, align, M, new Size(1000, 773));

            int diffX = 60;
            int diffY = 60;

            Rect lt = new Rect(new Point(0, 0), new Point(diffX, diffY));
            Rect rt = new Rect(new Point(align.width() - 1 - diffX, 0), new Point(align.width() - 1, diffY));
            Rect lb = new Rect(new Point(0, align.height() - 1 - diffY), new Point(diffX, align.height() - 1));
            Rect rb = new Rect(new Point(align.width() - 1 - diffX, align.height() - 1 - diffY), new Point(align.width() - 1, align.height() - 1));

            Mat ltMat = new Mat(align, lt);
            Mat rtMat = new Mat(align, rt);
            Mat lbMat = new Mat(align, lb);
            Mat rbMat = new Mat(align, rb);

            //// left-top
            //Imgproc.rectangle(align, lt.tl(), lt.br(), new Scalar(0, 255, 0, 255), 1);
            //// right-top
            //Imgproc.rectangle(align, rt.tl(), rt.br(), new Scalar(0, 255, 0, 255), 1);
            //// left-bottom
            //Imgproc.rectangle(align, lb.tl(), lb.br(), new Scalar(0, 255, 0, 255), 1);
            //// right-bottom
            //Imgproc.rectangle(align, rb.tl(), rb.br(), new Scalar(0, 255, 0, 255), 1);

            Point ltPoint = getPoint(ltMat, lt.tl());
            Point rtPoint = getPoint(rtMat, rt.tl());
            Point lbPoint = getPoint(lbMat, lb.tl());
            Point rbPoint = getPoint(rbMat, rb.tl());

            if (ltPoint != null && rtPoint != null && lbPoint != null && rbPoint != null) {
                if (ltPoint != null)
                    Imgproc.circle(align, ltPoint, 5, new Scalar(255, 0, 0), -1);
                if (rtPoint != null)
                    Imgproc.circle(align, rtPoint, 5, new Scalar(255, 0, 0), -1);
                if (lbPoint != null)
                    Imgproc.circle(align, lbPoint, 5, new Scalar(255, 0, 0), -1);
                if (rbPoint != null)
                    Imgproc.circle(align, rbPoint, 5, new Scalar(255, 0, 0), -1);

                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inPreferredConfig = Bitmap.Config.ARGB_8888;

                Bitmap bitmap = Bitmap.createBitmap(align.cols(), align.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(align, bitmap);
                Constants.selectedImageBitmap = bitmap;

                //create new intent to start process image
                Intent intent = new Intent(getApplicationContext(), Find4PointsActivity.class);
                startActivity(intent);
            } else {
                Toast.makeText(getApplicationContext(), "영역에 맞추어 다시 촬영해주세요", Toast.LENGTH_SHORT).show();
                return;
            }
        }
    };

    private View.OnClickListener CaptureBtnClick2 = new View.OnClickListener() {

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
        Mat drawing = mRgba.clone();
        getOMRRect(drawing);
        //drawing.release();

        return drawing;
    }

    public void getOMRRect(Mat img) {
        Mat img_gray = new Mat();
        Mat img_edges = new Mat();

        Imgproc.cvtColor(img, img_gray, Imgproc.COLOR_BGR2GRAY);
        Imgproc.Canny(img_gray, img_edges, 80, 100);
        Mat element = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(2 + 1, 2 + 1), new Point(1, 1));
        Imgproc.dilate(img_edges, img_edges, element);
        // Imgproc.erode(img_edges, img_edges, element);

        //Imgproc.cvtColor(img, img_gray, Imgproc.COLOR_BGR2Lab);
        //Core.inRange(img_gray, new Scalar(150, 100, 100), new Scalar(10000, 160, 160), img_gray);
        //Imgproc.Canny(img_gray, img_edges, 80, 100);
        //Mat element = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(2 + 1, 2 + 1), new Point(1, 1));
        //Imgproc.dilate(img_edges, img_edges, element);

        // mRgba = img_gray.clone();
        //Shape detection
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(img_edges, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

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
            if (Imgproc.contourArea(temp) > 210000 && Imgproc.contourArea(temp) < 300000)
                hulls.add(temp);
        }

        List<MatOfPoint2f> hull2f = new ArrayList<MatOfPoint2f>();
        for (int i = 0; i < hulls.size(); i++) {
            MatOfPoint2f newPoint = new MatOfPoint2f(hulls.get(i).toArray());
            hull2f.add(newPoint);
        }

        for (int i = 0; i < hulls.size(); i++) {
            //Approximate polygon
            nowRectPoints.clear();
            MatOfPoint2f approx = new MatOfPoint2f();
            Imgproc.approxPolyDP(hull2f.get(i), approx, 0.01 * Imgproc.arcLength(hull2f.get(i), true), true);
            List<Point> approx_polygon = approx.toList();
            approx_polygon = filterPolygon(approx_polygon);
            if (isRect(approx_polygon)) {
                double area = Imgproc.contourArea(approx);
                Log.d(TAG, "getPoint: " + area);
                drawShape(img, nowRectPoints, new Scalar(0, 255, 0), 4);
                test(img);

            } else
                continue;
        }

        img_gray.release();
        img_edges.release();
        element.release();
        hierarchy.release();
        contours.clear();
        hulls.clear();
        hull2f.clear();
    }

    public void test(Mat img) {
        Mat align = img;
        MatOfPoint2f srcQuad = new MatOfPoint2f(nowRectPoints.get(2), nowRectPoints.get(3), nowRectPoints.get(0), nowRectPoints.get(1));
        MatOfPoint2f dstQuad = new MatOfPoint2f(new Point(0, 0), new Point(1000, 0), new Point(1000, 773), new Point(0, 773));

        Mat M = Imgproc.getPerspectiveTransform(srcQuad, dstQuad);
        Imgproc.warpPerspective(mRgba, align, M, new Size(1000, 773));

        int diffX = 60;
        int diffY = 60;

        Rect lt = new Rect(new Point(0, 0), new Point(diffX, diffY));
        Rect rt = new Rect(new Point(align.width() - 1 - diffX, 0), new Point(align.width() - 1, diffY));
        Rect lb = new Rect(new Point(0, align.height() - 1 - diffY), new Point(diffX, align.height() - 1));
        Rect rb = new Rect(new Point(align.width() - 1 - diffX, align.height() - 1 - diffY), new Point(align.width() - 1, align.height() - 1));

        Mat ltMat = new Mat(align, lt);
        Mat rtMat = new Mat(align, rt);
        Mat lbMat = new Mat(align, lb);
        Mat rbMat = new Mat(align, rb);

        //// left-top
        //Imgproc.rectangle(align, lt.tl(), lt.br(), new Scalar(0, 255, 0, 255), 1);
        //// right-top
        //Imgproc.rectangle(align, rt.tl(), rt.br(), new Scalar(0, 255, 0, 255), 1);
        //// left-bottom
        //Imgproc.rectangle(align, lb.tl(), lb.br(), new Scalar(0, 255, 0, 255), 1);
        //// right-bottom
        //Imgproc.rectangle(align, rb.tl(), rb.br(), new Scalar(0, 255, 0, 255), 1);

        Point ltPoint = getPoint(ltMat, lt.tl());
        Point rtPoint = getPoint(rtMat, rt.tl());
        Point lbPoint = getPoint(lbMat, lb.tl());
        Point rbPoint = getPoint(rbMat, rb.tl());

        if (ltPoint != null)
            Imgproc.circle(align, ltPoint, 5, new Scalar(255, 0, 0), -1);
        if (rtPoint != null)
            Imgproc.circle(align, rtPoint, 5, new Scalar(255, 0, 0), -1);
        if (lbPoint != null)
            Imgproc.circle(align, lbPoint, 5, new Scalar(255, 0, 0), -1);
        if (rbPoint != null)
            Imgproc.circle(align, rbPoint, 5, new Scalar(255, 0, 0), -1);

       // align.release();
        ltMat.release();
        rtMat.release();
        lbMat.release();
        rbMat.release();
    }

    public void drawShape(Mat img, Point tl, List<Point> shape, Scalar color, int thickness) {
        //Display filtered polygons
        for (int k = 0; k < shape.size() - 1; k++) {
            Imgproc.line(img, plusPoints(shape.get(k), tl), plusPoints(shape.get(k + 1), tl), color, thickness);
            Imgproc.circle(img, plusPoints(shape.get(k), tl), 5, color, thickness);
        }
        Imgproc.line(img, plusPoints(shape.get(0), tl), plusPoints(shape.get(shape.size() - 1), tl), color, thickness);
        Imgproc.circle(img, plusPoints(shape.get(shape.size() - 1), tl), 5, color, thickness);
    }

    public void drawShape(Mat img, List<Point> shape, Scalar color, int thickness) {
        //Display filtered polygons
        for (int k = 0; k < shape.size() - 1; k++) {
            Imgproc.line(img, shape.get(k), shape.get(k + 1), color, thickness);
            // Imgproc.circle(img, shape.get(k), 5, color, thickness);
        }
        Imgproc.line(img, shape.get(0), shape.get(shape.size() - 1), color, thickness);
        //Imgproc.circle(img,  shape.get(shape.size() - 1), 5, color, thickness);
    }

    public Point plusPoints(Point a, Point b) {
        return new Point(a.x + b.x, a.y + b.y);
    }

    public Point getPoint(Mat areaMat, Point tl) {
        Mat img_edges = new Mat();

        //Imgproc.cvtColor(areaMat, areaMat, Imgproc.COLOR_BGR2GRAY, 1);
        //Imgproc.Canny(areaMat, img_edges, 80, 100);
        //Mat element = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(2 + 1, 2 + 1), new Point(1, 1));
        //Imgproc.dilate(img_edges, img_edges, element);

        Imgproc.cvtColor(areaMat, areaMat, Imgproc.COLOR_BGR2Lab);
        Core.inRange(areaMat, new Scalar(150, 100, 100), new Scalar(10000, 160, 160), areaMat);
        Imgproc.Canny(areaMat, img_edges, 80, 100);
        Mat element = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(2 + 1, 2 + 1), new Point(1, 1));
        Imgproc.dilate(img_edges, img_edges, element);

        //Shape detection
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(img_edges, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE, new Point(0, 0));

        List<MatOfPoint> hulls = new ArrayList<>();

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

        List<MatOfPoint2f> hull2f = new ArrayList<>();
        for (int i = 0; i < hulls.size(); i++) {
            MatOfPoint2f newPoint = new MatOfPoint2f(hulls.get(i).toArray());
            hull2f.add(newPoint);
        }

        for (int i = 0; i < hulls.size(); i++) {
            //Approximate polygon
            MatOfPoint2f approx = new MatOfPoint2f();
            Imgproc.approxPolyDP(hull2f.get(i), approx, 0.01 * Imgproc.arcLength(hull2f.get(i), true), true);
            List<Point> approx_polygon = approx.toList();
            approx_polygon = filterPolygon(approx_polygon);
            double area = Imgproc.contourArea(approx);
            Log.d(TAG, "getPoint: " + area);
            if (area > 70 && area < 200) {
                //Center of mass
                int cx = 0,
                        cy = 0;
                for (int k = 0; k < approx_polygon.size(); k++) {
                    cx += (int) approx_polygon.get(k).x;
                    cy += (int) approx_polygon.get(k).y;
                }
                cx /= approx_polygon.size();
                cy /= approx_polygon.size();
                return plusPoints(tl, new Point(cx, cy));
            }
        }

        img_edges.release();
        element.release();
        hierarchy.release();
        contours.clear();
        hulls.clear();
        hull2f.clear();

        return null;
    }

    public List<Point> filterPolygon(List<Point> approx_polygon) {
        List<Point> approxList = new ArrayList<>();
        approxList.addAll(approx_polygon);
        while (true) {
            double max_ar = 0;
            int max_ar_id = 0;
            for (int k = 0; k < approxList.size(); k++) {
                List<Point> cur_polygon = new ArrayList<>();
                cur_polygon.addAll(approxList);
                cur_polygon.remove(cur_polygon.get(k));

                MatOfPoint cur_area = new MatOfPoint();
                MatOfPoint approx_area = new MatOfPoint();
                cur_area.fromList(cur_polygon);
                approx_area.fromList(approxList);

                double area_ratio = Imgproc.contourArea(cur_area) / Imgproc.contourArea(approx_area);

                if (area_ratio > max_ar) {
                    max_ar = area_ratio;
                    max_ar_id = k;
                }
            }

            //If area still large enough remove a vertex
            if (max_ar > 0.8) {
                //cout << "Remove vertex  " << max_ar_id << endl;
                approxList.remove(approxList.get(max_ar_id));
            } else
                break;
        }

        return approxList;
    }

    public static double distanceTwoPoints(Point left, Point right) {
        return Math.sqrt((float) ((left.x - right.x) * (left.x - right.x) + (left.y - right.y) * (left.y - right.y)));
    }

    public static double angleThreePoints(Point left, Point p_base, Point right) {
        //Calculate distances between points
        double a = distanceTwoPoints(left, p_base);
        double b = distanceTwoPoints(right, p_base);
        double c = distanceTwoPoints(left, right);

        //Calculate angle in degrees (0...180)
        double theta = Math.acos((float) ((a * a + b * b - c * c) / (2.0 * a * b)));
        double angle = theta / Math.PI * 180;

        return angle;
    }

    public boolean isRect(List<Point> shape) {
        double[] length = new double[4], angle = new double[4];

        if (shape.size() != 4)
            return false;

        MatOfPoint shape_area = new MatOfPoint();
        shape_area.fromList(shape);

        //Calculate side lengths
        length[0] = distanceTwoPoints(shape.get(0), shape.get(1));
        length[1] = distanceTwoPoints(shape.get(1), shape.get(2));
        length[2] = distanceTwoPoints(shape.get(2), shape.get(3));
        length[3] = distanceTwoPoints(shape.get(3), shape.get(0));

        //Calculate angles
        angle[0] = angleThreePoints(shape.get(0), shape.get(1), shape.get(2));
        angle[1] = angleThreePoints(shape.get(1), shape.get(2), shape.get(3));
        angle[2] = angleThreePoints(shape.get(2), shape.get(3), shape.get(0));
        angle[3] = angleThreePoints(shape.get(3), shape.get(0), shape.get(1));

        double maxCosine = 0;
        for (int i = 2; i < 5; i++) {
            double cosine = Math.abs(MathUtils.angle(shape.get(i % 4), shape.get(i - 2), shape.get(i - 1)));
            maxCosine = Math.max(cosine, maxCosine);
        }

        if (maxCosine >= 0.3) {
            return false;
        }

        nowRectPoints.addAll(shape);
        return true;
    }
}
