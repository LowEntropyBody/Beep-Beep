package com.beep.cesl.bluetooth;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.beep.cesl.PlaySound;
import com.beep.cesl.activity.MainActivity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

/**
 * 蓝牙辅助类
 * Demo in Activity: BluetoothHelper bluetoothHelper = new BluetoothHelper(this, handler);
 */
public class BluetoothHelper {
    //蓝牙状态字指示
    public static final int CONNECT_FAIL = 0; //状态字: 蓝牙连接失败
    public static final int CONNECT_SUCCEED = 1; //状态字: 蓝牙连接成功
    public static final int CONNECTING = 2; //状态字: 蓝牙正在连接
    public static final int SEND_FAIL = 3; //状态字: 开始工作（发蓝牙）失败
    public static final int READ_FAIL = 4; //状态字: 蓝牙信息收取失败
    public static final int EXIT_SUCCEED = 5; //状态字: 蓝牙退出连接成功
    public static final int EXIT_FAIL = 6; //状态字: 蓝牙退出连接失败
    public static final int DATA = 7; //状态字: 蓝牙收到信息
    public static final int TESTING = 8; //状态字: 蓝牙正在测试收发
    public static final int PLAY = 9; //状态字： 请求重发声音
    public static final int WORKING = 10; //状态字： 收到反馈可以发声音

    //蓝牙接收和发送的信息标志
    private static final String ERROR = "e"; //信息标志：终端接收错误请求重新发送
    public static final String WORK = "w"; //信息标志：开始发声音和蓝牙
    private static final String TEST = "t"; //信息标志：测试蓝牙连接
    public static final String STOP = "s"; //信息标志：系统停止工作
    public static  final String BEGIN = "b"; //信息标志：开始工作

    private static final int TIME = 5; //等待标志位刷新的ms数

    private Activity activity;
    private BluetoothAdapter bluetoothAdapter = null; //本设备（手机）的蓝牙适配器对象
    private PlaySound playSound = new PlaySound(); //声音播放类
    private ScanCallback scanCallback = null; //回调函数初始为空
    private BluetoothSocket btSocket = null; //未连接时蓝牙socket为空

    private byte[] wave; //playSound生成的声音信号波形
    private Handler handler = null; //把信息传回MainActivity //生成正弦波声音序列
    private boolean isConnected = false; //蓝牙连接状态标志位
    private boolean isWork = false; //蓝牙工作状态标志位

    public String flag = null; //蓝牙发送的内容

    public BluetoothHelper(Activity activity, Handler handler){
        this.activity = activity;
        this.handler = handler;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter(); //可先检查Adapter是否为null判断蓝牙是否可用
    }

    //打开蓝牙
    public void openBluetooth() {
        //调用isEnabled()方法判断当前蓝牙设备是否可用
        if(bluetoothAdapter != null) {
            if(!bluetoothAdapter.isEnabled()) {
                //如果蓝牙设备不可用的话,创建一个intent对象,该对象用于启动一个Activity,提示用户启动蓝牙适配器
                //Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                //activity.startActivityForResult(intent, BLUETOOTH_RESULT);
                //无提示开启蓝牙
                bluetoothAdapter.enable();
            }
        }else{
            displayToast("对不起，您的设备不支持蓝牙");
        }
    }

    //定义接口通过callback传递设备扫描结果
    public interface ScanCallback {
        void run(BluetoothDevice device);
    }
    /*
        MainActivity中复写蓝牙扫描回调接口
        Demo :
        private BluetoothHelper.ScanCallback scanCallback = new BluetoothHelper.ScanCallback(){
            @Override
            public void run(BluetoothDevice device){
                Log.v(MainActivity.TAG , "zName : " + device.getName() + " Address: " + device.getAddress());
            }
        };
    bluetoothHelper.scanDevices(scanCallback);
    */

    //停止扫描设备的时候把广播关闭
    private void stopScanDevices() {
        bluetoothAdapter.cancelDiscovery();
        activity.unregisterReceiver(bluetoothDiscoveryReceiver);
    }

