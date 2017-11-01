#ifndef _LOCATION_H
#define _LOCATION_H

#define DIMENSION 3 //三维定位包含x,y,z三个坐标值
#define NUM_RECEIVER 4 //一共4个接收信标加上位于中心的参考信标
#define SIDE 15.0 //信标坐标值，单位：cm

 /**         
 			    |<—— side ——>|
 O---------------------------O
 receiver_2     |			receiver_1
 			    |
 			    |
 			    |
 —————————————— O ————————————> y
 			    |Reference
 			    |
 			    |
 			    |
 O--------------|-------------O
 receiver3      |			receiver_0
               \/ x
 **/

//用于存放计算过程中产生中间量的结构体
struct ValTemp
{
	float d_distance[NUM_RECEIVER]; 
	float d_distance_square[NUM_RECEIVER];
	float d_distance_sum;
	float delta[NUM_RECEIVER];
	float d_square[NUM_RECEIVER][NUM_RECEIVER];
	float coordinate_t[DIMENSION][NUM_RECEIVER];
	float matrix_temp1[DIMENSION][NUM_RECEIVER];	
	float matrix_temp2[DIMENSION][NUM_RECEIVER];
	float matrix_temp3[DIMENSION][DIMENSION];
	float matrix_temp4[DIMENSION];
	float matrix_temp5[DIMENSION][DIMENSION];
	float location_est[DIMENSION];
};

void location_cal(float * distances);

#endif
