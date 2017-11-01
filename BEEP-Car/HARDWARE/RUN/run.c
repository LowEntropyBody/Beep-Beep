#include "run.h"
#include "sys.h"
#include "timer.h"
#include "usart.h"
#include "math.h"
#include "stdlib.h"

struct Car car; 

///////////////////////////////////////引脚输出初始化/////////////////////////////////////
//对控制相关的引脚输出进行初始化
void my_GPIO_config(u8* pin){
	GPIO_TypeDef *GPIO;
	GPIO_InitTypeDef GPIO_InitStructure;
	uint16_t n;
	
	switch(pin[0])
	{
	case GPIO_A:
		GPIO=GPIOA;
		RCC_AHB1PeriphClockCmd(RCC_AHB1Periph_GPIOA,ENABLE);
		break;
	case GPIO_B:
		GPIO=GPIOB;
		RCC_AHB1PeriphClockCmd(RCC_AHB1Periph_GPIOB,ENABLE);
		break;
	case GPIO_C:
		GPIO=GPIOC;
		RCC_AHB1PeriphClockCmd(RCC_AHB1Periph_GPIOC,ENABLE);
		break;
	case GPIO_D:
		GPIO=GPIOD;
		RCC_AHB1PeriphClockCmd(RCC_AHB1Periph_GPIOD,ENABLE);
		break;
	case GPIO_E:
		GPIO=GPIOE;
		RCC_AHB1PeriphClockCmd(RCC_AHB1Periph_GPIOE,ENABLE);
		break;
	case GPIO_F:
		GPIO=GPIOF;
		RCC_AHB1PeriphClockCmd(RCC_AHB1Periph_GPIOF,ENABLE);
		break;
	case GPIO_G:
		GPIO=GPIOG;
		RCC_AHB1PeriphClockCmd(RCC_AHB1Periph_GPIOG,ENABLE);
		break;
	}
	
	switch(pin[1])
	{
	case 0:
		n=GPIO_Pin_0;
		break;
	case 1:
		n=GPIO_Pin_1;
		break;
	case 2:
		n=GPIO_Pin_2;
		break;
	case 3:
		n=GPIO_Pin_3;
		break;
	case 4:
		n=GPIO_Pin_4;
		break;
	case 5:
		n=GPIO_Pin_5;
		break;
	case 6:
		n=GPIO_Pin_6;
		break;
	case 7:
		n=GPIO_Pin_7;
		break;
	case 8:
		n=GPIO_Pin_8;
		break;
	case 9:
		n=GPIO_Pin_9;
		break;
	case 10:
		n=GPIO_Pin_10;
		break;
	case 11:
		n=GPIO_Pin_11;
		break;
	case 12:
		n=GPIO_Pin_12;
		break;
	case 13:
		n=GPIO_Pin_13;
		break;
	case 14:
		n=GPIO_Pin_14;
		break;
	case 15:
		n=GPIO_Pin_15;
		break;
	}
	
	GPIO_InitStructure.GPIO_Pin = n;
	GPIO_InitStructure.GPIO_Mode = GPIO_Mode_OUT; //输出模式
	GPIO_InitStructure.GPIO_OType = GPIO_OType_PP;//推挽模式
	GPIO_InitStructure.GPIO_Speed = GPIO_Speed_50MHz; //输出频率最高50MHz
	GPIO_InitStructure.GPIO_PuPd = GPIO_PuPd_UP; //上拉
	GPIO_Init(GPIO, &GPIO_InitStructure); //初始化
}

//////////////////////////////////////小车运动和控制参数初始化////////////////////////////////////
void init_control(struct Wheel *wheel, int freq, int duty){
	wheel->edge_count = 0;
	wheel->edge = 0;
	wheel->count = 0;
	wheel->freq = freq;
	wheel->duty = duty;
}

void init_car(int grid_round, float wheel_space, float wheel_radius, float rad_block, int speed_max, int freq, int duty){
	car.is_change = 0;
	car.is_run = 0;
	car.grid_round = grid_round;
	car.wheel_space = wheel_space;
	car.wheel_radius = wheel_radius;
	car.rad_block = rad_block;
	car.speed_max = speed_max;	
	init_control(car.wheel, freq, duty);
	init_control(car.wheel+1, freq, duty);
	PULSE_LEFT = 0;
	PULSE_RIGHT = 0;
}

void init_run(){
	//引脚声明应与run.h中宏保持一致
	u8 pin_pulse_left[2] = {GPIO_C, 7};
	u8 pin_pulse_right[2] = {GPIO_C, 8};

	my_GPIO_config(pin_pulse_left);
	my_GPIO_config(pin_pulse_right);
	
	//参数初始化
	init_car(GRID_ROUND, WHEEL_SPACE, WHEEL_RADIUS, RAD_BLOCK, SPEED_MAX, FREQ, DUTY);
	
	//初始化定时器
	TIM4_Int_Init(100000/((car.wheel)->freq)/((car.wheel)->duty)-1,840-1); //TIM4做840分频得到100k再计数，控制左轮运动
	TIM5_Int_Init(100000/((car.wheel)->freq)/((car.wheel)->duty)-1,840-1); //TIM5做840分频得到100k再计数，控制右轮运动
}

