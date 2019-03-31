# LEGO MindStorm - "Unmanned Aerial"

LEGO MindStorm과 LEJOS(Java)를 이용한 무인 사다리차 제작.

## Description

##### 기능 목록

* 1. 라인트레이서
* 2. RGB 센서를 사용한 기능 수행 장소 파악
* 3. 터치 센서 & 초음파 센서를 이용한 사다리로 물건 받기

###### 2. 세부기능
+  라인트레이서 
+  RGB 센서를 사용한 기능 수행 장소 파악
+ 3. 터치 센서 & 초음파 센서를 이용한 사다리로 물건 받기


###### 3. 애로 사항

1. RGB **센서가 빛 센서를 방해** (1)
→ RGB **센서를** 0.1 **초 주기로 점멸 하게 함**

```ruby
// 메인에서 색깔에 따라 차를 멈추거나 사다리를 내리거나 하는 동작을 함
	void ladderWork()
	{
		// 색 센서를 계속 반짝이면 빛센서를 방해하므로 0.1초마다 반짝이게함!
		try
		{
			Thread.sleep(100);
		} catch (InterruptedException e)
		{
		}

		// 색 센서 반짝
		color = color_1.getColor();

		// RGB값을 색 객체에서 가져옴
		int R = color.getRed();
		int G = color.getGreen();
		int B = color.getBlue();
		LCD.drawString("R :" + R, 0, 0);
		LCD.drawString("G :" + G, 0, 1);
		LCD.drawString("B :" + B, 0, 2);
```

1. RGB **센서가 빛 센서를 방해** (2)
→ **빛 값이 너무 밝아도 흰색으로 인식 앆함**
→ boolean **변수를 이용해 색 센서가 색을 감지하는 동앆 빛센서는 선을 읽지
않게 함**

```ruby
// 가끔 빛센서가 색센서에 의해 값이 60 이상 찍히는 경우가 있는데 그걸 방지하고
				// 색 센서가 색을 인식하는 동안 감지하는 빛 값은 무시함(색 센서가 반짝여서 정확한 값이 아니므로)
				if (light.readValue() < 60 && !colorDetected)
				{
					// 흰 종이를 감지했을 때
					if (light.readValue() >= 40)
					{
						/* 좌회전
						 * 왼쪽 오른쪽 모터 둘다 앞으로 돌리긴 하는데 왼쪽 모터보다 오른쪽 모터를 더 빨리 돌려서 왼쪽으로 회전하게 함
						 */
						Motor.A.setSpeed(20);
						Motor.C.setSpeed(90);
						Motor.A.backward();
						Motor.C.backward();
						leftOrRight = -2; /* 좌회전이니까 -2 대입 */
					}
					// 검정선을 감지했을 때
					else
					{
						 
						/* 우회전
						 * 왼쪽 오른쪽 모터 둘다 앞으로 돌리긴 하는데 오른쪽 모터보다 왼쪽 모터를 더 빨리 돌려서 오른쪽으로 회전하게 함
						 */
						Motor.A.setSpeed(90);
						Motor.C.setSpeed(20);
						Motor.A.backward();
						Motor.C.backward();
						leftOrRight = 2; /* 우회전이니까 +2 대입 */
					}
				}
```


2. RGB **센서와 빛 센서의 동시 실행 문제**
→ **쓰레드로 해결**
```ruby
// 빛센서 쓰레드
	class LightDetect extends Thread
	{
		@Override
		public void run()
		{
			while (true) // 메인이 끝날때까지 계속 동작
			{
				try
				{
					// 0.05초 간격으로 빛센서를 쏨
					Thread.sleep(50);
```
3. RGB **센서의 반응 속도 문제**
→ **별도의 변수로 해결**
```ruby
// 만약 멈추는 순간 차가 왼쪽으로 돌았다면
			if (**leftOrRight** == -2)
			{
				// 오른쪽 모터를 뒤로 20도 돌려서 다시 오른쪽으로 복귀
				Motor.C.rotate(20);
			}

			// 만약 멈추는 순간 차가 오른쪽으로 돌았다면
			else if (**leftOrRight** == 2)
			{
				// 왼쪽 모터를 뒤로 20도 돌려서 다시 왼쪽으로 복귀
				Motor.A.rotate(20);
			}
```

###### 5. 시연 영상


