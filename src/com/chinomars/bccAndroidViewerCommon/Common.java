package com.chinomars.bccAndroidViewerCommon;

/**
 * Created by Chino on 3/8/16.
 */
public class Common
{
    // Commnunications Prtocol
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    public static final int MESSAGE_CONNECT = 6;
    public static final int MESSAGE_CONNECT_SUCCEED = 7;
    public static final int MESSAGE_CONNECT_LOST = 8;
    public static final int MESSAGE_RECV = 10;
    public static final int MESSAGE_EXCEPTION_RECV = 11;

    // C.P. plus
    public static final int SM_MEASURE = 12; // 发送协议
    public static final int RM_RESULT = 13; // 接收协议

    // Static Setting
    public static final String SPP_UUID = "00001101-0000-1000-8000-00805F9B34FB";
    public static final String BIND_BT_MAC = "30:14:12:26:11:35";
    public static final Integer WAITING_TIME = 1000;
    public static final Double MIN_N = 1.440; // mim n
    public static final Double MAX_N = 1.449; // max n

    // Measure mode
    public static final Integer MEASURE_MODE_UNKNOW = 0;
    public static final Integer MEASURE_MODE_BCC = 1;
    public static final Integer MEASURE_MODE_GXC = 2;
    public static final Integer MEASURE_RANGE_UNKNOWN = 0;
    public static final Integer MEASURE_RANGE_LONG = 1;
    public static final Integer MEASURE_RANGE_MID = 2;
    public static final Integer MEASURE_RANGE_SHORT = 3;

    // Erro Code
    public static final String TOAST = "toast";
    public static final String TAG = "BlueToothTool";

    // Static String Content
    public static final String APP_NAME = "臂长差测量软件";
    public static final String ABOUT_CONTENT = "请选择软件工作模式后再连接蓝牙:\n1.干涉型臂长差测量\n2.光线链路长度测量";
    public static final String BTN_YES = "确定";
}
