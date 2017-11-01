#include "beep.h"
#include "math.h"
#include "stdlib.h"

struct SoundSense sound_sense;
////////////////////////////////////拾音器及运算函数////////////////////////////////////
//-------------拾音器初始化函数------------------
void init_sound_pick(struct SoundPick *sound_pick, float delay, int vals_length, u8 adc_channel, float *thres)
{
	sound_pick->delay = delay;
	sound_pick->vals_length = vals_length;
	sound_pick->adc_channel = adc_channel;
	sound_pick->mean = 0.0;
	sound_pick->sound_appear_flag = 0;
	sound_pick->sound_appear_time = -1.0;
	sound_pick->sum = 0.0;
	sound_pick->thres = *thres;
}

//--------该函数返回输入数组开始的MODEL_LENGTH长度数据与model对齐做相关的结果------------
float point_correlation(float * vals_cal)
{
	int i;
	float correlation_temp = 0;  //计算到某点时的相关结果
	for(i = 0; i < MODEL_LENGTH; i++)
		correlation_temp += (*(vals_cal+i)) * sound_sense.model[i];
	return correlation_temp;
}

//--------------该函数判断当前位置（i）是否出现声音------------------
u8 is_sound_appear(float * vals_cal, int i, struct SoundPick *sound_pick) //检测当模版序列的首部移动至vals_cal[i]处进行相关，是否超过阈值(有信号)
{
	return (fabs((point_correlation(vals_cal + i))) > (sound_pick->thres));
}


/*
//----------------------------检测方案1-----------------------------
//通过是否周期出现三个峰值确定信号是否出现，确认出现后找出声音出现时间点
void find_sound_point(struct SoundPick *sound_pick)
{
	int i=0, j=0, appear_point=0, sound_appear_flag=0, max_appear_point = 0;
	float corr_max = 0, temp;
	for(i = 0; i < sound_pick->vals_length-8*MODEL_LENGTH; i++ ) //从当前轮次起始点开始直到将模版平移vals_length（动态）长度，检测相关结果 是否超过阈值
	{		
		if(is_sound_appear(sound_pick->vals, i)) //出现第一个大于阈值的相关结果，疑似是信号，以下将向后继续检测三个大周期（脉冲信号周期）
		{
			appear_point = i;  //保存i的值，之后若确定是信号出现，则从i的位置向后找最大值位置即信号出现时间点
			if (!(is_sound_appear(sound_pick->vals, i+2*MODEL_LENGTH))) //向后两个model信号周期检测相关结果小于阈值
			{
				for (j = i+4*MODEL_LENGTH; j < i+5*MODEL_LENGTH; j++ )  //从第一个超过阈值位置向后一个脉冲周期的位置开始 检测相关结果在一个model周期的长度内是否有超过阈值的点
				{
					if((is_sound_appear(sound_pick->vals, j))) //在第一个大于阈值的位置后一个脉冲周期附近出现大于阈值的相关结果（第二个）
					{
						sound_appear_flag = 1;
						break;
					}
				}
				if(sound_appear_flag)
					break;
			}
		}
	}
  //信号出现，在第一个大于阈值的位置后找一个声音脉宽长度内的最大值，对应位置就是声音出现时刻
	if(sound_appear_flag)
	{
		for(i = appear_point; i <= appear_point+MODEL_LENGTH; i++)
		{
			temp = point_correlation(sound_sense.sound_picks[0].vals+i);
			if (temp > corr_max)
			{
				corr_max = temp; 
				max_appear_point = i;
			}			
		}
		//sound_pick->sound_appear_time = (float)((sound_sense.now_time)*(sound_pick->vals_length) + max_appear_point)/((float)(sound_sense.sample_frequency));
		sound_pick->sound_appear_time = max_appear_point; //test
		sound_pick->sound_appear_flag = 1;
	}
}
*/

//-----------------------------检测方案2-----------------------------
//通过检测相关结果中是否出现多个（3个）大于阈值且聚集在一起的值，确定信号是否出现，确认出现后找出声音出现时间点------------
void find_sound_point_2(struct SoundPick *sound_pick)
{
	int count = 0, max_appear_point = 0, i, j;
	float corr_max, temp = 0.0;
	for(i = 0; i < sound_pick->vals_length - MODEL_LENGTH; i++) //从当前轮次起始点开始直到将模板平移vals_length长度，检测相关结果是否超过阈值
	{		
		if(is_sound_appear(sound_pick->vals, i, sound_pick)) //出现第一个大于阈值的相关结果，疑似是信号
		{
			for(j = i; j < i+MODEL_LENGTH/2; j++ ) //从第一个大于阈值的位置向后10个点检测
			{
				if(is_sound_appear(sound_pick->vals, j, sound_pick)) 
					count++;
			}
			if (count >= 3) //检测出至少3个超过阈值的相关值，确定目标信号出现
			{
				sound_pick->sound_appear_flag = 1;
				//寻找目标信号出现时间点：在第一个大于阈值的位置后找一个声音脉宽长度内的最大值，对应位置就是声音出现时刻
				for(j = i; j < i+MODEL_LENGTH; j++)
				{
					temp = point_correlation(sound_pick->vals+j);
					if (temp > corr_max)
					{
						corr_max = temp; 
						max_appear_point = j;					
					}			
				}
				sound_pick->sound_appear_time = ((float)(max_appear_point))/(((float)(sound_sense.sample_frequency)));
				break;
			}
		}
	}
}
				
