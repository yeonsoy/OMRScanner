# OMRScanner
Android Studio with OpenCV, OMR Scanner for 수능

## MAIN GOAL
<img src="https://github.com/Seungyeon-Lee/OMRScanner/blob/master/resources/1.png" height=300 />

핸드폰으로 OMR을 촬영하여, 채점을 할 수 있도록 하는 어플리케이션

<br>

## IDEA
왼쪽 위, 아래 / 오른쪽 위, 아래 4점을 인식하여 진행.

<img src="https://github.com/Seungyeon-Lee/OMRScanner/blob/master/resources/idea.png" height=200 />

카드 인식처럼 프리뷰(preview)의 4점을 유저에게 보여주며 인식하도록 함.

<br>

## IMPLEMENT
1. OMR을 촬영. 스캐너 영상과 같이 원근 변환(Perspective transformation)

<img src="https://github.com/Seungyeon-Lee/OMRScanner/blob/master/resources/2.png" />

2. 원근 변환 조건 설정.
- 영상에 존재하는 contours 중 영역 크기가 카메라 영상 이미지 화면의 1/3 이상의 크기일 것.
- 해당 영역의 꼭지점이 4개이며, 4점이 각각 이루는 각도가 90도 ~ 120도 사이일 것.

<img src="https://github.com/Seungyeon-Lee/OMRScanner/blob/master/resources/3.png" height=400 />


3. 변환된 이미지 내의 정답란 찾기

<img src="https://github.com/Seungyeon-Lee/OMRScanner/blob/master/resources/4.png" height=400 />


4. 정답란을 Y개만큼 나누고  X개 만큼 나누어 마킹한 정답이 무엇인지 인식

<img src="https://github.com/Seungyeon-Lee/OMRScanner/blob/master/resources/5.png" height=400 />

5. 최종.

<img src="https://github.com/Seungyeon-Lee/OMRScanner/blob/master/resources/6.gif" />
