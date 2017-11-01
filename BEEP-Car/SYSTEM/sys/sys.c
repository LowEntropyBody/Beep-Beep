#include "sys.h"  

//THUMB指令不支持汇编内联
//采用如下方法实现执行汇编指令WFI  
__asm void WFI_SET(void)
{
	WFI;		  
}
//关闭所有中断(但是不包括fault和NMI中断)
__asm void INTX_DISABLE(void)
{
	CPSID   I
	BX      LR	  
}
//开启所有中断
__asm void INTX_ENABLE(void)
{
	CPSIE   I
	BX      LR  
}
//设置栈顶地址
//addr:栈顶地址
__asm void MSR_MSP(u32 addr) 
{
	MSR MSP, r0 			//set Main Stack value
	BX r14
}

//GPIO引脚配置
/*
 (1)GPIO_Mode_AIN 模拟in 
 (2)GPIO_Mode_IN_FLOATING 浮空
 (3)GPIO_Mode_IPD 下拉 
 (4)GPIO_Mode_IPU 上拉
 (5)GPIO_Mode_Out_OD 开漏
 (6)GPIO_Mode_Out_PP 推挽
 (7)GPIO_Mode_AF_OD 
 (8)GPIO_Mode_AF_PP 
 GPIO_Speed_10MHz 10MHz 
 GPIO_Speed_2MHz 2MHz 
 GPIO_Speed_50MHz 50MHz
 */  















