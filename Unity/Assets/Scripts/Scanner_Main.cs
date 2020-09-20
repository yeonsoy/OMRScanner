using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;
using OpenCVForUnityExample;
using Rect = OpenCVForUnity.Rect;
using System.IO;

#if UNITY_5_3 || UNITY_5_3_OR_NEWER
using UnityEngine.SceneManagement;
#endif
using OpenCVForUnity;
using ScannerGeomatry;


[RequireComponent(typeof(WebCamTextureToMatHelper))]
public class Scanner_Main : MonoBehaviour
{


    WebCamTextureToMatHelper webCamTextureToMatHelper;
    Mat frame, img_orig, img_gray, img_lab, img_edges, img_segmented, drawing, imgLab;
    Texture2D texture;
    public GameObject result;
    public Texture2D result_texture;
    public bool showTextureOnScreen = false;
    public bool nowDetected = true;

    public ALIGNMENT alignment;
    public List<Point> nowRectPoints, dstRectPoints;

    void Start()
    {
        webCamTextureToMatHelper = gameObject.GetComponent<WebCamTextureToMatHelper>();
        webCamTextureToMatHelper.Initialize();

        frame = new Mat();
        img_orig = new Mat();
        img_lab = new Mat();
        img_gray = new Mat();
        img_edges = new Mat();
        img_segmented = new Mat();
        drawing = new Mat();
        imgLab = new Mat();

        // alignment = ALIGNMENT.DRAWING;
        nowRectPoints = new List<Point>();
        dstRectPoints = new List<Point>();
        dstRectPoints.Add(new Point(0, 0));
        dstRectPoints.Add(new Point(1120, 0));
        dstRectPoints.Add(new Point(1120, 860));
        dstRectPoints.Add(new Point(0, 860));


        if (showTextureOnScreen)
            gameObject.GetComponent<Renderer>().enabled = true;
        else
            gameObject.GetComponent<Renderer>().enabled = false;
    }

    // top-left = 0; top-right = 1; 
    // right-bottom = 2; left-bottom = 3;
    List<Point> orderRectCorners(List<Point> corners)
    {
        if (corners.Count == 4)
        {
            nowRectPoints = orderPointsByRows(corners);

            if (nowRectPoints[0].x > nowRectPoints[1].x)
            { // swap points
                Point tmp = nowRectPoints[0];
                nowRectPoints[0] = nowRectPoints[1];
                nowRectPoints[1] = tmp;
            }

            if (nowRectPoints[2].x < nowRectPoints[3].x)
            { // swap points
                Point tmp = nowRectPoints[2];
                nowRectPoints[2] = nowRectPoints[3];
                nowRectPoints[3] = tmp;
            }
            return nowRectPoints;
        }
        return null;
    }

    List<Point> orderPointsByRows(List<Point> points)
    {
        Comp comparer = new Comp();
        points.Sort(comparer);

        return points;
    }