    public void scanDevices(ScanCallback scanCallback) {
        this.scanCallback = scanCallback;
        IntentFilter bluetoothDiscoveryFilter = new IntentFilter(); //广播信息过滤器
        bluetoothDiscoveryFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        bluetoothDiscoveryFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        bluetoothDiscoveryFilter.addAction(BluetoothDevice.ACTION_FOUND);
        bluetoothDiscoveryFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        activity.registerReceiver(bluetoothDiscoveryReceiver, bluetoothDiscoveryFilter); //注册广播接收器
        bluetoothAdapter.startDiscovery(); //开始扫描设备
    }
     //蓝牙扫描时的广播接收器,用于接收扫描到的设备
    private BroadcastReceiver bluetoothDiscoveryReceiver = new BroadcastReceiver() {
         @Override
         public void onReceive(Context context, Intent intent) {
             // TODO Auto-generated method stub
             if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(intent.getAction())) {
                 Log.v(MainActivity.TAG, "### BT ACTION_DISCOVERY_STARTED ###");
             } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(intent.getAction())) {
                 Log.v(MainActivity.TAG, "### BT ACTION_DISCOVERY_FINISHED ###");
             } else if (BluetoothDevice.ACTION_FOUND.equals(intent.getAction())) {
                 Log.v(MainActivity.TAG, "### BT BluetoothDevice.ACTION_FOUND ###");
                 BluetoothDevice btDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                 if (btDevice != null && scanCallback != null)
                     scanCallback.run(btDevice);
             } else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(intent.getAction())) {
                 Log.v(MainActivity.TAG, "### BT ACTION_BOND_STATE_CHANGED ###");
                 int cur_bond_state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE);
                 int previous_bond_state = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.BOND_NONE);
                 Log.v(MainActivity.TAG, "### cur_bond_state ###" + cur_bond_state + " ~~ previous_bond_state" + previous_bond_state);
             }
         }
     };

    //得到已配对的蓝牙设备
    public ArrayList<BluetoothDevice> getBondedDevices() {
        Set<BluetoothDevice> devices = bluetoothAdapter.getBondedDevices();
        ArrayList<BluetoothDevice> list = new ArrayList<>();
        if (devices.size() > 0) {
            for (BluetoothDevice bluetoothDevice : devices) {
                list.add(bluetoothDevice);
            }
        }
        return list;
    }

    //设备配对
    public void bondDevice(BluetoothDevice device){
        if (device.getBondState() == BluetoothDevice.BOND_NONE) {
            //利用反射方法调用BluetoothDevice.createBond(BluetoothDevice remoteDevice);
            Method createBondMethod;
            try {
                createBondMethod = BluetoothDevice.class
                        .getMethod("createBond");
                createBondMethod.invoke(device);
            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                e.printStackTrace();
                Log.e("BOND", e.toString());
            }
        }else if(device.getBondState() == BluetoothDevice.BOND_BONDED){
            connectDevice(device); //若设备已配对则直接进行连接
        }
    }

    //设备连接
    private void connectDevice(BluetoothDevice device)  {
        final  BluetoothDevice device2 = device; //避免device被释放之后无法读取
        Thread connectThread = new Thread(new Runnable() {
            @Override
            public void run() {
                setState(CONNECTING);
                BluetoothSocket socketTemp = null;
                final String SPP_UUID = "00001101-0000-1000-8000-00805F9B34FB";
                UUID uuid = UUID.fromString(SPP_UUID);
                try {
                    socketTemp = device2.createInsecureRfcommSocketToServiceRecord(uuid);
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e("CONNECT", e.toString());
                    try {
                        btSocket.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                        Log.e("CONNECT", e1.toString());
                    }
                }
                btSocket = socketTemp;
                try {
                    if (btSocket != null) {
                        btSocket.connect();
                        wave = playSound.start(4000, 100, 4, 10); //声音信号的频率(Hz)、持续时间(ms)、占空比(1/duty)、正弦周期个数
                        receiveBlueTooth(); //蓝牙连接成功便开始接收蓝牙信息
                        setState(CONNECT_SUCCEED);
                        isConnected = true;
                        flag = TEST;
                        sendBlueTooth(flag); //蓝牙连接成功便开始发送测试信息
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e("CONNECT", e.toString());
                    setState(CONNECT_FAIL);
                    isConnected = false;
                }
            }
        });
        connectThread.start();
    }

    //与已连接设备断开连接
    public void exitConnect() {
        Thread exitThread = new Thread(new Runnable() {
            @Override
            public void run() {
                //sendBlueTooth(flag); //test
                if(btSocket !=null)
                {
                    try {
                        btSocket.close();
                        isConnected = false;
                        isWork = false;
                        setState(EXIT_SUCCEED);
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e("EXIT", e.toString());
                        setState(EXIT_FAIL);
                    }
                }
                setState(EXIT_FAIL);
            }
        });
        exitThread.start();
    }

    //在蓝牙连接但没有工作期间向终端持续发送蓝牙测试标志，以保持蓝牙发送信息速度
    public void testBlueTooth(){
        final Thread testThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (isWork) {
                    try {
                        Thread.sleep(TIME);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                sendBlueTooth(flag);
            }
        });
        testThread.start();
    }

    private void sendBlueTooth(String str) {
        OutputStream out = null;
        str = str + "\r\n"; //串口接收信息须以\r\n结束
        if (btSocket != null) //先检查是否已连接
        {
            try {
                out = btSocket.getOutputStream(); //通过向out写入字符对外发送
            } catch (IOException e) {
                e.printStackTrace();
                Log.e("SEND", e.toString());
            }
            byte[] msgBuffer = str.getBytes();
            /*
             *当用到多字符发送的时候逐字符发送
             * for (byte b : msgBuffer) {
                  outStream.write(b);
                   Thread.sleep(TIME);
                }
             */
            try {
                assert out != null;
                out.write(msgBuffer);//发送信息：写入、刷新
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
                Log.e("SEND", e.toString());
                setState(SEND_FAIL);
            }
        }else {
            displayToast("请检查蓝牙连接");
        }
    }

    //用于蓝牙向已连接设备发送信息并播放声音
     public void beginActivity() {
         final Thread playThread = new Thread(new Runnable() {
             @Override
             public void run() {
                 isWork = true;
                 sendBlueTooth(flag);
                 playSound.play(wave);
             }
         });
         playThread.start();
     }

    //用于蓝牙接收已连接设备发送过来的信息
    private void receiveBlueTooth() {
         final Thread receiveThread = new Thread(new Runnable(){
             @Override
             public void run() {
                 //以下变量count,length用来确定信息是否接收完整
                 int count, length; //实际接收到的信息长度
                 if(btSocket != null) {
                     while (true) {
                         while (!isConnected){
                             try {
                                 Thread.sleep(TIME);
                             } catch (InterruptedException e) {
                                 e.printStackTrace();
                             }
                         }
                         try {
                             length = 0;
                             count = 0;
                             InputStream inputStream = btSocket.getInputStream(); //InputStream读取发送过来的流信息
                             while (length == 0) {
                                 Thread.sleep(TIME);
                                 length = inputStream.available();
                             }
                             byte[] data = new byte[length];
                             while (count < length) {
                                 count += inputStream.read(data, count, length - count); //保证接收信息的完整性
                             }
                             String character = new String(data);
                             Log.v(MainActivity.TAG, "received: " + character); //test
                             Message msg = handler.obtainMessage();
                             //根据终端蓝牙返回的信息标志进行反应
                             switch (character) {
                                 case TEST:
                                     msg.what = TESTING;
                                     break;
                                 case WORK:
                                     msg.what = WORKING;
                                     break;
                                 case ERROR:
                                     msg.what = PLAY;
                                     break;
                             }
                             handler.sendMessage(msg); //通过Handler把msg传回到主线程中处理
                         } catch (IOException e) {
                             setState(READ_FAIL);
                             Log.e("RECEIVE", e.toString());
                             break;
                         } catch (InterruptedException e) {
                             e.printStackTrace();
                         }
                     }
                 }
             }
         });
         receiveThread.start();
     }

    //用于传递程序状态（回调到主线程）
    private void setState(int state) {
        Message msg = handler.obtainMessage();
        msg.what = state;
        handler.sendMessage(msg);
    }

    /* 结束Activity，结束前依次停止扫描并关闭连接
     * Demo:
     * @Override
        protected void onDestroy() {
            bluetoothHelper.onDestroy();
            super.onDestroy();
        }
     */
     public void onDestroy() {
         stopScanDevices(); //关闭蓝牙广播
         //关闭蓝牙连接
         if(btSocket != null){
             try {
                 btSocket.close();
             } catch (IOException e) {
                 e.printStackTrace();
                 Log.e("DESTROY", e.toString());
             }
         }
     }

     //用于消息通知显示的函数
    public void displayToast(String str) {
        Toast.makeText(activity, str, Toast.LENGTH_LONG).show();
    }
}
