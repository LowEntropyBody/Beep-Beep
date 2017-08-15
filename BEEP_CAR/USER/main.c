#include "sys.h"
#include "delay.h"
#include "usart.h"
#include "led.h"
#include "adc.h"
#include "math.h"
#include "timer.h"

#define SAMPLE_NUM 100 //����ֵ�������еĳ���
#define SOUND_NUM 4    //����������ģ��ĸ���

u16 adcCHx[SAMPLE_NUM][SOUND_NUM]; //�������·����ֵ�Ķ�ά����

u16 adcx; //test
float voltage; //test

char adc_channel[19] = {0x00,0x01,0x02,0x03,0x04,0x05,0x06,0x07,0x08,0x09,
						0x0A,0x0B,0x0C,0x0D,0x0E,0x0F,0x10,0x11,0x12};  //���±�i��ѡ��ADC��CH

int main(void)
{ 
	NVIC_PriorityGroupConfig(NVIC_PriorityGroup_2);//����ϵͳ�ж����ȼ�����2
	delay_init(168);		   //��ʼ����ʱ����clk
	LED_Init();				     //��ʼ��LED�˿�
	uart_init(115200);     //��ʼ�����ڲ�����
	Adc_Init();         //��ʼ��ADC		
	TIM3_Int_Init(10000-1, 8400-1); //��ʱ��ʱ��84M����Ƶϵ��8400������
								   //84M/8400=10kHz�ļ���Ƶ�ʣ�����10000��Ϊ1000ms
	
	//��LED0ָʾ������¼
	LED0 = 0;
	delay_ms(1000);
	LED0 = 1;	
	
	while(1)
	{ 	
		if(uart_receiver_begin()) //�����յ���Ϣʱ
		{					   
			LED0 = 0; //�������յ���ϢʱLED0��1000ms
			delay_ms(1000);
			LED0 = 1;
			
			uart_receiver_reset(); //�ѽ��ձ�־λ������0
		}else
		{			
		}	
	}
}

//��ʱ��3�жϷ�����
void TIM3_IRQHandler(void)
{
	if(TIM_GetITStatus(TIM3,TIM_IT_Update)==SET) //����ж�
	{	
		/*
		u16 count = 0;
		u8 i;
		//ADC��ͨ������ֵ
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
	TIM_ClearITPendingBit(TIM3,TIM_IT_Update);  //����жϱ�־λ
}