    public class Comp : IComparer<Point>
    {
        public int Compare(Point x, Point y)
        {
            if (x.y < y.y) return -1;
            if (x.y > y.y) return 1;
            return 0;
        }
    }

// Update is called once per frame
void Update()
    {
        if (webCamTextureToMatHelper.IsPlaying() && webCamTextureToMatHelper.DidUpdateThisFrame())
        {
            frame = webCamTextureToMatHelper.GetMat();
            frame.copyTo(img_orig);

            drawing = img_orig.clone();

            int lowThreshold = 50;// (int)200;// slider.value;
            const int ratio = 1;
            const int kernel_size = 3;

            Imgproc.cvtColor(img_orig, img_lab, Imgproc.COLOR_BGR2Lab);
            double omrSize = img_orig.cols() * img_orig.rows();

            Imgproc.cvtColor(img_orig, img_gray, Imgproc.COLOR_RGBA2GRAY);
            Imgproc.GaussianBlur(img_gray, img_gray, new Size(15, 15), 1.5, 1.5);       //Gaussian blur
            Imgproc.erode(img_gray, img_gray, new Mat(), new Point(-1, -1), 1);                //Erosion
                                                                                               // Imgproc.dilate(img_gray, img_gray, new Mat(), new Point(-1, -1), 10, 1, new Scalar(10));    //Dilation
            Imgproc.Canny(img_gray, img_edges, lowThreshold, lowThreshold * ratio, kernel_size, false);

            //Shape detection
            List<MatOfPoint> contours = new List<MatOfPoint>();
            Mat hierarchy = new Mat();
            Imgproc.findContours(img_edges, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE, new Point(0, 0));

        //Texture2D tex = new Texture2D(img_edges.width(), img_edges.height(), TextureFormat.RGB24, false);
        //Utils.matToTexture2D(img_edges, tex);
        //byte[] bytes1 = tex.EncodeToJPG();
        //File.WriteAllBytes("D:/2019/OMR/" + "test213123.png", bytes1);

            List<MatOfPoint> hulls = new List<MatOfPoint>();

            for (int i = 0; i < contours.Count; i++)
            {
                MatOfInt hull_temp = new MatOfInt();
                Imgproc.convexHull(contours[i], hull_temp);
                int[] arrIndex = hull_temp.toArray();
                Point[] arrContour = contours[i].toArray();
                Point[] arrPoints = new Point[arrIndex.Length];

                for (int k = 0; k < arrIndex.Length; k++)
                    arrPoints[k] = arrContour[arrIndex[k]];

                MatOfPoint temp = new MatOfPoint();
                temp.fromArray(arrPoints);

                //Filter outliers
                if (Imgproc.contourArea(temp) > omrSize / 3 && Imgproc.contourArea(temp) < (omrSize * 4) / 5)
                    hulls.Add(temp);
            }

            List<MatOfPoint2f> hull2f = new List<MatOfPoint2f>();
            for (int i = 0; i < hulls.Count; i++)
            {
                MatOfPoint2f newPoint = new MatOfPoint2f(hulls[i].toArray());
                hull2f.Add(newPoint);
            }

            for (int i = 0; i < hulls.Count; i++)
            {
                //Approximate polygon
                MatOfPoint2f approx = new MatOfPoint2f();

                Imgproc.approxPolyDP(hull2f[i], approx, 0.01 * Imgproc.arcLength(hull2f[i], true), true);
                List<Point> approx_polygon = approx.toList();
                // approx_polygon = Scannerproc.filterPolygon(approx_polygon);
                // Debug.Log(approx_polygon.Count);
                if (!Scannerproc.isSquare(approx_polygon))
                    continue;
                else
                {
                    nowRectPoints.Clear();
                    nowRectPoints.AddRange(approx_polygon);
                    perspectiveAlign();
                }

                //Center of mass
                int cx = 0,
                    cy = 0;


                for (int k = 0; k < approx_polygon.Count; k++)
                {
                    cx += (int)approx_polygon[k].x;
                    cy += (int)approx_polygon[k].y;
                }
                cx /= approx_polygon.Count;
                cy /= approx_polygon.Count;

                Scannerproc.drawShape(drawing, approx_polygon, new Scalar(0, 255, 0));
            }

            if (showTextureOnScreen)
                showCurrentTextureOnScreen();
        }
    }

