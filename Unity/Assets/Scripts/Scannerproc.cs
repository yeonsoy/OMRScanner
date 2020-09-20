using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using OpenCVForUnity;

namespace ScannerGeomatry
{
    public class Scannerproc
    {
        //Area thresholds
        public const int AREALT_LOW = 3500; // 5630
        public const int AREALT_HIGH = 6000;
        public const int AREAMT_LOW = 1500; // 1979
        public const int AREAMT_HIGH = 3000;
        public const int AREAST_LOW = 300; // 890
        public const int AREAST_HIGH = 1500;
        public const int AREAS_LOW = 1000;
        public const int AREAS_HIGH = 2500;
        public const int AREAP_LOW = 1000;
        public const int AREAP_HIGH = 2500;
        public const bool FILTER_BY_AREA_FLAG = true;
        public const int MIN_AREA = 350;
        public const int MAX_AREA = 6000;
        public const int ANGLE_THRESHHOLD = 170;
        public const double POLYGON_LENGTH_THRESHOLD = 0.10f;
        public static int piecesDifferentFactor = 20;

        static int yy = 0;

        public static Mat perspectiveAlign(Mat src, Mat dst)
        {

            //Parameters for perspective wrapping
            //MatOfPoint2f srcQuad = new MatOfPoint2f(new Point(-80, 150), new Point(570, 150), new Point(55, 440), new Point(425, 440));
            //MatOfPoint2f dstQuad = new MatOfPoint2f(new Point(0, 0), new Point(480, 0), new Point(0, 370), new Point(480, 370));

            //  Texture2D tex1 = new Texture2D(src.width(), src.height(), TextureFormat.RGB24, false);
            //  Utils.matToTexture2D(src, tex1);
            //  byte[] bytes1 = tex1.EncodeToJPG();
            //  System.IO.File.WriteAllBytes("D:/Patchs/" + yy + ".jpg", bytes1);
            //  yy++;

            // FOR THE NEW BASE
            // MatOfPoint2f srcQuad = new MatOfPoint2f(new Point(-20, 185), new Point(460, 185), new Point(90, 500), new Point(360, 500));
            // MatOfPoint2f dstQuad = new MatOfPoint2f(new Point(0, 0), new Point(480, 0), new Point(0, 640), new Point(480, 640));

            MatOfPoint2f srcQuad = new MatOfPoint2f(new Point(-60, 80), new Point(540, 80), new Point(45, 370), new Point(435, 370));
            MatOfPoint2f dstQuad = new MatOfPoint2f(new Point(0, 0), new Point(480, 0), new Point(0, 380), new Point(480, 380));

            Mat M = Imgproc.getPerspectiveTransform(srcQuad, dstQuad);
            Imgproc.warpPerspective(src, dst, M, new Size(src.width(), src.height()));
            Imgproc.resize(dst, dst, new Size(src.width(), 550));
            return dst;
        }

        public static string detectColor(Mat img, Point center)
        {
            Point red1 = new Point(158, 77);
            Point red2 = new Point(150, 85);
            Point green = new Point(98, 142);
            Point orange1 = new Point(130, 90);
            Point orange2 = new Point(145, 80);
            Point orange3 = new Point(151, 72);
            Mat imgLab = new Mat();
            Imgproc.cvtColor(img, imgLab, Imgproc.COLOR_BGR2Lab);

            byte[] vec3 = new byte[3];
            imgLab.get((int)center.y, (int)center.x, vec3);
            Point colorPoint = new Point((int)vec3[1], (int)vec3[2]);


            //Debug.Log("Color Point : " + colorPoint);
           // 
            //cout << "	Color point: " << colorPoint << endl;

            double[] dist = new double[6];
            dist[0] = distanceTwoPoints(colorPoint, red1);
            dist[1] = distanceTwoPoints(colorPoint, red2);
            dist[2] = distanceTwoPoints(colorPoint, green);
            dist[3] = distanceTwoPoints(colorPoint, orange1);
            dist[4] = distanceTwoPoints(colorPoint, orange2);
            dist[5] = distanceTwoPoints(colorPoint, orange3);

            int id = -1;
            double min_dist = 999999;
            for (int i = 0; i < 6; i++)
            {
                if (dist[i] < min_dist)
                {
                    min_dist = dist[i];
                    id = i;
                }
                //cout << "	" << dist[i] << " ";
            }
            //cout << endl;
            //cout << id << endl;


           // Debug.Log("Color : " + colorPoint + "ID = " + id );

            if (id == 0)
                return "Red";
            if (id == 1)
                return "Red";
            if (id == 2)
                return "Green";
            if (id == 3)
                return "Orange";
            if (id == 4)
                return "Orange";
            if (id == 5)
                return "Orange";

            return "Unknown";
        }

