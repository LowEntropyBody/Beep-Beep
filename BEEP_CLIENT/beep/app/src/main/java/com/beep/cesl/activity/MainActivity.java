package com.beep.cesl.activity;
//导入类
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.beep.cesl.PlaySound;
import com.beep.cesl.beepandroid.R;
import com.beep.cesl.bluetooth.BluetoothHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

//以下定义MainActivity
public class MainActivity extends AppCompatActivity {

    public static String TAG = "testing"; //用作程序测试时的消息标签
    private BluetoothHelper bluetoothHelper = null; //本程序用到的蓝牙辅助封装类

    private ArrayList<Map<String,Object>> mData = new ArrayList<>(); //mData用于存放当前发现的设备（名称、地址、状态）
    private ArrayList<BluetoothDevice> devices = new ArrayList<>();  //devices用于存储蓝牙扫描到的设备信息
    private SimpleAdapter adapter; //SimpleAdapter类把数据配到显示控件上
    private ListView listView; //显示当前设备的名称、地址、状态
    private Button beginStopButton; //连接后继续活动的开始按钮
    private Button exitButton; //退出连接的按钮

    PlaySound playSound = new PlaySound(); //声音播放类
    boolean isPlaySound = false; //声音播放标志位
    boolean isReceive = false; //接收信息标志位

