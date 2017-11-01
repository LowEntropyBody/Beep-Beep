#ifndef __RUN_H
#define __RUN_H

#include "sys.h"
#include "beep.h"

/////////////////////////////////////////常数/////////////////////////////////////////
#define SAFE_DISTANCE 20 ////安全距离，小车在目标这个距离处停下，单位：cm
#define GRID_ROUND  200 //电机运行一圈的格数，为200
#define WHEEL_SPACE 49.2 //小车驱动轮间距，单位：cm
#define WHEEL_RADIUS 4.777 //小车驱动轮半径，单位：cm
#define RAD_BLOCK (0.01*PI)  //以弧度表示每一个转弯单位block（0.005圈即1.8度）
#define SPEED_MAX 10	//最大前进速度，单位：cm/s
#define SAFE_DISTANCE 20 //安全距离，小车距离目标这个长度时停下
#define RUN_TIME_UP 2000; //下次接收信号时间，即此次小车运行时间，单位：ms

///////////////////////////////////////PWM波参数////////////////////////////////////////
#define FREQ 200 //PWM波的频率
#define DUTY 10 //PWM波的占空比1/DUTY(占空比指光耦驱动导通的时间比)

///////////////////////////////////////转向参数/////////////////////////////////////////
#define LEFT 0
#define RIGHT 1

//////////////////////////////////////引脚利用声明//////////////////////////////////////
#define PULSE_LEFT PCout(7) //左轮速度控制信号，接左轮电机驱动的PUL-
#define PULSE_RIGHT PCout(8) //右轮速度控制信号，接右轮电机驱动的PUL-

//Ex_NVIC_Config专用定义
#define GPIO_A 0
#define GPIO_B 1
#define GPIO_C 2
#define GPIO_D 3
#define GPIO_E 4
#define GPIO_F 5
#define GPIO_G 6 
#define GPIO_H 7
#define GPIO_I 8
#define FTIR   1  //下降沿触发
#define RTIR   2  //上升沿触发

/**
*小车尺寸参数结构体
**/
struct Wheel
{
	int edge_count; //产生PWM波时的计数变量
	int edge; //产生PWM波的计数上限	
	int freq; //PWM波的频率,单位Hz
	int duty; //PWM波的占空比1/duty
	int count; //用于产生一定占空比PWM波的计数变量
};

struct Car
{
	struct Wheel wheel[2]; //指向左轮/右轮控制变量的指针
	u8 is_change; //为避免轮速度发生变化时的竞争冒险设置的锁变量
	u8 is_run; //指示小车是否完成上一轮运动的标志位，1表示未完成，0表示已完成
	int grid_round; //电机运行一圈的格数
	float wheel_space; //小车驱动轮间距，单位：cm
	float wheel_radius; //小车驱动轮半径，单位：cm
	float rad_block; //以弧度表示每一个转弯单位block
	int speed_max; //小车最大前进速度，单位：cm/s	
};

void init_run(void);
void track(float x, float y);
//test
void run(int block_left, u8 dir_left, int block_right, u8 dir_right);
extern struct Car car; 

#endif
