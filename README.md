## UtilsProject
  >这是一个工具类project，将开发过程中，多次用到的工具，如 Log,PreUtils.java ,showToast,ThreadUtils,RandomUtil,Acache,ArrayUtil,等打包成jar，避免每次开发都需要重新导入java文件，简化开发过程
  
> 主要介绍几个简单工具使用

>本工程里面部分java文件来自其他博客或github工程，具体来源已经难以找到，，sorry.....


在命令行使用 `gradlew makeJar` 即可编译出 jar，然后就可以导入到任意项目中使用了

### 1. Log 的使用
[基于这位仁兄的项目而来](https://github.com/pengwei1024/LogUtils)
主要是将他项目中多个类整合到一起，且对性能进行了一些优化,根据修改，性能比起原作者的提升了至少 2-3 倍（使用for循环一万次打印LOG，取平均时间）

1. 支持打印log一键跳转代码
2. 支持一键配置日志输出，方便调试发布时开启关闭日志
3. 支持设置日志输出等级
4. 支持保存日志文件
5. 支持配置日志文件保存路径
6. 支持全局tag
7. 与系统工具类Log api完全兼容，只需修改包名就可导入
8. 支持 List，Map，数组，Intent，bundle，等打印
9. 支持Object打印

up:
 10. 初始化时自动读取app是否为Debug或Release模式，根据两种模式，自动配置是否允许日志和文件输出，使用`Log.getLogConfig().appIsDebug()`查看是否处于debug模式。依旧支持手动配置。  

> Log.v(Object obj)或者Log.v(String tag,Object obj);工具会打印当前类执行行，其余等级与系统的android.util.log一致，只需要修改导包，就可以迅速移植。

<pre> 
isDebug = true;
//日志文件输出等级
Log.getLogConfig().configLevel(Log.LogLevel.TYPE_DEBUG);
Log.getLogConfig().configAllowLog(isDebug);  //允许日志输出
Log.getLogConfig().setLogFileDir() //设置日志文件保存路径
Log.getLogConfig().savaLogFile(isDebug);  //保存日志文件
//IO读写是一个耗时操作，故保存文件使用了线程保存，在退出程序前，为保证日志全部保存，需要调用flush方法，该方法是一个阻塞方法。
Log.flushLogFile();
</pre>
> 输出的日志文件默认保存在SD卡 LogUtilsFile目录，日志文件最多保存 7天，最多保存100M，如果超过100M会递归删除，如：超过100M，删除6天前日志，若仍旧超出，删除5天前，。。。。直到全部删除。  
> 防止日志文件大量占用存储
### 2.ACache 使用
[这是原作者](https://github.com/Jay-huangjie/ACache)
> ACache.putString(String key,String value); 输入key，和value，即可保存到文件缓存  
> 增加多个静态方法，方便直接调用  
> 增加setFilePath方法，支持设置默认保存路径，可以将配置文件，或者缓存文件放在指定路径下
### 3.ImageUtil的使用
<pre>
//bitmap转Drawable，使用的均是系统提供的方法
bitmapToDrawable(Bitmap bitmap)
bitmapToDrawable(Resources res, Bitmap bitmap)
drawableToBitmap(Drawable drawable)
//读取资源文件，本地文件为Bitmap,根据传入的宽高，或者view，会自动缩放图片，节约内存资源，防止oom
getBitmap(Resources res, int resId,int reqWidth, int reqHeight)
getBitmap(Resources res, int resId,View imageView)
getBitmap(String path,int reqWidth, int reqHeight)
//旋转图片
rotateBitmap(Bitmap origin, float alpha)
//缩放图片
scaleBitmap(Bitmap origin, int newWidth, int newHeight)
//获取ImageView宽高
getImageViewWidth(View imageView)
//合成两个bitmap，支持从任意 xy 坐标轴开始绘制
toConformBitmap(Bitmap background, Bitmap foreground, int startX, int startY)
//加载图片控件，LoadImageImpl是回调，
//适合用在功能简单的地方， 如果需要实现非常复杂的图片加载功能，建议还是
//使用 ImageLoad 第三方库
LoadImage(String path, View view, LoadImageImpl impl)
Bitmap getBitmap(String path, View view); //子线程请求网络或读取图片
void Load(View view, String path, Bitmap bitmap);//主线程加载图片
</pre>
### 4.ByteUtils 使用

<pre>
//aes加密
public static byte[] encrypt(String key, byte[] bytes)
//aes解密
public static byte[] decrypt(String key, byte[] encrypted)
//index从0开始
    //获取取第index位
    public static int GetBit(byte b, int index)
    //将第index位设为1
    public static byte SetBitOne(byte b, int index) 
    //将第index位设为0
    public static byte SetBitZero(byte b, int index)
    //将第index位取反
    public static byte ReverseBit(byte b, int index) 
//将 int转换成4位的byte
    public static byte[] getBytes(int i) 
、、、将short转换成 byte
    public static byte[] getBytes(short s) 
、、、
    public static byte[] getBytes(long l) 
、、、
    public static int getInt(byte[] buf)
、、、
    public static short getShort(byte[] buf) 
、、、
    public static long getLong(byte[] buf) 

</pre>
### 5. NetworkLibs 用来做局域网开发
>支持socket管理，使用udp组播发现设备，tcp建立对等连接，发送接收均采用了线程和list作为缓存区。使用心跳连接，保证连接的有效性，使用动态 token，使用ByteUtil中的 aes加密，确保数据安全。

>简单使用如下：
<pre>
//启动tcp服务
 ConnectManager.getInstance().startDeviceTCPServer("service", this);
//启动udp组播
 ConnectManager.getInstance().startDeviceUDPServer("service", this, this);
//客户端扫描设备
ConnectManager.getInstance().serchDeviceList(this);
//客户端连接设备
ConnectManager.getInstance().connectDevice(deviceInfo, "123", "123", this);
</pre>
>启动设备，启动组播，扫描设备，连接设备功能都需要传入回调，回调如下：
>建立连接，请求连接，是否允许连接，接收到消息，断开连接。都在回调之中
<pre>
DeviceUdpCallback, DeviceSocketCallback
PhoneUdp.SearchListCallBack, PhoneSocket.PhoneSocketCallBack
</pre>
### 6. ArithUtil 数字工具类
> 支持小数点计算，高精度计算  
> 支持 16进制和10进制互相转换


还有更多util，认为写的不好，用处也不是太大，故木有介绍了。。。。