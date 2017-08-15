package com.beep.cesl.bluetooth;
//导入类
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

import com.beep.cesl.beepandroid.R;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;

/**
 * 蓝牙辅助类
 * Demo in Activity: BluetoothHelper bluetoothHelper = new BluetoothHelper(this, handler);
 */
public class BluetoothHelper {
    //蓝牙状态字指示
    public static final int CONNECT_FAIL = 0;
    public static final int CONNECT_SUCCEED = 1;
    public static final int CONNECTING = 2;
    public static final int BEGIN_FAIL = 3;
    public static final int READ_FAIL = 4;
    public static final int DATA = 5;
    //蓝牙接收信息的字节数
    private static final byte LENGTH = 1;

    private Activity activity;
    private BluetoothAdapter bluetoothAdapter = null; //本设备（手机）的蓝牙适配器对象
    private ScanCallback scanCallback = null; //回调函数初始为空
    private BluetoothSocket btSocket = null; //未连接时蓝牙socket为空
    private Handler handler = null;

    public BluetoothHelper(Activity activity, Handler handler){
        this.activity = activity;
        this.handler = handler;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter(); //可先检查Adapter是否为null判断蓝牙是否可用
    }

    //打开蓝牙
    public void openBluetooth() {
        //调用isEnabled()方法判断当前蓝牙设备是否可用
        if(bluetoothAdapter!=null) {
            if (!bluetoothAdapter.isEnabled()) {
                //如果蓝牙设备不可用的话,创建一个intent对象,该对象用于启动一个Activity,提示用户启动蓝牙适配器
                //Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                //activity.startActivityForResult(intent, BLUETOOTH_RESULT);
                /*
                 * 无提示开启蓝牙
                 */
                bluetoothAdapter.enable();//直接调用函数enable()，静默打开蓝牙设备 ；
            }
        }else{
            Toast.makeText(activity, "对不起，您的设备不支持蓝牙",Toast.LENGTH_SHORT).show(); //对手机用户进行提示
        }
    }

    //定义接口通过callback传递设备扫描结果
    public interface ScanCallback {
        void run(BluetoothDevice device);
    }

    //停止扫描设备的时候把广播关闭
    private void stopScanDevices() {
        bluetoothAdapter.cancelDiscovery();
        activity.unregisterReceiver(bluetoothDiscoveryReceiver);
    }

