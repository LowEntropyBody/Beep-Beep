#ifndef _MSG_H
#define _MSG_H

#include "sys.h"

#define TIME 1000 //每隔TIME微秒检查一次串口蓝牙信息
#define RX_ERROR "e" //信息标志：请求重新发送声音
#define TEST "t" //信息标志：测试蓝牙连接
#define WORK "w" //信息标志：系统开始采样
#define BEGIN "b" //信息标志：系统开始工作

u8 isBeginWork(void);
void isWorkSet(u8 a);
void init_msg(void);

#endif
