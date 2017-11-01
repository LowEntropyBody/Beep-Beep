#include "sys.h"
#include "delay.h"
#include "usart.h"
#include "led.h"
#include "adc.h"
#include "math.h"
#include "timer.h"
#include "beep.h"
#include "run.h"
#include "location.h"
#include "msg.h"

int main(void)
{ 
	NVIC_PriorityGroupConfig(NVIC_PriorityGroup_2);//设置系统中断优先级分组2
	delay_init(168);		   //初始化延时函数clk
	LED_Init();				     //初始化LED端口
	uart_init(115200);     //初始化串口波特率
	
	//系统参数初始化，用LED0指示
	LED0 = 0;
	init_beep();
	init_run();
	init_msg();
	LED0 = 1;
	
	while(1)
	{		
		if(isBeginWork())
		{
			LED0 = 0; //LED0指示终端工作过程			
			start_sound_sense(); 
			
			if(sound_sense_wait_cal())				
				location_cal(sound_arrival_time_to_distance());
			else
				printf(RX_ERROR); //蓝牙发送ERROR请求重新发送声音信号
			
			isWorkSet(0); 		
			LED0 = 1;
		}
		
		/*
		run(200,1,200,1);
		while(car.is_run);
		delay_ms(1000);
		run(200,0,200,0);
		while(car.is_run);
		delay_ms(1000);
		*/
	}	
}

