#include "sys.h"
#include "delay.h"
#include "usart.h"
#include "led.h"
#include "adc.h"
#include "math.h"
#include "timer.h"

#define SAMPLE_NUM 100 //采样值数组序列的长度
#define SOUND_NUM 4    //超声波接收模块的个数

u16 adcCHx[SAMPLE_NUM][SOUND_NUM]; //存放若干路采样值的二维数组

u16 adcx; //test
float voltage; //test

char adc_channel[19] = {0x00,0x01,0x02,0x03,0x04,0x05,0x06,0x07,0x08,0x09,
						0x0A,0x0B,0x0C,0x0D,0x0E,0x0F,0x10,0x11,0x12};  //用下标i来选择ADC的CH

int main(void)
{ 
	NVIC_PriorityGroupConfig(NVIC_PriorityGroup_2);//设置系统中断优先级分组2
	delay_init(168);		   //初始化延时函数clk
	LED_Init();				     //初始化LED端口
	uart_init(115200);     //初始化串口波特率
	Adc_Init();         //初始化ADC		
	TIM3_Int_Init(10000-1, 8400-1); //定时器时钟84M，分频系数8400，所以
								   //84M/8400=10kHz的计数频率，计数10000次为1000ms
	
	//用LED0指示程序烧录
	LED0 = 0;
	delay_ms(1000);
	LED0 = 1;	
	
	while(1)
	{ 	
		if(uart_receiver_begin()) //当接收到信息时
		{					   
			LED0 = 0; //蓝牙接收到信息时LED0亮1000ms
			delay_ms(1000);
			LED0 = 1;
			
			uart_receiver_reset(); //把接收标志位重新置0
		}else
		{			
		}	
	}
}

//定时器3中断服务函数
void TIM3_IRQHandler(void)
{
	if(TIM_GetITStatus(TIM3,TIM_IT_Update)==SET) //溢出中断
	{	
		/*
		u16 count = 0;
		u8 i;
		//ADC各通道采样值
		for(i = 0; i < SOUND_NUM; i++)	
		{
			adcCHx[count][i] = Get_Adc(adc_channel[i]);
		}
		count += 1;
		if (count == SAMPLE_NUM)
			count = 0;
		*/
		
		//test
		//adcx = Get_Adc(ADC_Channel_5);
		//printf("adc = %d\n",adcx);
		//voltage = (float)(adcx*(3.3/4096));
		//printf("voltage = %f\n",voltage);
		
		LED1 = !LED1;
		printf("%d",10);
	}
	TIM_ClearITPendingBit(TIM3,TIM_IT_Update);  //清除中断标志位
}