//test: 打印采样值及相关结果
void vals_printf(struct SoundPick *sound_pick){
	int j;
	/*
	//输出采样值
	printf("\nsample = [");	
	for(i = 0; i < sound_pick->vals_length; i++)
		printf("%f,", sound_pick->vals[i]);		
	printf("];\n");
	*/
	//输出相关值
	printf("correlation = [");
	for(j = 0; j < sound_pick->vals_length-MODEL_LENGTH; j++)
		printf("%f,", point_correlation(sound_pick->vals+j));
	printf("];\n");	
	printf("sound_appear_time = %f\n\n", sound_pick->sound_appear_time);
}

////////////////////////////////////////////////////////////////////////////////////////

//////////////////////////////////////声音采样初始化////////////////////////////////////
//声音采样初始化函数
void init_sound_sense(int sound_pick_num, int sample_frequency, int duration, int signal_frequency)
{
	int i;
	sound_sense.sound_pick_num = sound_pick_num;
	sound_sense.sample_frequency = sample_frequency;
	sound_sense.duration = duration;
	sound_sense.signal_frequency = signal_frequency;
	sound_sense.points_num = duration * sample_frequency;
	sound_sense.now_count = 0;
	sound_sense.start_sense_flag = 0;
	sound_sense.start_cal_flag = 0;
	//生成做相关的参考序列
	for(i = 0; i < MODEL_LENGTH; i++){
		sound_sense.model[i] = sin(2 * PI * ((float)signal_frequency)/((float)sample_frequency) * i);
	}
}

////////////////////////////////////////////////////////////////////////////////////////

///////////////////////////////////////全局函数////////////////////////////////////////
//初始化模块
void init_beep()
{
	int i;
	float TH[SOUND_PICK_NUM] = {TH0, TH1, TH2, TH3, TH4};
	u8 adc_chs[16] = {ADC_Channel_0, ADC_Channel_1, ADC_Channel_2, ADC_Channel_3,
					  ADC_Channel_4, ADC_Channel_5, ADC_Channel_6, ADC_Channel_7,
					  ADC_Channel_8, ADC_Channel_9, ADC_Channel_10,ADC_Channel_11,
					  ADC_Channel_12,ADC_Channel_13,ADC_Channel_14,ADC_Channel_15};
	Adc_Init();
					  
	//初始化SoundSense
	init_sound_sense(SOUND_PICK_NUM, SAMPLE_FREQUENCY, SAMPLE_DURATION, SIGNAL_FREQUENCY);
	
	//初始化拾音器
	for(i=0; i < sound_sense.sound_pick_num; i++)
		init_sound_pick(sound_sense.sound_picks+i, 0, sound_sense.points_num, adc_chs[i], TH+i);
	
	//初始化采样定时器
	TIM3_Int_Init(1000/(SAMPLE_FREQUENCY)-1,84-1);
}

//开始采样
void start_sound_sense()
{
	sound_sense.start_sense_flag = 1;
}


//等待采集结果，并计算结果
u8 sound_sense_wait_cal()
{
	int i;
	u8 temp = 1; //用于各拾音器收到声音标志位做与的临时量
	
	while(!sound_sense.start_cal_flag); //等待定时器采完一轮把start_cal_flag置位然后继续执行		
	
	//计算该拾音器声音出现的时间点
	for(i = 0; i < sound_sense.sound_pick_num; i++)
	{
		find_sound_point_2(sound_sense.sound_picks+i);	
		temp = temp && sound_sense.sound_picks[i].sound_appear_flag;
	}
	
	//算完复位
	sound_sense.start_cal_flag = 0;
	for(i = 0; i < sound_sense.sound_pick_num; i++)
	{		
		sound_sense.sound_picks[i].sound_appear_flag = 0;
	}
	
	//test: 结果输出
	delay_ms(2000);
	for(i = 0; i < SOUND_PICK_NUM; i++)
	{
		printf("sound_pick_%d\n", i);
		vals_printf(sound_sense.sound_picks+i);
	}
	
	return temp;
}

//把声音信号相对到达时间转换为相对距离
float* sound_arrival_time_to_distance()
{
	int i;
	for(i = 0; i < sound_sense.sound_pick_num; i++)
	{
		sound_sense.distances[i] = ((sound_sense.sound_picks+i)->sound_appear_time)*(SOUND_VELOCITY);
		sound_sense.sound_picks[i].sound_appear_time = -1;
		printf("\ndistances_i = %f", sound_sense.distances[i]); //test
	}
	
	return sound_sense.distances;
}

//采样定时器3中断服务函数
void TIM3_IRQHandler(void)
{
	int i;
	if(TIM_GetITStatus(TIM3,TIM_IT_Update) == SET) //溢出中断
	{
		if(sound_sense.start_sense_flag){
			if(sound_sense.now_count < sound_sense.points_num){
				//ADC采样
				for(i = 0; i < SOUND_PICK_NUM; i++){
						sound_sense.sound_picks[i].vals[sound_sense.now_count] = (double)Get_Adc(sound_sense.sound_picks[i].adc_channel)/4096*3.3;
						//sound_sense.sound_picks[i].sum += sound_sense.sound_picks[i].vals[sound_sense.now_count];
				}
				sound_sense.now_count++;
			}else{
				//采集完毕
				//先求各个拾音器该轮采样值的平均值（偏置），再重新置0等待下一轮计算
				/*
				for(i = 0; i < sound_sense.sound_pick_num; i++){
						sound_sense.sound_picks[i].mean = sound_sense.sound_picks[i].sum / sound_sense.sound_picks[i].vals_length;
						sound_sense.sound_picks[i].sum = 0.0;
				}
				*/
				sound_sense.now_count = 0; //下一个采样值从头重写vals
				sound_sense.start_sense_flag = 0;//停止采样
				sound_sense.start_cal_flag = 1; //使主函数可以开始计算相关
			}
		}
	}
	TIM_ClearITPendingBit(TIM3,TIM_IT_Update);  //清除中断标志位
}
///////////////////////////////////////////////////////////////////////////////////////
