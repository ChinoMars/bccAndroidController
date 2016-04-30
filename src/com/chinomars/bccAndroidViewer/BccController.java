package com.chinomars.bccAndroidViewer;

/**
 * Created by Chino on 3/8/16.
 */

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import android.app.Dialog;
import android.graphics.Color;
import android.widget.*;

import com.chinomars.bccAndroidViewerCommon.Common;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
//import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

public class BccController extends Activity {
//    public static String SPP_UUID = "00001101-0000-1000-8000-00805F9B34FB";

    ListView lvBTDevices;
    ArrayAdapter<String> adtDevices;
    List<String> lstDevices = new ArrayList<String>();
    BluetoothAdapter btAdapt;
    public static BluetoothSocket btSocket;

    Button btnScan, btnConnect, btnAbout, btnLocalMAC;
    RadioGroup rdiogModeSet;

    int measureMode = Common.MEASURE_MODE_UNKNOW;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        // Button 设置
        btnScan = (Button) this.findViewById(R.id.btn_scan);
        btnScan.setOnClickListener(new ClickEvent());

        btnConnect = (Button) this.findViewById(R.id.btn_connect);
        btnConnect.setOnClickListener(new ClickEvent());

        btnAbout = (Button) this.findViewById(R.id.btn_about);
        btnAbout.setOnClickListener(new ClickEvent());

//        btnLocalMAC = (Button) this.findViewById(R.id.btn_localMAC);
//        btnLocalMAC.setOnClickListener(new ClickEvent());

        // Radio Button 设置
        rdiogModeSet = (RadioGroup) this.findViewById(R.id.rdiog_modeSet);
        rdiogModeSet.setOnCheckedChangeListener(new CheckedChangeEvent());


        lvBTDevices = (ListView) this.findViewById(R.id.lvDevices);
        adtDevices = new ArrayAdapter<>(BccController.this,
                android.R.layout.simple_list_item_1, lstDevices);
        lvBTDevices.setAdapter(adtDevices);
        lvBTDevices.setOnItemClickListener(new ItemClickEvent());

        btAdapt = BluetoothAdapter.getDefaultAdapter();

        if (!btAdapt.isEnabled()) {
            btAdapt.enable();
        }


        // 注册Receiver来获取蓝牙设备相关的结果
        IntentFilter intent = new IntentFilter();
        intent.addAction(BluetoothDevice.ACTION_FOUND); // 用BroadcastReceiver来取得搜索结果
        intent.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        intent.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(searchDevices, intent);

