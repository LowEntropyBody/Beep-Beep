package com.beep.cesl.activity;

import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.beep.cesl.PlaySound;
import com.beep.cesl.beepandroid.R;
import com.beep.cesl.bluetooth.BluetoothHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    public static String TAG = "testing"; //用作程序测试时的消息标签
    private BluetoothHelper bluetoothHelper = null; //本程序用到的蓝牙辅助封装类
    PlaySound playSound = new PlaySound(); //声音播放类

    private ArrayList<Map<String,Object>> mData = new ArrayList<>(); //mData用于存放当前发现的设备（名称、地址、状态）
    private ArrayList<BluetoothDevice> devices = new ArrayList<>();  //devices用于存储蓝牙扫描到的设备信息
    private SimpleAdapter adapter; //SimpleAdapter类把数据配到显示控件上
    private ListView listView; //显示当前设备的名称、地址、状态
    private Button beginStopButton; //连接后继续活动的开始按钮
    private Button exitButton; //退出连接的按钮
    private int deviceIndex; //用户上一次点击的设备序号
    private TextView tipsView; //提示栏文字显示

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN); //弹出输入法时布局不致于移位
        setContentView(R.layout.activity_main);

        listView = (ListView) findViewById(R.id.activity_main_device_list);
        beginStopButton = (Button) findViewById(R.id.activity_main_begin_stop);
        exitButton = (Button) findViewById(R.id.activity_main_exit);
        tipsView = (TextView) findViewById(R.id.tips);

        beginStopButton.setOnClickListener(beginListener);
        exitButton.setOnClickListener(exitListener);
        listView.setOnItemClickListener(onItemClickListener);
        tipsView.setTextSize(18);
        tipsView.setText("  Device not connected yet, waiting ...");
        beginStopButton.setVisibility(View.INVISIBLE); //MainActivity刚开始时Button是不可见的
        exitButton.setVisibility(View.INVISIBLE);

        //以下：adapter把数据配到显示控件上
        adapter = new SimpleAdapter(this, mData, R.layout.device_list,
                new String[]{"device_list_name", "device_list_address", "device_list_state"},
                new int[]{R.id.device_list_name, R.id.device_list_address, R.id.device_list_state});
        listView.setAdapter(adapter);

        bluetoothHelper = new BluetoothHelper(this, handler);

        //遍历所有已配对的设备并添加到显示设备的list中（否则其不会出现在可用设备list中）
        ArrayList<BluetoothDevice> temps = bluetoothHelper.getBondedDevices();
        for(int i = 0; i<temps.size(); i++)
            addDeviceToList(temps.get(i));

        bluetoothHelper.openBluetooth(); //打开蓝牙
        bluetoothHelper.scanDevices(scanCallback); //显示扫描到的蓝牙设备，复写见下面
    }

    //把扫描到的设备添加到显示的设备list中
    private void addDeviceToList(BluetoothDevice device) {
        //如果设备已经存在于list中则不再重复添加
        for(int i = 0; i < mData.size(); i++) {
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
    private void updateDeviceToList(BluetoothDevice device, String state) {
        //判断设备是否已经存在list中，然后更新连接状态
        for(int i = 0; i < mData.size(); i++){
            if (device.getAddress().equals(mData.get(i).get("device_list_address"))) {
                    mData.get(i).put("device_list_state", state);
            adapter.notifyDataSetChanged(); //如果适配器的内容改变则刷新item的内容
            return;
            }
        }
        tipsView.setText("  Device connected already");
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
            bluetoothHelper.bondDevice(devices.get(position));
            deviceIndex = position;
        }
    };

    //绑定在退出Button上的监听器
    private View.OnClickListener exitListener = new View.OnClickListener() {
        @Override
        public void onClick(View w) {
            bluetoothHelper.flag = BluetoothHelper.STOP;
            bluetoothHelper.exitConnect();
        }
    };

    //绑定在开始Button上的监听器
    private View.OnClickListener beginListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            //点击开始Button则启动定时器，通过蓝牙发送信息标志同时播放声音信号
            bluetoothHelper.flag = BluetoothHelper.BEGIN;
            tipsView.setText("  Begin ...");
        }
    };

    //退出Button执行操作
    private void exitActivity() {
        listView.setEnabled(true);
        beginStopButton.setVisibility(View.INVISIBLE);  //若某设备退出连接，则Button不再可见
        exitButton.setVisibility(View.INVISIBLE);
        mData.get(deviceIndex).put("device_list_state", "BONDED");
        adapter.notifyDataSetChanged(); //如果适配器的内容改变则刷新item的内容
    }

    public final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            //根据handler回调的msg显示状态信息
            switch (msg.what) {
                case BluetoothHelper.CONNECT_FAIL:
                    bluetoothHelper.displayToast("蓝牙连接失败，请重新连接");
                    updateDeviceToList(devices.get(deviceIndex), "BONDED");
                    listView.setEnabled(true);
                    beginStopButton.setVisibility(View.INVISIBLE);  //若某设备已连接，则Button可见
                    exitButton.setVisibility(View.INVISIBLE);
                    tipsView.setText("  Device connection fails, please connect again ...");
                    break;
                case BluetoothHelper.CONNECT_SUCCEED:
                    bluetoothHelper.displayToast("蓝牙连接成功，准备开始");
                    updateDeviceToList(devices.get(deviceIndex), "CONNECTED");
                    beginStopButton.setVisibility(View.VISIBLE);  //若某设备已连接，则Button可见
                    exitButton.setVisibility(View.VISIBLE);
                    listView.setEnabled(false); //若某设备已连接，则list中其它设备不能再点击
                    tipsView.setText("  Device connected already, waiting to begin ...");
                    break;
                case BluetoothHelper.CONNECTING:
                    bluetoothHelper.displayToast("连接中......");
                    tipsView.setText("  Device connecting, wait ...");
                    break;
                case BluetoothHelper.SEND_FAIL:
                    bluetoothHelper.displayToast("发送信息失败");
                break;
                case BluetoothHelper.READ_FAIL:
                    bluetoothHelper.displayToast("接收信息失败");
                    break;
                case BluetoothHelper.EXIT_SUCCEED:
                    exitActivity();
                    bluetoothHelper.displayToast("已退出连接");
                    break;
                case BluetoothHelper.EXIT_FAIL:
                    bluetoothHelper.displayToast("退出连接失败");
                    break;
                case BluetoothHelper.DATA:
                    bluetoothHelper.displayToast(msg.obj + "");
                    break;
                case BluetoothHelper.TESTING:
                    bluetoothHelper.testBlueTooth(); //收到蓝牙测试信息反馈就继续保持测试
                    break;
                case BluetoothHelper.WORKING:
                    bluetoothHelper.flag = BluetoothHelper.WORK;
                    bluetoothHelper.beginActivity();
                    break;
                case BluetoothHelper.PLAY:

                    break;
            }
        }
    };

    //Activity结束
    @Override
    protected void onDestroy() {
        bluetoothHelper.onDestroy();
        playSound.stop();
        super.onDestroy();
    }
}
