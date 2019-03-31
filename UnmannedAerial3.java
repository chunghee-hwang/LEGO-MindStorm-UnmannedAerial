package com.mydomain;

import lejos.nxt.*;
import lejos.robotics.Color;

public class UnmannedAerial3
{
	// 컬러 센서 객체
	ColorSensor color_1 = new ColorSensor(SensorPort.S4, SensorConstants.TYPE_LIGHT_ACTIVE);

	// 색 객체
	Color color;

	// 초음파센서 객체
	UltrasonicSensor sonic;

	// 터치센서 객체
	TouchSensor touchSensor;

	// 빛 센서 객체
	LightSensor light = new LightSensor(SensorPort.S3);

	// 색센서가 색을 감지중이면 true
	boolean colorDetected = false;

	// 빛센서 동작시키는 쓰레드 객체
	Thread lightDetectT;

	// 취소 버튼 객체
	Button escapeB;

	// ladderWork함수에서 isTurn을 true로 만들어주면 빛 센서 쓰레드 쪽에서 검정선을 찾을 때까지 도는 작업을 함
	boolean isTurn = false;

	// 초기 설정
	UnmannedAerial3()
	{
		// LCD.drawString("UnmannedArial", 0, 0);
		// 버튼 한 번 눌렀을 때 시작
		Button.waitForAnyPress();

		// 취소 버튼 눌렀을 때 프로그램 꺼지게 함.
		escapeB = Button.ESCAPE;
		escapeB.addButtonListener(new ButtonListener()
		{
			@Override
			public void buttonPressed(Button b)
			{
				System.exit(0);
			}

			@Override
			public void buttonReleased(Button b)
			{
			}
		});

		// 터치센서 객체 생성
		touchSensor = new TouchSensor(SensorPort.S1 /* 센서를 꽂을 포트는 1번이라는 뜻 */);

		// 초음파센서 객체 생성
		sonic = new UltrasonicSensor(SensorPort.S2 /* nxt본체의 포트 2번에 꽂을 거라는 뜻 */);

		// 초음파를 간격을 두고 쏘는게 아니라 끊임없이 쏨
		sonic.setMode(UltrasonicSensor.MODE_CONTINUOUS);

		// 빛 센서 쓰레드 시작
		startLightSensor();

		while (!Button.ESCAPE.isDown())
		{
			// 메인에서 색깔에 따라 차를 멈추거나 사다리를 내리거나 하는 동작을 함
			ladderWork();
		}
	}

	public static void main(String[] args)
	{
		new UnmannedAerial3();
	}

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

		// 빨강을 감지했을 때 사다리를 내리고 공을 받은 뒤 공을 초음파센서로 인식하면 사다리를 다시 올리고 유턴함
		// 오른쪽에 있는 G>255 조건은 가끔 초록색 값이 팔백몇으로 나올때가 있어서 넣었음
		if ((R > 180 && G < 100 && B < 100) || (R > 180 && G > 255 && B < 100))
		{
			colorDetected = true; /* 색을 읽는 동안 빛센서 쓰레드가 아무동작을 못하게 하는 변수 켜줌 */
			LCD.drawString("RED", 0, 5);
			Sound.twoBeeps(); /* 빨강을 탐지했을 때 삐삑 소리나게함 */

			// 빨강을 감지했으니까 모터 멈춤
			Motor.A.stop();
			Motor.C.stop();

			// 만약 멈추는 순간 차가 왼쪽으로 돌았다면
			if (leftOrRight == -2)
			{
				// 오른쪽 모터를 뒤로 20도 돌려서 다시 오른쪽으로 복귀
				Motor.C.rotate(20);
			}

			// 만약 멈추는 순간 차가 오른쪽으로 돌았다면
			else if (leftOrRight == 2)
			{
				// 왼쪽 모터를 뒤로 20도 돌려서 다시 왼쪽으로 복귀
				Motor.A.rotate(20);
			}

			// 여기부턴 사다리 내리는 부분
			Motor.B.setSpeed(40); // 모터 B(사다리)속도 40설정

			int count = 0;
			for (int i = 0; i < 20; i++)
			{
				Motor.B.rotate(-5);
				count++;

				// 사다리를 내리던 중에 터치센서가 눌리면
				if (touchSensor.isPressed())
				{
					Motor.B.stop(); /* 사다리 멈춤 */

					// 초음파센서의 거리가 8 이하이거나 0일때까지 계속 초음파센서로 거리 측정
					while (!Button.ESCAPE.isDown())
					{
						try
						{
							Thread.sleep(500);
						} catch (Exception e)
						{
						}

						// 초음파센서는 거리가 0일때 0대신 255를 반환함!
						// 만약 공을 인식했다면
						if (sonic.getDistance() <= 8 || sonic.getDistance() == 255)
						{
							// 사다리를 다시 올림
							Motor.B.rotate(count * 4);

							// while문 나감
							break;
						}
					}
					break;
				}

				// 터치센서가 안눌려도 i == 18에 초음파센서로 공이 올때까지 기다림
				if (i == 18)
				{
					try
					{
						// 사다리를 올리기 전에 혹시 공이 아직 건물에서 내려오지 않았을 경우를 대비해 2초동안 기다림
						Thread.sleep(2000);
					} catch (InterruptedException e)
					{
					}

					// 초음파센서의 거리가 8 이하이거나 0일때까지 계속 초음파센서로 거리 측정
					while (!Button.ESCAPE.isDown())
					{
						try
						{
							Thread.sleep(500);
						} catch (Exception e)
						{
						}

						// 만약 공을 인식했다면
						if (sonic.getDistance() <= 8 || sonic.getDistance() == 255)
						{
							// 사다리를 다시 올림
							Motor.B.rotate(count * 4);

							// while문 나감
							break;
						}
					}

				}

			} //for 끝

			// 사다리까지 내렸다가 올렸으면 라인을 찾을 때까지 제자리에서 돔(빛센서 쓰레드에서)
			isTurn = true; /*빛센서 쓰레드의 if문에 영향을 주는 변수임. 
			true로 바꿈으로서 빛센서쓰레드에서 제자리에서도는 동작 수행*/

			colorDetected = false; /* 색 감지하는 걸 마쳤으니 false로 바꿈 */

			/* 쓰레드에서 제자리에서 도는 동작을 하는 동안 잠시 색센서 관련 동작 멈춤 */
			while (isTurn)
			{
				try
				{
					Thread.sleep(500);
					// busy-waiting
				} catch (InterruptedException e)
				{
				}
			}

		}

