package com.chinomars.bccAndroidViewerCommon;

/**
 * Created by Chino on 3/8/16.
 */
public class Common {
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
    public static final int MESSAGE_SEND_COMMAND = 12; // 发送协议
    public static final int MESSAGE_RECV_RESULT = 13; // 接收协议

    // Static Setting
    public static final String SPP_UUID = "00001101-0000-1000-8000-00805F9B34FB";
    public static final String BIND_BT_MAC = "30:14:12:26:11:35";
    public static final int MIN_N = 14400; // mim n
    public static final int MAX_N = 14490; // max n
    public static final double SCALE = 10000; // based on the MAX_N and MIN_N
    public static final int RESULT_DATA_LEN = 806; // 待定 3 short int + 400 short int
    public static final int MAX_CURVE_LEN = 400;

    // Measure mode
    public static final Integer MEASURE_MODE_UNKNOW = 0;
    public static final Integer MEASURE_MODE_BCC = 1;
    public static final Integer MEASURE_MODE_GXC = 2;
    public static final Integer MEASURE_RANGE_UNKNOW = 0;
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
    public static final String BCC_MODE_TITLE = "干涉型臂长差测量软件";
    public static final String GXC_MODE_TITLE = "光线链路长度测量软件";
}
