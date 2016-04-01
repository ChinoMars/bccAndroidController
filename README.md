# bccAndroidController
**中二光子实验室出品**

暂时还未取名叫啥。。。

名字想好了！！！叫做 **光线长度精密测量仪器控制软件**。。。

> 顺便贴个初版的软件贴图好了，顺便测试下七牛外链图片
![image](http://7xjhsi.com1.z0.glb.clouddn.com/image/githubreadme/S60323-193741.jpg?imageView2/2/w/500
)

##功能简介
- 用于通过蓝牙控制下位机器进行具体的测量工作
- 用于显示蓝牙上传的测量结果
- 用于保存历史测量数据

##开发进度
###20160310
- 完成Android app. UI设计
- 完成欢迎界面按键功能和蓝牙扫描和自动连接控制

###20160311
- resultController中蓝牙连接部分，简单通信协议
- 优化两处RadioButton，均改为RadioGroup

###20160313
- 程序引用不同版本jar包导致编译错误，已解决
- 文件名设置ui
- 数据保存ui，功能待完成

###20160314
- 蓝牙通信部分调试完毕，可以和通过usb连接蓝牙设备的电脑通过串口助手互发消息
- 文件名设置功能

###20160315
- 测试文件读取，实际数据保存和读取
- ~~文件保存出错，调试中 -> 已解决~~
- 文件读写完成 **并没有哈哈哈**

###20160316
- 增加`lib`：MPAndroidChart-2.0.9，Thank PhilJay for [PhilJay/MPAndroidChart](https://github.com/PhilJay/MPAndroidChart)
	
###20160317
- 某著名游戏公司面试挂归，今天我什么都不想干^^

###20160320
- 用户参数设置ui

###20160321
- 用户参数设置功能完善
- 新的数据文件读取功能
- 点击切换软件log和chart
- 保存数据到新的目录结构

###20160322
- 读取数据设置为listView方式选取来读 -> 调试完成
- 画图

###20160323
- 绘图功能解决无法绘制负数数据的问题
- 绘图配色调整
- ~~待解决问题：从文件读取绘图崩溃~~
- 绘图数据归一化
- ~~待解决问题：*seekbar*随折射率改变而改变~~

###20160324
- 待解决问题：
    - ~~发送和接收乱码~~
    - 更新绘图的频率：`canUpdateResult`相关

###20160328
- 发送接收乱码

###20160329
- 确定收发数据格式
- 数据发送模块
- 数据接收解析模块
- 自动开启蓝牙
    
###需求更新
####20160401
- 定时器：发送测量指令开始计时，超过T后报错“timeout”
- UI：测量进度指令，当前`时间开销/T*100%`

####20160315
- ~~SeekBar调节范围`1.4400`-`1.4700`~~
- ~~参数设置界面需包括~~
	- ~~操作人~~
	- ~~产品型号~~
	- ~~产品编号~~
	- ~~生产日期~~
	- ~~测量时间~~
	- ~~备注~~
- 文件保存形式
	- ~~同一个产品型号和编号新开一个路径~~
	- ~~文件名为：产品编号+3位编号~~
- 文件读取
	- ~~seekbar下方显示`操作人`和`测量时间`~~
	- ~~读取按钮之后需要显示存放路径，`让我想想怎么搞。。。`~~
