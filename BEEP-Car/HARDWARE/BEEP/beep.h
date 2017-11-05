#ifndef __BEEP_H
#define __BEEP_H
#include "sys.h"
#include "delay.h"
#include "timer.h"
#include "adc.h"
#include "usart.h"

//使用ADC1,定时器3
#define SOUND_PICK_NUM 5       //拾音器个数
#define SAMPLE_FREQUENCY 20     //ADC采集频率kHZ,需要被1000整除
#define SAMPLE_DURATION 150      //ADC采集持续时间，采集完做卷积判断声音到达时刻,ms
#define SIGNAL_FREQUENCY 4      //声音的频率
#define MODEL_PERIOD_NUM 10   //一个相关信号周期包含的正弦周期个数

#define MODEL_LENGTH ((SAMPLE_FREQUENCY)*(MODEL_PERIOD_NUM)/(SIGNAL_FREQUENCY)) //模板信号长度
#define PI 3.1415927       //圆周率 
#define SOUND_VELOCITY 34 //声速，单位：cm/ms

////相关结果超过阈值的时候认为声音信号出现（经验值）
#define CORRELATION_THRESHOLD 0.5 //test
#define TH0 3.0
#define TH1 1.0
#define TH2 0.8
#define TH3 1.0
#define TH4 5.0

/////////////////////////////////////////拾音器/////////////////////////////////////////
/**
*拾音器结构体
**/
struct SoundPick
{
	float delay;//拾音器自身延时，默认为0
	float vals[(SAMPLE_FREQUENCY)*(SAMPLE_DURATION)];//存取ADC采样数据的值
	float mean;//vals平均值
	int vals_length;//vals数组长度
	u8 adc_channel;//ADC通道
	u8 sound_appear_flag;//表示信号是否已经出现，1代表已经出现
	float sound_appear_time; //拾音器确定声音出现的时间点（第一轮采样开始为时间原点）,单位：ms
	float sum; //用于计算该轮采样值平均值（即偏置）的临时求和
	float thres; 
};
///////////////////////////////////////////////////////////////////////////////////////

/////////////////////////////////////////感知声音///////////////////////////////////////
/**
*拾音器组，感知声音出现，用于控制acd采集频率，采集时间，采集点数以及采集轮次
**/
struct SoundSense
{
	struct SoundPick sound_picks[SOUND_PICK_NUM];//拾音器组数据
	int sound_pick_num;//拾音器个数
	int sample_frequency;//ADC采集频率kHZ
	int signal_frequency;//声音的频率
	int duration;//ADC采集持续时间，采集完做卷积判断声音到达时刻,单位：ms
	int points_num;//每次采集一共采点个数points_num=duration*frequency;
	int now_count;//用于记录现在采到第几个点
	u8 start_sense_flag;//等于1开始采样，等于0停止采样
	u8 start_cal_flag;//等于1说明一轮采集完毕，可以开始计算卷积
	float model[MODEL_LENGTH]; //用于跟采样值做相关的参考序列
	float distances[SOUND_PICK_NUM]; //各个拾音器测到的声音信号相对到达时间
};
//////////////////////////////////////////////////////////////////////////////////////

//初始化模块
void init_beep(void);
//开始采样
void start_sound_sense(void);
//等待采集结果，并计算结果
u8 sound_sense_wait_cal(void);	
//相对时间转换为相对距离
float* sound_arrival_time_to_distance(void);

#endif