		// Blue가 나왔을 경우 목적지에 물건 내리기
		else if (B > G && B > R)
		{
			colorDetected = true;
			LCD.drawString("BLUE", 1, 5);
			Sound.twoBeeps(); /* 파랑을 탐지했을 때 삐삑 소리나게함 */

			// 차 멈춘후 사다리 땅까지 내리기
			Motor.A.stop();
			Motor.C.stop();
			Motor.B.setSpeed(90);
			Motor.B.rotate(-150);
			Motor.B.rotate(150);

			// 프로그램 종료
			System.exit(0);
			colorDetected = false;
		}

	}

	// 빛센서 쓰레드 시작하게 하는 함수
	void startLightSensor()
	{
		light.setFloodlight(true);
		Runnable lightDetect = new LightDetect();
		lightDetectT = new Thread(lightDetect);
		lightDetectT.setDaemon(true);
		lightDetectT.start();
	}

	// 아래에 있는 빛센서 쓰레드에서 차가 지그재그로 가는데
	// 좌회전 하는 순간에는 leftOrRight에 -2를 대입하고, 우회전 하는 순간에는 leftOrRight에 +2를 대입한다.
	// 나중에 빨간색을 감지하고 차를 멈추는 순간 차가 왼쪽으로 트는지 오른쪽으로 트는지 알려주는 변수
	int leftOrRight = 0;

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
				} catch (InterruptedException e)
				{
				}

				/* isTurn == true일때 와일문 들어가서 제자리에서 회전함 */
				if (isTurn)
				{
					//이미 빛센서가 검정선을 밟고 있을 수도 있으니까 검정선에서 빠져나가기 위해 왼쪽 모터를 200도 정도 돌림
					Motor.A.setSpeed(80);
					Motor.A.rotate(-200);
					Motor.C.stop();

					// 제자리에서 돌기시작할 때 이미 빛센서가 검정선을 밟고 있을 수도 있어서 이 와일문 넣음
					// 만약 검정선 위에 이미 있을 경우 이 와일문 실행되고 하얀 종이 위에 있을 경우 이 와일문 빠져나감
					while (light.readValue() <= 40)
					{
						Motor.A.setSpeed(90);
						Motor.C.setSpeed(90);

						// 모터를 하나는 뒤로 하나는 앞으로 돌리면 제자리회전됨
						Motor.A.backward();
						Motor.C.forward();

					}

					// 차가 검정선을 찾을때까지 계속 돈다.
					while (light.readValue() > 40)
					{
						Motor.A.setSpeed(90);
						Motor.C.setSpeed(90);

						// 모터를 하나는 뒤로 하나는 앞으로 돌리면 제자리회전됨
						Motor.A.backward();
						Motor.C.forward();
					}
					isTurn = false; /* 검정선을 찾았으니까 false로 바꿈 */
				}

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
			}
		}
	}
}
