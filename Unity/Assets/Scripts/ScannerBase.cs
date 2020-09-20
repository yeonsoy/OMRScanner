using System.Collections;
using System.Collections.Generic;
using UnityEngine;

public class ScannerBase : MonoBehaviour {

    public bool ORIGIN = false;
    public bool ALIGN = false;
    public bool IMGLAB = false;
    public bool DRAW = true;

    //Area thresholds
    const int AREALT_LOW = 3500; // 5630
    const int AREALT_HIGH = 8000;
    const int AREAMT_LOW = 1500; // 1979
    const int AREAMT_HIGH = 3500;
    const int AREAST_LOW = 200; // 890
    const int AREAST_HIGH = 1500;
    const int AREAS_LOW = 1000;
    const int AREAS_HIGH = 2000;
    const int AREAP_LOW = 1000;
    const int AREAP_HIGH = 4000;
    const bool FILTER_BY_AREA_FLAG = true;
    const int MIN_AREA = 200;
    const int MAX_AREA = 8000;
    const int ANGLE_THRESHHOLD = 170;
    const double POLYGON_LENGTH_THRESHOLD = 0.10f;

}