        addPairedDevice();

    }

    private BroadcastReceiver searchDevices = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if(action.equals(BluetoothDevice.ACTION_FOUND)) { //found device
                BluetoothDevice device = intent
                        .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String str = device.getName() + "|" + device.getAddress();

                if (lstDevices.indexOf(str) == -1)// 防止重复添加
                    lstDevices.add(str); // 获取设备名称和mac地址
                if (lstDevices.indexOf("null|" + device.getAddress()) != -1)
                    lstDevices.set(lstDevices.indexOf("null|" + device.getAddress()), str);
                adtDevices.notifyDataSetChanged();

            } else if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_STARTED)) {
                btnScan.setText("正在扫描");
                btnScan.setTextColor(Color.RED);

                btnConnect.setEnabled(false);
            } else if (action
                    .equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)) {
                btnScan.setTextColor(Color.BLACK);
                if(!btnConnect.isEnabled())
                    btnConnect.setEnabled(true);
                Toast.makeText(BccController.this, "扫描完成，选择工作模式后点击连接蓝牙", 3000).show();
            }
        }
    };

    private void addPairedDevice() { // 增加配对设备
        Set<BluetoothDevice> pairedDevices = btAdapt.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                String str = device.getName() + "|" + device.getAddress();
                lstDevices.add(str);
                adtDevices.notifyDataSetChanged();
            }
        }
    }


    @Override
    protected void onDestroy() {
//        btAdapt.disable();

        super.onDestroy();
        android.os.Process.killProcess(android.os.Process.myPid());
    }


    class ItemClickEvent implements AdapterView.OnItemClickListener
    {
        @Override
        public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
                                long arg3)
        {
            // 如果蓝牙还没开启
            if (btAdapt.getState() != BluetoothAdapter.STATE_ON)
            {
                Toast.makeText(BccController.this, "请开启蓝牙", 1000).show();
                return;
            }

            if (measureMode == Common.MEASURE_MODE_UNKNOW) {
                Toast.makeText(BccController.this, "请选择工作模式", 1000).show();
                return;
            }

            if (btAdapt.isDiscovering())
                btAdapt.cancelDiscovery();
            String str = lstDevices.get(arg2);
            if (str == null | str.equals(""))
                return;
            String[] values = str.split("\\|");
            String address = values[1];
            Log.e(Common.TAG, values[1]);
            try {
                Intent intMain = new Intent(getApplicationContext(), ResultController.class);
                Bundle bd = new Bundle();
                bd.putString("NAME", values[0]);
                bd.putString("MAC", values[1]);
                bd.putInt("MODE", measureMode);
                intMain.putExtras(bd);
                startActivity(intMain);
            } catch (Exception e) {
                Log.d(Common.TAG, "Error connected to: " + address);
                e.printStackTrace();
            }

        }

    }

    // 菜单事件
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(Menu.NONE, Menu.FIRST + 1, 1, "退出系统").setIcon(
                android.R.drawable.ic_menu_close_clear_cancel);
        menu.add(Menu.NONE, Menu.FIRST + 2, 2, "关于我们").setIcon(
                android.R.drawable.ic_menu_help);

        return true;
    }

    public void ShowAbout() {
        new AlertDialog.Builder(this)
                .setTitle(Common.APP_NAME)
                .setMessage(Common.ABOUT_CONTENT)
                .setPositiveButton(Common.BTN_YES,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int which) {
                                 dialog.dismiss();
                            }
                        }).show();

    }

    // 菜单选项事件
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case Menu.FIRST + 1:
                finish();
                break;
            case Menu.FIRST + 2:
                ShowAbout();
                break;
        }
        return true;
    }

    // 按钮事件
    class ClickEvent implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            if (v == btnScan) { // 搜索设备
                btnConnect.setEnabled(false);
                if (btAdapt == null) {
                    Toast.makeText(BccController.this, "您的机器上没有发现蓝牙适配器，本程序将不能运行!", 1000).show();
                    return;
                }

                if (btAdapt.getState() != BluetoothAdapter.STATE_ON) { // bluetooth is down
                    Toast.makeText(BccController.this, "请先开启蓝牙", 1000).show();
                    return;
                }

                if (!btAdapt.isDiscovering()) {
                    lstDevices.clear();
                    addPairedDevice();
                    btAdapt.startDiscovery();
                }

            } else if (v == btnConnect) {
                if (measureMode == Common.MEASURE_MODE_UNKNOW) {
                    Toast.makeText(BccController.this, "请先选择工作模式", 1000).show();
                    return;
                }

                Iterator<String> iter = lstDevices.iterator();
                while (iter.hasNext()) {
                    String str = iter.next();
                    String[] values = str.split("\\|");
                    if (str == null || str.equals("")) {
                        continue;
                    }

                    if (values[1].equals(Common.BIND_BT_MAC)) {
                        Log.e(Common.TAG, values[1]);
                        try {
                            Intent intResult = new Intent(getApplicationContext(), ResultController.class);
                            Bundle bd = new Bundle();
                            bd.putString("NAME", values[0]);
                            bd.putString("MAC", values[1]);
                            bd.putInt("MODE", measureMode);
                            intResult.putExtras(bd);
                            startActivity(intResult);

                        } catch (Exception e) {
                            Toast.makeText(BccController.this, "蓝牙连接失败，请扫描蓝牙后重试", 1000).show();
                            Log.d(Common.TAG, "Error connected to: " + Common.BIND_BT_MAC);
                            e.printStackTrace();
                        }
                        break;
                    }
                }

            } else if(v == btnAbout) {
                ShowAbout();
            } else if (v == btnLocalMAC) {
                mShowLocalMAC();

            }
        }

    }

    public void mShowLocalMAC() {
        if (btAdapt == null) return;
        String macAddr = btAdapt.getAddress();
        new AlertDialog.Builder(this)
                .setTitle("Local MAC Address")
                .setIcon(android.R.drawable.ic_dialog_info)
                .setMessage(macAddr)
                .setPositiveButton("Yes",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        }).show();
    }


    // Radio Button Event
    class CheckedChangeEvent implements RadioGroup.OnCheckedChangeListener {
        @Override
        public void onCheckedChanged(RadioGroup group, int checkId) {
            if (checkId == R.id.rdio_bccMode) {
                measureMode = Common.MEASURE_MODE_BCC;
            } else if(checkId == R.id.rdio_gxcMode) {
                measureMode = Common.MEASURE_MODE_GXC;
            }
        }

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish();
            return true;
        } else {
            return super.onKeyDown(keyCode, event);
        }
    }

}