    /*
        MainActivity中复写蓝牙扫描回调接口
        Demo :
        private BluetoothHelper.ScanCallback scanCallback = new BluetoothHelper.ScanCallback(){
            @Override
            public void run(BluetoothDevice device){
                Log.v(TAG , "zName : " + device.getName() + " Address: " + device.getAddress());
            }
        };
    bluetoothHelper.scanDevices(scanCallback);
    */
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
             String TAG = context.getString(R.string.TESTING);
             if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(intent.getAction())) {
                 Log.v(TAG, "### BT ACTION_DISCOVERY_STARTED ##");
             } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(intent.getAction())) {
                 Log.v(TAG, "### BT ACTION_DISCOVERY_FINISHED ##");
             } else if (BluetoothDevice.ACTION_FOUND.equals(intent.getAction())) {
                 Log.v(TAG, "### BT BluetoothDevice.ACTION_FOUND ##");
                 BluetoothDevice btDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                 if (btDevice != null && scanCallback != null)
                     scanCallback.run(btDevice);
             } else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(intent.getAction())) {
                 Log.v(TAG, "### BT ACTION_BOND_STATE_CHANGED ##");
                 int cur_bond_state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE);
                 int previous_bond_state = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.BOND_NONE);
                 Log.v(TAG, "### cur_bond_state ##" + cur_bond_state + " ~~ previous_bond_state" + previous_bond_state);
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

    /**设备配对
     * @param device
     * @return true 代表连接，false 代表绑定
     */
    public boolean bondDevice(BluetoothDevice device){
        if (device.getBondState() == BluetoothDevice.BOND_NONE) {
            //利用反射方法调用BluetoothDevice.createBond(BluetoothDevice remoteDevice);
            Method createBondMethod;
            try {
                createBondMethod = BluetoothDevice.class
                        .getMethod("createBond");
                Log.d("BlueToothTestActivity", "开始配对");
                createBondMethod.invoke(device);
            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                e.printStackTrace();
                Log.e("BOND", e.toString());
            }
            return false;
        }else if(device.getBondState() == BluetoothDevice.BOND_BONDED){
            connectDevice(device);
            return true; //若设备已配对则直接进行连接
        } else
            return false;
    }

    //设备连接
    private void connectDevice(BluetoothDevice device)  {
        final  BluetoothDevice device2 = device; //避免device被释放之后无法读取
        //为连接蓝牙设备开启线程
        Thread connectThread = new Thread(new Runnable() {
            @Override
            public void run() {
                setState(CONNECTING);
                BluetoothSocket socketTemp = null;
                // 固定的UUID
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
                        setState(CONNECT_SUCCEED);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e("CONNECT", e.toString());
                    setState(CONNECT_FAIL);
                }
            }
        });
        connectThread.start();
    }

     //与已连接设备断开连接
    public boolean exitConnect() {
        if(btSocket != null) {
            try {
                btSocket.close();
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                Log.e("EXIT", e.toString());
                return false;
            }
        }
        return false;
    }

    //用于蓝牙向已连接设备发送信息
     public boolean sendMessage(String str) {
         if(btSocket != null) //先检查是否已连接
         {
             OutputStream outStream;
             try {
                 outStream = btSocket.getOutputStream(); //通过向outStream写入字符对外发送
             } catch (IOException e) {
                 e.printStackTrace();
                 Log.e("SEND", e.toString());
                 return false;
             }
             byte[] msgBuffer = str.getBytes(); //逐字符发送，每发送一个停10ms,实际上一般只用到单字符
             /*
             *当用到多字符发送的时候
             * for (byte b : msgBuffer) {
                        outStream.write(b);
                        Thread.sleep(10);
                    }
             */
             try {
                 outStream.write(msgBuffer);//发送信息两步骤：写入、刷新
                 Thread.sleep(10); //保证成功写入
                 outStream.flush();
                 return true;  //发送成功返回true
             } catch (IOException e) {
                 e.printStackTrace();
                 Log.e("SEND", e.toString());
                 setState(BEGIN_FAIL);
                 return false; //否则返回false
             } catch (InterruptedException e) {
                 e.printStackTrace();
                 Log.e("SEND", e.toString());
                 return false; //无法sleep返回false
             }
         }else {
             Log.e("SEND", "Socket Connection Failed");
             return false; //若设备未连接返回false
         }
     }

    //用于蓝牙接收已连接设备发送过来的信息
     public void receiveMessage() {
         final Thread receiveThread = new Thread(new Runnable(){
             @Override
             public void run() {
                 //以下变量count用来确定信息是否接收完整
                 byte[] data = new byte[LENGTH]; //临时存放接收的字符
                 int count = 0; //实际接收到的信息长度
                 if(btSocket != null) {
                     while (true) {
                         try {
                             InputStream inputStream = btSocket.getInputStream(); //InputStream读取发送过来的流信息
                             Log.v("##RECEIVE##", "inputStream");
                             //while (count < LENGTH) {
                                 //count += inputStream.read(data, count, LENGTH - count); //保证接收信息的完整性
                             //}
                             while (count == 0) {
                                 count = inputStream.available();
                                 Log.v("##RECEIVE##", "waiting");
                                 Thread.sleep(1000);
                             }
                             byte[] bytes = new byte[count];
                             inputStream.read(bytes);

                             Log.v("##RECEIVE##", "bytes : "+Arrays.toString(bytes));

                             Message msg = handler.obtainMessage();
                             msg.what = DATA;
                             msg.obj = bytes;
                             //msg.obj = Arrays.toString(data);
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

    //用于写入程序状态（回调到主线程）
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
}