       public static bool isCircle(List<Point> shape)
        {
            //Check number of vertices
          if (shape.Count < 20)
              return false;

            MatOfPoint shape_area = new MatOfPoint();
            shape_area.fromList(shape);

            MatOfPoint2f shape_area2f = new MatOfPoint2f(shape_area.toArray());

         //  if (Imgproc.contourArea(shape_area) < 7500 || Imgproc.contourArea(shape_area) > 10000)
         //    return false;

            double area = Imgproc.contourArea(shape_area);
            double perim = Imgproc.arcLength(shape_area2f, true);
            double ratio = area / perim;

            if (ratio < 18 || ratio > 30)
                return false;

            for (int i = 1; i < shape.Count; i++)
            {
                if (distanceTwoPoints(shape[i - 1], shape[i]) > 30)
                    return false;
            }
            return true;
        }

        public static bool isHeart(List<Point> shape)
        {
            //Check number of vertices
            if (shape.Count < 20)
                return false;

            MatOfPoint shape_area = new MatOfPoint();
            shape_area.fromList(shape);

            MatOfPoint2f shape_area2f = new MatOfPoint2f(shape_area.toArray());

       //   if (Imgproc.contourArea(shape_area) > 6000)
       //       return false;

            double area = Imgproc.contourArea(shape_area);
            double perim = Imgproc.arcLength(shape_area2f, true);
            double ratio = area / perim;

            if (ratio < 18 || ratio > 23)
                return false;

            for (int i = 1; i < shape.Count; i++)
            {
                if (distanceTwoPoints(shape[i - 1], shape[i]) > 20)
                    return true;
            }
            return false;
        }

        public static bool isTriangle(List<Point> shape)
        {
            //cout << "	Vertex Num: " << shape.size() << endl;
            //cout << "	Area: " << contourArea(shape) << endl;

            //Check number of vertices
            if (shape.Count != 3)
                return false;

            //Check shape angles
            double [] length = new double[3], angle = new double[3];
            angle[0] = angleThreePoints(shape[0], shape[1], shape[2]);
            angle[1] = angleThreePoints(shape[1], shape[2], shape[0]);
            angle[2] = angleThreePoints(shape[2], shape[0], shape[1]);

            //if (angle[0] > ANGLE90_LOW && angle[0] < ANGLE90_HIGH)
            //	cout << "	Angles:" << angle[0] << " " << angle[1] << " " << angle[2] << endl;

            MatOfPoint shape_area = new MatOfPoint();
            shape_area.fromList(shape);

          if (!(Imgproc.contourArea(shape_area) > 3300 && Imgproc.contourArea(shape_area) < 6000))
              return false;

            if ((angle[0] > 50 && angle[0] < 70 && angle[1] > 50 && angle[1] < 70 && angle[2] > 50 && angle[2] < 70))
                return true;
            else
                return false;

        }

        public static double distanceTwoPoints(Point left, Point right)
        {
            return Mathf.Sqrt((float)((left.x - right.x) * (left.x - right.x) + (left.y - right.y) * (left.y - right.y)));
        }

        public static double angleThreePoints(Point left, Point p_base, Point right)
        {
            //Calculate distances between points
            double a = distanceTwoPoints(left, p_base);
            double b = distanceTwoPoints(right, p_base);
            double c = distanceTwoPoints(left, right);

            //Calculate angle in degrees (0...180)
            double theta = Mathf.Acos((float)((a * a + b * b - c * c) / (2.0 * a * b)));
            double angle = theta / Mathf.PI * 180;

            return angle;
        }