    public void perspectiveAlign()
    {
        if (nowDetected)
        {
            Mat align = new Mat();
            orderRectCorners(nowRectPoints);
            Mat srcQuad = Converters.vector_Point_to_Mat(nowRectPoints, CvType.CV_32F);
            Mat dstQuad = Converters.vector_Point_to_Mat(dstRectPoints, CvType.CV_32F);

            Mat M = Imgproc.getPerspectiveTransform(srcQuad, dstQuad);
            Imgproc.warpPerspective(img_orig, align, M, new Size(1120, 860));

            int diffX = 60;
            int diffY = 60;

            Rect lt = new Rect(new Point(0, 0), new Point(diffX, diffY));
            Rect rt = new Rect(new Point(align.width() - 1 - diffX, 0), new Point(align.width() - 1, diffY));
            Rect lb = new Rect(new Point(0, align.height() - 1 - diffY), new Point(diffX, align.height() - 1));
            Rect rb = new Rect(new Point(align.width() - 1 - diffX, align.height() - 1 - diffY), new Point(align.width() - 1, align.height() - 1));

            // left-top
            Imgproc.rectangle(align, lt.tl(), lt.br(), new Scalar(0, 255, 0, 255), 1);
            // right-top
            Imgproc.rectangle(align, rt.tl(), rt.br(), new Scalar(0, 255, 0, 255), 1);
            // left-bottom
            Imgproc.rectangle(align, lb.tl(), lb.br(), new Scalar(0, 255, 0, 255), 1);
            // right-bottom
            Imgproc.rectangle(align, rb.tl(), rb.br(), new Scalar(0, 255, 0, 255), 1);

            //for (int i = 0; i < 20; i++)
            //{
            //    Rect r = new Rect(new Point(435, 137.5 + 32.5 * i), new Point(435 + 110, 170 + 32.5 * i));
            //    int num = getAnswerNumber(align, r);
            //    Imgproc.putText(align, " " + num, new Point(r.x - 40, r.y + 25), 1, 2, new Scalar(255, 0, 0, 255), 3, Core.LINE_AA, false);
            //    Imgproc.rectangle(align, r.tl(), r.br(), new Scalar(0, 255, 0, 255), 2);
            //}
            //
            //for (int i = 0; i < 20; i++)
            //{
            //    Rect r = new Rect(new Point(590, 137.5 + 32.5 * i), new Point(590 + 110, 170 + 32.5 * i));
            //    int num = getAnswerNumber(align, r);
            //    Imgproc.putText(align, " " + num, new Point(r.x - 40, r.y + 25), 1, 2, new Scalar(255, 0, 0, 255), 3, Core.LINE_AA, false);
            //    Imgproc.rectangle(align, r.tl(), r.br(), new Scalar(0, 255, 0, 255), 2);
            //}
            //
            //for (int i = 0; i < 5; i++)
            //{
            //    Rect r = new Rect(new Point(750, 137.5 + 32.5 * i), new Point(750 + 110, 170 + 32.5 * i));
            //    int num = getAnswerNumber(align, r);
            //    Imgproc.putText(align, " " + num, new Point(r.x - 40, r.y + 25), 1, 2, new Scalar(255, 0, 0, 255), 3, Core.LINE_AA, false);
            //    Imgproc.rectangle(align, r.tl(), r.br(), new Scalar(0, 255, 0, 255), 2);
            //}

            getAnswerNumber(align);
            result.GetComponent<Renderer>().material.mainTexture = result_texture;
            result_texture.Resize(align.width(), align.height());
            result.gameObject.transform.localScale = new Vector3(align.width() / 2.5f, align.height() / 2.5f, 3);
            Utils.matToTexture2D(align, result_texture);
        }
    }
    public void getAnswerNumber(Mat align)
    {
        Mat align_gray = new Mat(), align_edges = new Mat();
        Imgproc.cvtColor(align, align_gray, Imgproc.COLOR_RGB2GRAY);
        Imgproc.Canny(align_gray, align_edges, 50, 50);
        Mat element = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(2 + 1, 2 + 1), new Point(1, 1));
        Imgproc.dilate(align_edges, align_edges, element);


        //Shape detection
        List<MatOfPoint> contours = new List<MatOfPoint>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(align_edges, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE, new Point(0, 0));

        List<MatOfPoint> hulls = new List<MatOfPoint>();

       //Texture2D tex = new Texture2D(align_edges.width(), align_edges.height(), TextureFormat.RGB24, false);
       //Utils.matToTexture2D(align_edges, tex);
       //byte[] bytes1 = tex.EncodeToJPG();
       //File.WriteAllBytes("D:/2019/OMR/" + "test.png", bytes1);

        for (int i = 0; i < contours.Count; i++)
        {
            MatOfInt hull_temp = new MatOfInt();
            Imgproc.convexHull(contours[i], hull_temp);
            int[] arrIndex = hull_temp.toArray();
            Point[] arrContour = contours[i].toArray();
            Point[] arrPoints = new Point[arrIndex.Length];

            for (int k = 0; k < arrIndex.Length; k++)
                arrPoints[k] = arrContour[arrIndex[k]];

            MatOfPoint temp = new MatOfPoint();
            temp.fromArray(arrPoints);

            //Filter outliers
            if (Imgproc.contourArea(temp) > 90000 && Imgproc.contourArea(temp) < 110000)
                hulls.Add(temp);
        }
       
