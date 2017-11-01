#include "location.h"
#include "stdlib.h"
#include "usart.h"

////////////////////////////////////初始化////////////////////////////////////

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
 
void init_coordinate(float *coordinate, float x, float y, float z)
{
	*coordinate = x;
	*(coordinate+1) = y;
	*(coordinate+2) = z;
}

///////////////////////////////////矩阵运算///////////////////////////////////

void array_array_square(float *arr, float (*square)[NUM_RECEIVER], float sum)
{
	int i, j;
	for(i = 0; i < NUM_RECEIVER; i++)
		for(j = 0; j < NUM_RECEIVER; j++)
		{
			square[i][j] = -(arr[i]*arr[j])/sum;
			if(i == j)
				square[i][j] += 1;					  
		}	
}

//矩阵转置: 4*3 矩阵A转置得到 3*4 矩阵B
void matrix_t (float (*A)[DIMENSION], float (*B)[NUM_RECEIVER])
{
	int i,j;
	for (i = 0; i < DIMENSION ; i++ )
		for ( j = 0; j < NUM_RECEIVER; j++)
			B[i][j] = A[j][i];
}

//矩阵乘法：3*4 矩阵A和 4*4 矩阵B相乘得到 3*4 矩阵C
void matrix34_matrix44_multiply (float (*A)[NUM_RECEIVER], float (*B)[NUM_RECEIVER], float (*C)[NUM_RECEIVER])
{
	int i, j, k;
	float temp;
	for (i = 0 ; i < DIMENSION ; i++ )
		for (j = 0 ; j < NUM_RECEIVER; j++)
			{
			temp = 0.0;
			for(k = 0; k < NUM_RECEIVER; k++)
				temp += A[i][k] * B[k][j];
			C[i][j] = temp;
			}
}

//矩阵乘法：3*4 矩阵A和 4*3 矩阵B相乘得到 3*3 矩阵C
void matrix34_matrix43_multiply (float (*A)[NUM_RECEIVER], float (*B)[DIMENSION], float (*C)[DIMENSION])
{
	int i, j, k;
	float temp;
	for (i = 0 ; i < DIMENSION ; i++ )
		for (j = 0 ; j < DIMENSION; j++)
			{
			temp = 0.0;
			for(k = 0; k < NUM_RECEIVER; k++)
				temp += A[i][k] * B[k][j];
			C[i][j] = temp;
			}
}

//矩阵乘法：3*4 矩阵A和 4*1 数组B相乘得到 3*1 数组C
void matrix34_array41_multiply(float (*A)[NUM_RECEIVER], float *B, float *C)
{
	int i, j;
	float temp;
	for(i = 0; i < DIMENSION; i++)
	{
		temp = 0.0;
		for(j = 0; j < NUM_RECEIVER; j++)
			temp += A[i][j] * B[j];
		C[i] = temp;
	} 
}

//矩阵乘法：3*3 矩阵A和 3*1 数组B相乘得到 3*1 数组C
void matrix33_array31_multiply(float (*A)[DIMENSION], float *B, float *C)
{
	int i, j;
	float temp;
	for(i = 0; i < DIMENSION; i++)
	{
		temp = 0.0;
		for(j = 0; j < DIMENSION; j++)
			temp += A[i][j] * B[j];
		C[i] = temp;
	} 
}

// 3*3 方阵求逆辅助函数1
float get_a(float A[DIMENSION][DIMENSION], int n)
{  
    int i, j, k; 
	float a = 0;
	float temp[DIMENSION][DIMENSION]; 
	float a_temp;	
	
    if(n == 1)    
        return A[0][0];  
	
    for(i = 0; i < n; i++)  
    {  
        for(j = 0; j < n - 1; j++)    
            for(k = 0; k < n - 1; k++)    
                temp[j][k] = A[j+1][(k>=i)? (k+1): k];
			
        a_temp = get_a(temp, n-1);  
		
        if(i%2 == 0)   
            a += A[0][i] * a_temp;    
        else    
            a -= A[0][i] * a_temp;    
    }  
    return a;  
} 
// 3*3 方阵求逆辅助函数2
void get_a_start(float A[DIMENSION][DIMENSION], float B[DIMENSION][DIMENSION], int n)
{   
    int i, j, k, t;  
    float temp[DIMENSION][DIMENSION];

    for(i = 0; i < n; i++)  
    {  
        for(j = 0;j < n; j++)  
        {  
            for(k = 0;k < n-1; k++)   
                for(t = 0; t < n-1; t++)   
                    temp[k][t] = A[(k>=i)? (k+1): k][(t>=j)? (t+1): t];     
            
            B[j][i] = get_a(temp, n-1);
			
            if((i+j)%2 == 1)          
                B[j][i] = -B[j][i];    
        }  
    }  
}  
// 3*3 方阵A求逆得到 3*3 方阵B
void matrix_inv (float A[DIMENSION][DIMENSION], float B[DIMENSION][DIMENSION])
{
	int i, j;
	float a = get_a(A, DIMENSION);
	if(a)
		get_a_start(A, B, DIMENSION);
		for(i = 0; i < DIMENSION; i++)   
			for(j = 0; j < DIMENSION; j++)  
			B[i][j] = B[i][j] / a;	
}