        public static bool isSquare(List <Point> shape)
        {
            double [] length = new double[4], angle = new double[4];

            //Check number of vertices
            //cout << "	Vertex Num: " << shape.size() << endl;
            //cout << "	Area: " << contourArea(shape) << endl;

            if (shape.Count != 4)
                return false;

            MatOfPoint shape_area = new MatOfPoint();
            shape_area.fromList(shape);

       //   if (!(Imgproc.contourArea(shape_area) > 8000 && Imgproc.contourArea(shape_area) < 12000))
       //       return false;
       //
            //Calculate side lengths
            length[0] = distanceTwoPoints(shape[0], shape[1]);
            length[1] = distanceTwoPoints(shape[1], shape[2]);
            length[2] = distanceTwoPoints(shape[2], shape[3]);
            length[3] = distanceTwoPoints(shape[3], shape[0]);

            //Calculate angles
            angle[0] = angleThreePoints(shape[0], shape[1], shape[2]);
            angle[1] = angleThreePoints(shape[1], shape[2], shape[3]);
            angle[2] = angleThreePoints(shape[2], shape[3], shape[0]);
            angle[3] = angleThreePoints(shape[3], shape[0], shape[1]);

            //Square check
            if (angle[0] > 80 && angle[0] < 100 &&
                angle[1] > 80 && angle[1] < 100 &&
                angle[2] > 80 && angle[2] < 100 &&
                angle[3] > 80 && angle[3] < 100)
                return true;
            else
                return false;

        }

        public static bool isStar(List<Point> shape)
        {
            double[] length = new double[5], angle = new double[5];

            if (shape.Count != 5)
                return false;

            MatOfPoint shape_area = new MatOfPoint();
            shape_area.fromList(shape);

              if (!(Imgproc.contourArea(shape_area) > 6000 && Imgproc.contourArea(shape_area) < 10000))
                  return false;

            //Calculate side lengths
            length[0] = distanceTwoPoints(shape[0], shape[1]);
            length[1] = distanceTwoPoints(shape[1], shape[2]);
            length[2] = distanceTwoPoints(shape[2], shape[3]);
            length[3] = distanceTwoPoints(shape[3], shape[4]);
            length[4] = distanceTwoPoints(shape[4], shape[0]);

            //Calculate angles
            angle[0] = angleThreePoints(shape[0], shape[1], shape[2]);
            angle[1] = angleThreePoints(shape[1], shape[2], shape[3]);
            angle[2] = angleThreePoints(shape[2], shape[3], shape[4]);
            angle[3] = angleThreePoints(shape[3], shape[4], shape[0]);
            angle[4] = angleThreePoints(shape[4], shape[0], shape[1]);

            //Star check
            if (angle[0] > 98 && angle[0] < 128 &&
                angle[1] > 98 && angle[1] < 128 &&
                angle[2] > 98 && angle[2] < 128 &&
                angle[3] > 98 && angle[3] < 128 &&
                angle[4] > 98 && angle[4] < 128)
                return true;
            else
                return false;

        }

        public static List<Point> filterPolygon(List<Point> approx_polygon)
        {
            while (true)
            {
                double max_ar = 0;
                int max_ar_id = 0;
                for (int k = 0; k < approx_polygon.Count; k++)
                {
                    List<Point> cur_polygon = new List<Point>();

                    for (int i = 0; i < approx_polygon.Count; i++)
                        cur_polygon.Add(approx_polygon[i]);

                    cur_polygon.Remove(cur_polygon[0 + k]);

                    MatOfPoint cur_area = new MatOfPoint();
                    MatOfPoint approx_area = new MatOfPoint();
                    cur_area.fromList(cur_polygon);
                    approx_area.fromList(approx_polygon);

                    double area_ratio = Imgproc.contourArea(cur_area) / Imgproc.contourArea(approx_area);

                    // Debug.Log("ratio" + area_ratio);

                    if (area_ratio > max_ar)
                    {
                        max_ar = area_ratio;
                        max_ar_id = k;
                    }
                }

                //If area still large enough remove a vertex
                if (max_ar > 0.8)
                {
                    // Debug.Log("Remove vertex  " + max_ar_id);
                    approx_polygon.Remove(approx_polygon.ToArray()[0 + max_ar_id]);
                }
                else
                    break;
            }

            return approx_polygon;
        }

        public static void drawShape(Mat img, List<Point> shape, Scalar color, int thickness = 3)
        {
            //Display filtered polygons
            for (int k = 0; k < shape.Count - 1; k++)
            {
                Imgproc.line(img, shape[k], shape[k + 1], color, thickness);
                Imgproc.circle(img, shape[k], 5, color, thickness);
            }
            Imgproc.line(img, shape[0], shape[shape.Count - 1], color, thickness);
            Imgproc.circle(img, shape[shape.Count - 1], 5, color, thickness);
        }

    }

}