       List<MatOfPoint2f> hull2f = new List<MatOfPoint2f>();
       for (int i = 0; i < hulls.Count; i++)
       {
           MatOfPoint2f newPoint = new MatOfPoint2f(hulls[i].toArray());
           hull2f.Add(newPoint);
       }

        List<Rect> rects = new List<Rect>();

       for (int i = 0; i < hulls.Count; i++)
       {
           //Approximate polygon
           MatOfPoint2f approx = new MatOfPoint2f();
           Imgproc.approxPolyDP(hull2f[i], approx, 0.01 * Imgproc.arcLength(hull2f[i], true), true);
           List<Point> approx_polygon = approx.toList();
           approx_polygon = Scannerproc.filterPolygon(approx_polygon);
           double area = Imgproc.contourArea(approx);

            if (Scannerproc.isSquare(approx_polygon))
            {
                Rect r = Imgproc.boundingRect(new MatOfPoint(approx_polygon.ToArray()));
                bool isContain = false;
                for (int k = 0; k < rects.Count; k++)
                {
 
                    if (Scannerproc.distanceTwoPoints(rects[k].tl(), r.tl()) < 100)
                   //if (rects[k].contains(r) || r.contains(rects[k]))
                     isContain = true;
                }

                if (!isContain)
                {
                    rects.Add(r);
                   // Imgproc.rectangle(align, r.tl(), r.br(), new Scalar(255, 0, 0, 255), 3);

                    for (int j = 1; j < 21; j++)
                    {
                        Rect roi = new Rect((int) r.tl().x + (int)((r.width * 1.3) / 6), (int) r.tl().y + (r.height / 21) * j, (int)((r.width * 4.7) / 6), r.height / 21);
                        int num = getAnswerNumber(align, roi);
                        if (num != 0)
                        {
                            Imgproc.putText(align, " " + num, new Point(roi.x - 40, roi.y + 25), 1, 2, new Scalar(255, 0, 0, 255), 3, Core.LINE_AA, false);
                            Imgproc.rectangle(align, roi.tl(), roi.br(), new Scalar(0, 255, 0, 255), 2);
                        }
                    }
                }
            }

           //Center of mass
           int cx = 0,
                   cy = 0;
           for (int k = 0; k < approx_polygon.Count; k++)
           {
               cx += (int)approx_polygon[k].x;
               cy += (int)approx_polygon[k].y;
           }
           cx /= approx_polygon.Count;
           cy /= approx_polygon.Count;
       
           // Imgproc.circle(roi, new Point(cx, cy), 5, new Scalar(255), -1);
       }