    //创建Activity
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); //设置MainActivity的Layout

        listView = (ListView) findViewById(R.id.activity_main_device_list); //找到显示设备ListView的ID
        beginStopButton = (Button) findViewById(R.id.activity_main_begin_stop); //找到开始Button的ID
        exitButton = (Button) findViewById(R.id.activity_main_exit);

        beginStopButton.setOnClickListener(beginStopListener); //设置一个监听器绑定在开始Button上，定义见后面
        exitButton.setOnClickListener(exitListener); //设置一个监听器绑定在退出Button上，定义见后面
        listView.setOnItemClickListener(onItemClickListener); //设置另一个监听器绑定在listView上，定义见后面
        beginStopButton.setVisibility(View.INVISIBLE); //MainActivity刚开始时Button是不可见的
        exitButton.setVisibility(View.INVISIBLE);

        //以下：adapter把数据配到显示控件上
        adapter = new SimpleAdapter(this,mData,R.layout.device_list,
                new String[]{"device_list_name","device_list_address","device_list_state"},
                new int[]{R.id.device_list_name,R.id.device_list_address,R.id.device_list_state});
        listView.setAdapter(adapter);

        bluetoothHelper = new BluetoothHelper(this, handler); //引用BluetoothHelper类

        //遍历所有已配对的设备并添加到显示设备的list中（否则其不会出现在可用设备list中）
        ArrayList<BluetoothDevice> temps = bluetoothHelper.getBondedDevices();
        for(int i=0;i<temps.size();i++)
            addDeviceToList(temps.get(i));

        bluetoothHelper.openBluetooth(); //打开蓝牙
        bluetoothHelper.scanDevices(scanCallback); //打印扫描到的蓝牙设备，复写见下面
    }

    //把扫描到的设备添加到显示的设备list中
    private void addDeviceToList(BluetoothDevice device) {
        //如果设备已经存在于list中则不再重复添加
        for(int i=0;i<mData.size();i++) {
            if (device.getAddress().equals(mData.get(i).get("device_list_address"))) {
                return;
            }
        }
        //以下：得到设备的配对状态（未配对NONE，正在配对BONDING，已配对BONDED）
        Map<String,Object> item = new HashMap<>();
        item.put("device_list_name", device.getName());
        item.put("device_list_address", device.getAddress());
        if(device.getBondState() == BluetoothDevice.BOND_NONE) {
            item.put("device_list_state", "NONE");
        }else if(device.getBondState() == BluetoothDevice.BOND_BONDING) {
            item.put("device_list_state", "BONDING");
        }else if(device.getBondState() == BluetoothDevice.BOND_BONDED) {
            item.put("device_list_state", "BONDED");
        }else {
            return;
        }
        //添加新设备
        mData.add(item);
        devices.add(device);
        adapter.notifyDataSetChanged(); //如果适配器的内容改变则刷新item的内容
    }

    //连接设备后对显示的设备list进行更新
    private void updateDeviceToList(BluetoothDevice device, boolean isConnected) {
        //判断设备是否已经存在list中，isConn标识设备是否已连接
        for(int i=0;i<mData.size();i++){
            if (device.getAddress().equals(mData.get(i).get("device_list_address"))) {
                if(isConnected) {
                    mData.get(i).put("device_list_state", "CONNECTED");
                    listView.setEnabled(false); //若某设备已连接，则list中其它设备不能再点击
                    beginStopButton.setVisibility(View.VISIBLE);  //若某设备已连接，则Button可见
                    exitButton.setVisibility(View.VISIBLE);
                }
                else {
                    mData.get(i).put("device_list_state", "BONDED"); //若连接不成功则继续显示该设备状态为“已配对”
                }
                adapter.notifyDataSetChanged(); //如果适配器的内容改变则刷新item的内容
                return;
            }
        }
    }

    //通过Callback打印当前扫描到的设备并添加到list中
    private BluetoothHelper.ScanCallback scanCallback = new BluetoothHelper.ScanCallback() {
        @Override
        public void run(BluetoothDevice device) {
            Log.v(TAG , "zName : " + device.getName() + " Address: " + device.getAddress());
            addDeviceToList(device);
        }
    };

    //绑定在listView上的监听器
    private AdapterView.OnItemClickListener onItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Log.v(TAG, position+""); //测试时以字符形式打印click的list位置（0,1,2,...）
            updateDeviceToList(devices.get(position), bluetoothHelper.bondDevice(devices.get(position))); //若点击则进行配对并对list进行更新
        }
    };

    //绑定在退出Button上的监听器
    private View.OnClickListener exitListener = new View.OnClickListener() {
        @Override
        public void onClick(View w) {
            if(bluetoothHelper.exitConnect()) {
                listView.setEnabled(true);
                beginStopButton.setVisibility(View.INVISIBLE);  //若某设备退出连接，则Button不再可见
                exitButton.setVisibility(View.INVISIBLE);
                isReceive = false;
                for(int i=0;i<mData.size();i++){
                    if(mData.get(i).get("device_list_state").equals("CONNECTED"))
                        mData.get(i).put("device_list_state", "BONDED");
                }
            }
            adapter.notifyDataSetChanged(); //如果适配器的内容改变则刷新item的内容
        }
    };

    //绑定在开始Button上的监听器
    private View.OnClickListener beginStopListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Log.v(TAG, bluetoothHelper.sendMessage("z")+""); //点击开始Button则通过蓝牙发送内容为"z"的信息
            if(!isReceive) {
                bluetoothHelper.receiveMessage();
                Log.v("##RECEIVE##", "isReceive : "+isReceive);
                isReceive = true;
            }


            Thread playThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (isPlaySound) {
                        playSound.play();
                    }
                }
            });
            isPlaySound = true;
            playSound.start();
            playThread.start();
            Log.d("##SOUND##", "PlaySound");

        }
    };

    public final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            //根据handler回调的msg显示状态
            switch (msg.what) {
                case BluetoothHelper.CONNECT_FAIL:
                    Toast.makeText( MainActivity.this, "连接失败", Toast.LENGTH_LONG).show();
                    break;
                case BluetoothHelper.CONNECT_SUCCEED:
                    Toast.makeText( MainActivity.this, "连接成功", Toast.LENGTH_LONG).show();
                    break;
                case BluetoothHelper.CONNECTING:
                    Toast.makeText( MainActivity.this, "连接中......", Toast.LENGTH_LONG).show();
                    break;
                case BluetoothHelper.BEGIN_FAIL:
                    Toast.makeText( MainActivity.this, "开始失败", Toast.LENGTH_LONG).show();
                    break;
                case BluetoothHelper.READ_FAIL:
                    Toast.makeText( MainActivity.this, "接收失败", Toast.LENGTH_LONG).show();
                    break;
                case BluetoothHelper.DATA:
                    Toast.makeText(MainActivity.this, msg.obj + "", Toast.LENGTH_LONG).show();
                    break;
            }
        }
    };

    //Activity的结束
    @Override
    protected void onDestroy() {
        bluetoothHelper.onDestroy();
        playSound.stop();
        super.onDestroy();
    }
}