///////////////////////////////////计算坐标///////////////////////////////////

void location_cal(float * distances)
{
	int i;
	float coordinate[NUM_RECEIVER][DIMENSION];
	struct ValTemp valTemp;
	float result[DIMENSION] = {0};
	
	init_coordinate(*(coordinate), SIDE, SIDE, 10.0);
	init_coordinate(*(coordinate+1), -SIDE, SIDE, 10.0);
	init_coordinate(*(coordinate+2), -SIDE, -SIDE, 10.0);
	init_coordinate(*(coordinate+3), SIDE, -SIDE, 10.0);
	valTemp.d_distance_sum = 0.0;
	
	//目标坐标计算公式：((coordinate_t * d_square * d_square)* coordinate)inv * ((coordinate_t * d_square * d_square)* delta)
	
	for(i = 0; i < NUM_RECEIVER; i++)
	{
		valTemp.d_distance[i] = *(distances+i) - *(distances+4);
		valTemp.d_distance_square[i] = (valTemp.d_distance[i])*(valTemp.d_distance[i]);
		valTemp.d_distance_sum += valTemp.d_distance_square[i];
		valTemp.delta[i] = (coordinate[i][0])*(coordinate[i][0])+(coordinate[i][1])*(coordinate[i][1])		
						   +(coordinate[i][2])*(coordinate[i][2]) - valTemp.d_distance_square[i];	
	}
	
	//4*1 d_distance_t 和 1*4 d_distance 相乘加单位矩阵得到 4*4 d_square
	array_array_square(valTemp.d_distance, valTemp.d_square, valTemp.d_distance_sum);
	
	//4*3 coordinate 转置得到 3*4 coordinate_t
	matrix_t(coordinate, valTemp.coordinate_t);
		
	//3*4 coordinate_t 和 4*4 d_square 相乘得到 3*4 matrix_temp1
	matrix34_matrix44_multiply(valTemp.coordinate_t, valTemp.d_square, valTemp.matrix_temp1);
		
	//3*4 matrix_temp1 和 4*4 d_square 相乘得到 3*4 matrix_temp2, matrix_temp2后面计算还会继续用到
	matrix34_matrix44_multiply(valTemp.matrix_temp1, valTemp.d_square, valTemp.matrix_temp2);
		
	//3*4 matrix_temp2 和 4*3 coordinate 相乘得到 3*3 matrix_temp3
	matrix34_matrix43_multiply(valTemp.matrix_temp2, coordinate, valTemp.matrix_temp3);
			
	//3*4 matrix_temp2 和 4*1 delta 相乘得到 3*1 matrix_temp4
	matrix34_array41_multiply(valTemp.matrix_temp2, valTemp.delta, valTemp.matrix_temp4);
	
	//3*3 matrix_temp3 求逆得到 3*3 matrix_temp5	
	matrix_inv(valTemp.matrix_temp3, valTemp.matrix_temp5);
	
	//3*3 matrix_temp5 和 3*1 matrix_temp4 相乘得到 3*1 location_est
	matrix33_array31_multiply(valTemp.matrix_temp5, valTemp.matrix_temp4, valTemp.location_est);
	
	//最后再除以2得到目标坐标
	for(i = 0; i < DIMENSION; i++)
		result[i] = valTemp.location_est[i]/2;
	
	//test
	printf("\nresult = %f %f %f", result[0], result[1], result[2]);
}