       if(rects.Count == 4)
        {
            nowDetected = false;
        }

    }

    public int getAnswerNumber(Mat align, Rect r)
    {
        Mat roi = new Mat(align, r);
        Mat roi_gray = new Mat(), roi_edges = new Mat();
        Imgproc.cvtColor(roi, roi_gray, Imgproc.COLOR_RGB2GRAY);
        Imgproc.Canny(roi_gray, roi_edges, 200, 200);
        // Mat element = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(2 + 1, 2 + 1), new Point(1, 1));
        // Imgproc.dilate(roi_edges, roi_edges, element);

        //Shape detection
        List<MatOfPoint> contours = new List<MatOfPoint>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(roi_edges, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE, new Point(0, 0));

        List<MatOfPoint> hulls = new List<MatOfPoint>();

        for (int i = 0; i < contours.Count; i++)
        {
            MatOfInt hull_temp = new MatOfInt();
            Imgproc.convexHull(contours[i], hull_temp);
            int[] arrIndex = hull_temp.toArray();
            Point[] arrContour = contours[i].toArray();
            Point[] arrPoints = new Point[arrIndex.Length];

            for (int k = 0; k < arrIndex.Length; k++)
                arrPoints[k] = arrContour[arrIndex[k]];

            MatOfPoint temp = new MatOfPoint();
            temp.fromArray(arrPoints);

            //Filter outliers
            if (Imgproc.contourArea(temp) > 40 && Imgproc.contourArea(temp) < 200)
            {
                hulls.Add(temp);
            }
        }

        List<MatOfPoint2f> hull2f = new List<MatOfPoint2f>();
        for (int i = 0; i < hulls.Count; i++)
        {
            MatOfPoint2f newPoint = new MatOfPoint2f(hulls[i].toArray());
            hull2f.Add(newPoint);
        }

        for (int i = 0; i < hulls.Count; i++)
        {
            //Approximate polygon
            MatOfPoint2f approx = new MatOfPoint2f();
            Imgproc.approxPolyDP(hull2f[i], approx, 0.01 * Imgproc.arcLength(hull2f[i], true), true);
            List<Point> approx_polygon = approx.toList();
            approx_polygon = Scannerproc.filterPolygon(approx_polygon);
            double area = Imgproc.contourArea(approx);
    
                //Center of mass
                int cx = 0,
                        cy = 0;
                for (int k = 0; k < approx_polygon.Count; k++)
                {
                    cx += (int)approx_polygon[k].x;
                    cy += (int)approx_polygon[k].y;
                }
                cx /= approx_polygon.Count;
                cy /= approx_polygon.Count;

            // Imgproc.circle(roi, new Point(cx, cy), 5, new Scalar(255), -1);


         // Texture2D tex = new Texture2D(roi.width(), roi.height(), TextureFormat.RGB24, false);
         // Utils.matToTexture2D(roi, tex);
         // byte[] bytes1 = tex.EncodeToJPG();
         // File.WriteAllBytes("D:/2019/OMR/" + "test.png", bytes1);

            Point pos1 = new Point((roi.width() * 1) / 10, cy);
            Point pos2 = new Point((roi.width() * 3) / 10, cy);
            Point pos3 = new Point((roi.width() * 5) / 10, cy);
            Point pos4 = new Point((roi.width() * 7) / 10, cy);
            Point pos5 = new Point((roi.width() * 9) / 10, cy);
            Point nowPos = new Point(cx, cy);

            double[] dist = new double[5];
            dist[0] = Scannerproc.distanceTwoPoints(pos1, nowPos);
            dist[1] = Scannerproc.distanceTwoPoints(pos2, nowPos);
            dist[2] = Scannerproc.distanceTwoPoints(pos3, nowPos);
            dist[3] = Scannerproc.distanceTwoPoints(pos4, nowPos);
            dist[4] = Scannerproc.distanceTwoPoints(pos5, nowPos);

            int id = -1;
            double min_dist = 999999;
            for (int t = 0; t < 5; t++)
            {
                if (dist[t] < min_dist)
                {
                    min_dist = dist[t];
                    id = t;
                }
            }


            return id + 1;

            //return plusPoints(tl, new Point(cx, cy));
        }




        return 0;
    }


    void showCurrentTextureOnScreen()
    {
        const float ratio = 1.0f;

        if (alignment == ALIGNMENT.ORIGINAL)
        {
            texture.Resize(img_orig.width(), img_orig.height());
            gameObject.transform.localScale = new Vector3(img_orig.width() / ratio, img_orig.height() / ratio, 1);
            Utils.matToTexture2D(img_orig, texture);
        }
        else if (alignment == ALIGNMENT.DRAWING)
        {
            texture.Resize(drawing.width(), drawing.height());
            gameObject.transform.localScale = new Vector3(drawing.width() / ratio, drawing.height() / ratio, 1);
            Utils.matToTexture2D(drawing, texture);
        }

        else if (alignment == ALIGNMENT.EDGES)
        {
            texture.Resize(img_edges.width(), img_edges.height());
            gameObject.transform.localScale = new Vector3(img_edges.width() / ratio, img_edges.height() / ratio, 1);
            Utils.matToTexture2D(img_edges, texture);
        }

        else if (alignment == ALIGNMENT.SEGMENTS)
        {
            texture.Resize(img_gray.width(), img_gray.height());
            gameObject.transform.localScale = new Vector3(img_gray.width() / ratio, img_gray.height() / ratio, 1);
            Utils.matToTexture2D(img_gray, texture);
        }
        else if (alignment == ALIGNMENT.LAB)
        {
            texture.Resize(imgLab.width(), imgLab.height());
            gameObject.transform.localScale = new Vector3(imgLab.width() / ratio, imgLab.height() / ratio, 1);
            Utils.matToTexture2D(imgLab, texture);
        }
    }

    public void OnWebCamTextureToMatHelperInitialized()
    {
        Debug.Log("OnWebCamTextureToMatHelperInitialized");
        Mat webCamTextureMat = webCamTextureToMatHelper.GetMat();
        gameObject.transform.localScale = new Vector3(webCamTextureMat.cols(), webCamTextureMat.rows(), 1);
        //Debug.Log("Screen.width " + Screen.width + " Screen.height " + Screen.height + " Screen.orientation " + Screen.orientation);

        float width = webCamTextureMat.width();
        float height = webCamTextureMat.height();

        float widthScale = (float)Screen.width / width;
        float heightScale = (float)Screen.height / height;
        if (widthScale < heightScale)
        {
            Camera.main.orthographicSize = (width * (float)Screen.height / (float)Screen.width) / 2;
        }
        else
        {
            Camera.main.orthographicSize = height / 2;
        }
        img_gray = new Mat(webCamTextureMat.rows(), webCamTextureMat.cols(), CvType.CV_8UC1);
        img_lab = new Mat(webCamTextureMat.rows(), webCamTextureMat.cols(), CvType.CV_8UC1);
        img_orig = new Mat(webCamTextureMat.rows(), webCamTextureMat.cols(), CvType.CV_8UC4);
        drawing = new Mat(webCamTextureMat.rows(), webCamTextureMat.cols(), CvType.CV_8UC4);
        frame = new Mat(webCamTextureMat.rows(), webCamTextureMat.cols(), CvType.CV_8UC4);


        if (showTextureOnScreen)
        {
            texture = new Texture2D(webCamTextureMat.width(), webCamTextureMat.height(), TextureFormat.RGBA32, false);
            gameObject.GetComponent<Renderer>().material.mainTexture = texture;
        }

        result_texture = new Texture2D(1120, 860, TextureFormat.RGBA32, false);

    }

    public void OnWebCamTextureToMatHelperDisposed()
    {
        // Debug.Log("OnWebCamTextureToMatHelperDisposed");
        if (img_gray != null)
            img_gray.Dispose();
        if (img_lab != null)
            img_lab.Dispose();
        if (img_orig != null)
            img_orig.Dispose(); if (frame != null)
            frame.Dispose();

    }
    public void OnWebCamTextureToMatHelperErrorOccurred(WebCamTextureToMatHelper.ErrorCode errorCode)
    {
        Debug.Log("OnWebCamTextureToMatHelperErrorOccurred " + errorCode);
    }

    void OnDestroy()
    {
        webCamTextureToMatHelper.Dispose();
    }

    public void OnPlayButtonClick()
    { webCamTextureToMatHelper.Play(); }

    public void OnPauseButtonClick()
    { webCamTextureToMatHelper.Pause(); }

    public void OnStopButtonClick()
    { webCamTextureToMatHelper.Stop(); }

    public void OnChangeCameraButtonClick()
    { webCamTextureToMatHelper.Initialize(null, webCamTextureToMatHelper.requestedWidth, webCamTextureToMatHelper.requestedHeight, !webCamTextureToMatHelper.requestedIsFrontFacing); }


    private bool debugMode = false;

    int alignIndex = 0;
    public void changeAlignment()
    {
        alignIndex++;
        if (alignIndex >= 3)
            alignIndex = 0;

        if (alignIndex == 0)
            alignment = ALIGNMENT.ORIGINAL;
        else if (alignIndex == 1)
            alignment = ALIGNMENT.TRANSFORMED;
        else if (alignIndex == 2)
            alignment = ALIGNMENT.DRAWING;
    }

    public enum ALIGNMENT { DRAWING, TRANSFORMED, ORIGINAL, EDGES, SEGMENTS, LAB };
    public enum TANGRAMMODE { NONE, TRAIN, TEST };

}
