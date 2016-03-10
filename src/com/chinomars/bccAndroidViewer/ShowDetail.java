package com.chinomars.bccAndroidViewer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import com.chinomars.bccAndroidViewerCommon.Common;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Created by Chino on 3/8/16.
 */
public class ShowDetail extends Activity
{
//    public static String SPP_UUID = "00001101-0000-1000-8000-00805F9B34FB";
    private InputStream mmInStream;
    private OutputStream mmOutStream;
    ScrollView svResult;

    Button btnExit, btnAll;
    Button btn02, btn03, btn04, btn05, btn06, btn07, btn08, btn09, btn10,
            btn11;
    TextView tvTitle, tvLog;

    BluetoothAdapter btAdapt = null;
    BluetoothSocket btSocket = null;

    Boolean bConnect = false;
    String strName = null;
    String strAddress = null;
    int nNeed = 0;
    byte[] bRecv = new byte[1024];
    int nRecved = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.detail);

        svResult = (ScrollView) findViewById(R.id.svResult);

        btnExit = (Button) this.findViewById(R.id.btnExit);
        btnExit.setOnClickListener(new ClickEvent());
        btnAll = (Button) this.findViewById(R.id.btnAll);
        btnAll.setOnClickListener(new ClickEvent());

        btn02 = (Button) this.findViewById(R.id.btn02);
        btn02.setOnClickListener(new ClickEvent());
        btn02.setId(0);
        btn03 = (Button) this.findViewById(R.id.btn03);
        btn03.setOnClickListener(new ClickEvent());
        btn03.setId(0);
        btn04 = (Button) this.findViewById(R.id.btn04);
        btn04.setOnClickListener(new ClickEvent());
        btn04.setId(0);
        btn05 = (Button) this.findViewById(R.id.btn05);
        btn05.setOnClickListener(new ClickEvent());
        btn05.setId(0);
        btn06 = (Button) this.findViewById(R.id.btn06);
        btn06.setOnClickListener(new ClickEvent());
        btn06.setId(0);
        btn07 = (Button) this.findViewById(R.id.btn07);
        btn07.setOnClickListener(new ClickEvent());
        btn07.setId(0);
        btn08 = (Button) this.findViewById(R.id.btn08);
        btn08.setOnClickListener(new ClickEvent());
        btn08.setId(0);
        btn09 = (Button) this.findViewById(R.id.btn09);
        btn09.setOnClickListener(new ClickEvent());
        btn09.setId(0);
        btn10 = (Button) this.findViewById(R.id.btn10);
        btn10.setOnClickListener(new ClickEvent());
        btn10.setId(0);
        btn11 = (Button) this.findViewById(R.id.btn11);
        btn11.setOnClickListener(new ClickEvent());
        btn11.setId(0);

        tvTitle = (TextView) this.findViewById(R.id.tvTitle);
        tvLog = (TextView) this.findViewById(R.id.tvLog);
        Bundle bunde = this.getIntent().getExtras();
        strName = bunde.getString("NAME");
        strAddress = bunde.getString("MAC");
        tvTitle.setText(strName);
        tvLog.append(strName + "......\n");

        btAdapt = BluetoothAdapter.getDefaultAdapter();
        if (btAdapt == null) {
            tvLog.append("本机无蓝牙，连接失败\n");
            finish();
            return;
        }

        if (btAdapt.getState() != BluetoothAdapter.STATE_ON) {
            tvLog.append("本机蓝牙状态不正常，连接失败\n");
            finish();
            return;
        }

        IntentFilter intent = new IntentFilter();
        intent.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        intent.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);

        registerReceiver(connectDevices, intent);

        setButtonEnable(false);
        mHandler.sendEmptyMessageDelayed(Common.MESSAGE_CONNECT, 1000);
    }

    public void setButtonColor(Button btn, Boolean bOn) {
        if (bOn) {
            btn.setTextColor(Color.GREEN);
            btn.setId(1);
        } else {
            btn.setTextColor(Color.BLACK);
            btn.setId(0);
        }
    }

    public void setButtonEnable(Boolean bOn) {
        if (bOn) {
            btn02.setEnabled(true);
            btn03.setEnabled(true);
            btn04.setEnabled(true);
            btn05.setEnabled(true);
            btn06.setEnabled(true);
            btn07.setEnabled(true);
            btn08.setEnabled(true);
            btn09.setEnabled(true);
            btn10.setEnabled(true);
            btn11.setEnabled(true);
            btnAll.setEnabled(true);
        } else {
            btn02.setEnabled(false);
            btn03.setEnabled(false);
            btn04.setEnabled(false);
            btn05.setEnabled(false);
            btn06.setEnabled(false);
            btn07.setEnabled(false);
            btn08.setEnabled(false);
            btn09.setEnabled(false);
            btn10.setEnabled(false);
            btn11.setEnabled(false);
            btnAll.setEnabled(false);
        }
    }

    // Hander
    public final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Common.MESSAGE_CONNECT:
                    new Thread(new Runnable() {
                        public void run() {
                            InputStream tmpIn;
                            OutputStream tmpOut;
                            try
                            {
                                UUID uuid = UUID.fromString(Common.SPP_UUID);
                                BluetoothDevice btDev = btAdapt
                                        .getRemoteDevice(strAddress);
                                btSocket = btDev
                                        .createRfcommSocketToServiceRecord(uuid);
                                btSocket.connect();
                                tmpIn = btSocket.getInputStream();
                                tmpOut = btSocket.getOutputStream();
                            }
                            catch (Exception e)
                            {
                                Log.d(Common.TAG, "Error connected to: "
                                        + strAddress);
                                bConnect = false;
                                mmInStream = null;
                                mmOutStream = null;
                                btSocket = null;
                                e.printStackTrace();
                                mHandler.sendEmptyMessage(Common.MESSAGE_CONNECT_LOST);
                                return;
                            }

                            mmInStream = tmpIn;
                            mmOutStream = tmpOut;
                            mHandler.sendEmptyMessage(Common.MESSAGE_CONNECT_SUCCEED);
                        }

                    }).start();
                    break;
                case Common.MESSAGE_CONNECT_SUCCEED:
                    addLog("连接成功");
                    setButtonEnable(true);
                    bConnect = true;
                    new Thread(new Runnable() {
                        public void run() {
                            byte[] bufRecv = new byte[1024];
                            int nRecv = 0;
                            while (bConnect) {
                                try {
                                    Log.e(Common.TAG, "Start Recv" + String.valueOf(mmInStream.available()));
                                    nRecv = mmInStream.read(bufRecv);
                                    if (nRecv < 1) {
                                        Log.e(Common.TAG, "Recving Short");
                                        Thread.sleep(100);
                                        continue;
                                    }
                                    System.arraycopy(bufRecv, 0, bRecv, nRecved, nRecv);
                                    Log.e(Common.TAG, "Recv:" + String.valueOf(nRecv));
                                    nRecved += nRecv;
                                    if(nRecved < nNeed)
                                    {
                                        Thread.sleep(100);
                                        continue;
                                    }

                                    mHandler.obtainMessage(Common.MESSAGE_RECV,
                                            nNeed, -1, null).sendToTarget();

                                } catch (Exception e) {
                                    Log.e(Common.TAG, "Recv thread:" + e.getMessage());
                                    mHandler.sendEmptyMessage(Common.MESSAGE_EXCEPTION_RECV);
                                    break;
                                }
                            }
                            Log.e(Common.TAG, "Exit while");
                        }
                    }).start();
                    break;
                case Common.MESSAGE_EXCEPTION_RECV:
                case Common.MESSAGE_CONNECT_LOST:
                    addLog("连接异常，请退出本界面后重新连接");
                    try {
                        if (mmInStream != null)
                            mmInStream.close();
                        if (mmOutStream != null)
                            mmOutStream.close();
                        if (btSocket != null)
                            btSocket.close();
                    } catch (IOException e) {
                        Log.e(Common.TAG, "Close Error");
                        e.printStackTrace();
                    } finally {
                        mmInStream = null;
                        mmOutStream = null;
                        btSocket = null;
                        bConnect = false;
                        setButtonEnable(false);
                    }
                    break;
                case Common.MESSAGE_WRITE:

                    break;
                case Common.MESSAGE_READ:

                    break;
                case Common.MESSAGE_RECV:
                    Boolean bOn = false;
                    String strRecv = bytesToString(bRecv, msg.arg1);
                    addLog("接收数据: " + strRecv);

                    if (msg.arg1 == 9) {
                        if (strRecv.indexOf("OK+Set:") != 0) {
                            addLog("接收数据错误" + String.valueOf(strRecv.indexOf("OK+Set:")));
                            return;
                        }
                        if (strRecv.charAt(strRecv.length() - 1) == '1') {
                            bOn = true;
                        }
                        switch (strRecv.charAt(strRecv.length() - 2)) {
                            case '2':
                                setButtonColor(btn02, bOn);
                                break;
                            case '3':
                                setButtonColor(btn03, bOn);
                                break;
                            case '4':
                                setButtonColor(btn04, bOn);
                                break;
                            case '5':
                                setButtonColor(btn05, bOn);
                                break;
                            case '6':
                                setButtonColor(btn06, bOn);
                                break;
                            case '7':
                                setButtonColor(btn07, bOn);
                                break;
                            case '8':
                                setButtonColor(btn08, bOn);
                                break;
                            case '9':
                                setButtonColor(btn09, bOn);
                                break;
                            case 'A':
                                setButtonColor(btn10, bOn);
                                break;
                            case 'B':
                                setButtonColor(btn11, bOn);
                                break;
                        }
                    } else // nLength == 17
                    {
                        if (strRecv.indexOf("OK+PIO:") != 0) {
                            addLog("接收数据错误" + String.valueOf(strRecv.indexOf("OK+PIO:")));
                            return;
                        }
                        for (int i = 7; i < 17; i++) {
                            bOn = false;
                            if (strRecv.charAt(i) == '1') {
                                bOn = true;
                            }
                            switch (i) {
                                case 7:
                                    setButtonColor(btn02, bOn);
                                    break;
                                case 8:
                                    setButtonColor(btn03, bOn);
                                    break;
                                case 9:
                                    setButtonColor(btn04, bOn);
                                    break;
                                case 10:
                                    setButtonColor(btn05, bOn);
                                    break;
                                case 11:
                                    setButtonColor(btn06, bOn);
                                    break;
                                case 12:
                                    setButtonColor(btn07, bOn);
                                    break;
                                case 13:
                                    setButtonColor(btn08, bOn);
                                    break;
                                case 14:
                                    setButtonColor(btn09, bOn);
                                    break;
                                case 15:
                                    setButtonColor(btn10, bOn);
                                    break;
                                case 16:
                                    setButtonColor(btn11, bOn);
                                    break;
                            }
                        }
                    }
                    break;
                case Common.MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(),
                            msg.getData().getString(Common.TOAST),
                            Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    private BroadcastReceiver connectDevices = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(Common.TAG, "Receiver:" + action);
            if (action.equals(BluetoothDevice.ACTION_ACL_CONNECTED)) {
            } else if (action.equals(BluetoothDevice.ACTION_ACL_DISCONNECTED)) {

            }
        }
    };

    @Override
    protected void onDestroy() {
        this.unregisterReceiver(connectDevices);
        Log.e(Common.TAG, "Free detail");
        super.onDestroy();
    }

    /* DEMO版较为简单，在编写您的应用时，请将此函数放到线程中执行，以免UI不响应 */
    public void send(Button btn, String strPio) {
        String strValue = "0";
        if (btn.getId() == 0)
            strValue = "1";
        if (!bConnect)
            return;
        try {
            if (mmOutStream == null)
                return;
            if (strPio.equals("?")) {
                nNeed = 17;
                nRecved = 0;
                mmOutStream.write(("AT+PIO" + strPio).getBytes());
                addLog("发送AT+PIO" + strPio);
            } else {
                nNeed = 9;
                nRecved = 0;
                mmOutStream.write(("AT+PIO" + strPio + strValue).getBytes());
                addLog("发送AT+PIO" + strPio + strValue);
            }
        } catch (Exception e) {
            Toast.makeText(this, "发送指令失败!", Toast.LENGTH_SHORT).show();
            return;
        }
    }

    // 按钮事件
    class ClickEvent implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            if (v == btnExit) {
                closeAndExit();
                return;
            } else if (v == btnAll) {
                // get status
                send(btnAll, "?");
            } else if (v == btn02) {
                send(btn02, "2");
            } else if (v == btn03) {
                send(btn03, "3");
            } else if (v == btn04) {
                send(btn04, "4");
            } else if (v == btn05) {
                send(btn05, "5");
            } else if (v == btn06) {
                send(btn06, "6");
            } else if (v == btn07) {
                send(btn07, "7");
            } else if (v == btn08) {
                send(btn08, "8");
            } else if (v == btn09) {
                send(btn09, "9");
            } else if (v == btn10) {
                send(btn10, "A");
            } else if (v == btn11) {
                send(btn11, "B");
            }
        }

    }

    public void closeAndExit() {
        if (bConnect) {
            bConnect = false;

            try {
                Thread.sleep(100);
                if (mmInStream != null)
                    mmInStream.close();
                if (mmOutStream != null)
                    mmOutStream.close();
                if (btSocket != null)
                    btSocket.close();
            } catch (Exception e) {
                Log.e(Common.TAG, "Close error...");
                e.printStackTrace();
            }
        }
        finish();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            closeAndExit();
            return true;
        } else {
            return super.onKeyDown(keyCode, event);
        }
    }

    public static String bytesToString(byte[] b, int length) {
        StringBuffer result = new StringBuffer("");
        for (int i = 0; i < length; i++) {
            result.append((char) (b[i]));
        }

        return result.toString();
    }

    public void addLog(String str) {
        tvLog.append(str + "\n");
        svResult.post(new Runnable() {
            public void run() {
                svResult.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });
    }
}