//////////////////////////////////////小车运动控制函数////////////////////////////////////
//新坐标到来时进行重置
void reset_control(){
	(car.wheel)->edge_count = 0;
	(car.wheel+1)->edge_count = 0;
	//DIR_LEFT = 1; //test
	//DIR_RIGHT = 1;
	PULSE_LEFT = 0;
	PULSE_RIGHT = 0;
}

//run函数控制左轮右轮转动格数及方向
void run(int block_left, u8 dir_left, int block_right, u8 dir_right){
	car.is_change = 1;
	(car.wheel)->edge = block_left;  
	(car.wheel+1)->edge = block_right; 
	reset_control();
	car.is_run = 1;
	car.is_change = 0;
}

/////////////////////////////////////定时器控制小车驱动///////////////////////////////////
//定时器4控制左轮驱动
void TIM4_IRQHandler(void)
{
	if(TIM_GetITStatus(TIM4,TIM_IT_Update) == SET) 
	{
		if(car.is_change == 0){
			if(((car.wheel)->edge_count) < ((car.wheel)->edge)){
				//共阳极接法实际占空比为(1-1/duty),低电平时直接翻转，高电平时则计数满再翻转，得到高占空比
				if(!PULSE_LEFT)
					PULSE_LEFT = !PULSE_LEFT;
				else if((car.wheel)->count == (car.wheel)->duty-2)
				{
					PULSE_LEFT = !PULSE_LEFT;
					(car.wheel)->count = 0;
					(car.wheel)->edge_count ++;
				}
				else 
					(car.wheel)->count ++;
		}
		else if(((car.wheel+1)->edge_count) >= ((car.wheel+1)->edge)) //两个轮的运动都完成则把is_run复位
				car.is_run = 0;	
	}
	TIM_ClearITPendingBit(TIM4,TIM_IT_Update); 
	}
}
	
//定时器5控制右轮驱动
void TIM5_IRQHandler(void)
{
	if(TIM_GetITStatus(TIM5,TIM_IT_Update) == SET) 
	{
		if(car.is_change == 0){
			if(((car.wheel+1)->edge_count) < ((car.wheel+1)->edge)){
				//共阳极接法实际占空比为(1-1/duty),低电平时直接翻转，高电平时则计数满再翻转，得到高占空比
				if(!PULSE_RIGHT)
					PULSE_RIGHT = !PULSE_RIGHT;
				else if((car.wheel+1)->count == (car.wheel+1)->duty-2)
				{
					PULSE_RIGHT = !PULSE_RIGHT;
					(car.wheel+1)->count = 0;
					(car.wheel+1)->edge_count ++;
				}
				else 
					(car.wheel+1)->count ++;	
			}
		}
	TIM_ClearITPendingBit(TIM5,TIM_IT_Update); 
	}
}

//////////////////////////////////////小车控制算法函数////////////////////////////////////
//(x, y)：目标在xOy二维平面上的坐标位置，单位：cm
void track(float x, float y){
	int turn_block = 0 ;
	float r = sqrt(x*x + y*y); //r:目标距离		
	if(r > SAFE_DISTANCE) // 若超出安全距离，计算运动参数，否则不动
	{
	//-----------旋转至面向目标----------
	//计算旋转角度，在目标在y轴上（x=0），turn_block不变，为初始值0
		if (y&&x)  //如果不在坐标轴上，计算电机旋转格数
		{
			if (y > 0) //目标在前方，夹角锐角
				turn_block = atan(fabs(x/y))*(car.wheel_space)/2.0/(car.wheel_radius)/(car.rad_block); //turn_block定义的是int，小数结果直接取整数部分
			else //目标在后方，atan结果在-pi/2~pi/2，因此+pi
				turn_block = (PI-atan(fabs(x/y)))*(car.wheel_space)/2.0/(car.wheel_radius)/(car.rad_block);
		}	
		else if (y == 0) //如果目标在x轴上，小车转90度，计算电机旋转格数
			turn_block = (PI/2.0)*(car.wheel_space)/2.0/(car.wheel_radius)/(car.rad_block);
		else if ((x == 0)&&(y < 0)) //如果目标在x负半轴上，小车转180度，计算电机旋转格数
					turn_block = (PI/1.0)*(car.wheel_space)/2.0/(car.wheel_radius)/(car.rad_block);
							
		//开始旋转
		if (x >= 0) //如果目标在小车右侧，则小车顺时针转（左轮向前转，右轮向后转）
			run(turn_block, 1, turn_block, 0);
		else    //目标在小车左侧，小车逆时针转（左轮向后转，右轮向前转）
			run(turn_block, 0, turn_block, 1);	

		while(car.is_run); //等待小车旋转完成

		//-------------直走------------
		turn_block = (r - SAFE_DISTANCE)/(car.wheel_radius)/(car.rad_block);
		run(turn_block, 1, turn_block, 1);	
		while(car.is_run); //等待小车前进完成
	}
}
