package com.aust.airbon;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggerFactory;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created on 2017/12/15.
 */
public class VirtualServer {

    /*服务器的静态数据。这部分数据一旦初始化，就不能够被修改 */
    private int heartBeatPort;      //心跳端口(Socket)，用于从监控系统检测虚拟服务器是否在线
    private int dataTransferPort;   //另外开启一个Socket服务，用于按一定频率传输数据给监控系统
    private int configUpdatePort;   //另外开启一个Socket服务，用于接受监控系统的修改配置请求
    private String CPU;             //模拟的CPU型号
    private int memory;             //模拟的内存大小
    private int disk;               //模拟的磁盘空间

    /*服务器的动态数据。这部分数据可以被用户修改 */
    private String IP;              //虚拟服务器IP地址
    private int maxAllowedThreads;  //能运行的最大线程数

    /*运行状态，会随系统随时改变 */
    private boolean online;         //虚拟服务器的状态，online时Socket服务正常运行，offline关闭
    private int usedCPU;            //CPU使用率
    private int usedMemory;         //内存状态
    private int usedDisk;           //磁盘使用量
    private int currentThreads;     //当前运行的线程数

    /*该成员变量不是服务器的属性，用于定义服务器的数据变化趋势
     *有三种：1. load_increase 压力持续增长 2. load_decrease 压力持续下降 3. load_balance 压力平衡
     */
    private int trend;

    /* Socket */
    private ServerSocket heartBeatSocket = null;
    private ServerSocket dataTransferSocket = null;
    private ServerSocket configUpdateSocket = null;

    //Logger记录日志
    static Logger  heartBeatLog = Logger.getLogger("heart_beat_log");
    static Logger dataTransferLog = Logger.getLogger("date_transfer_log");
    static Logger configUpdateLog = Logger.getLogger("config_update_log");
    static Logger serversStatusLog = Logger.getLogger("servers_status_log");
    static Logger startStopLog = Logger.getLogger("start_stop_log");
    static Logger errorLog = Logger.getLogger("error_log");

    /* 空的构造函数 */
    private VirtualServer() {

    }

    private VirtualServer(int heartBeatPort, int dataTransferPort, int configUpdatePort,
                         String CPU, int memory, int disk, String IP,
                         int maxAllowedThreads, boolean online, int usedCPU, int usedMemory,
                         int usedDisk, int currentThreads, ServerSocket heartBeatSocket,
                         ServerSocket dataTransferSocket, ServerSocket configUpdateSocket) {
        this.heartBeatPort = heartBeatPort;
        this.dataTransferPort = dataTransferPort;
        this.configUpdatePort = configUpdatePort;
        this.CPU = CPU;
        this.memory = memory;
        this.disk = disk;
        this.IP = IP;
        this.maxAllowedThreads = maxAllowedThreads;
        this.online = online;
        this.usedCPU = usedCPU;
        this.usedMemory = usedMemory;
        this.usedDisk = usedDisk;
        this.currentThreads = currentThreads;
        this.heartBeatSocket = heartBeatSocket;
        this.dataTransferSocket = dataTransferSocket;
        this.configUpdateSocket = configUpdateSocket;
    }

    /* 传入基本服务器数据，构造函数 */
    public VirtualServer(int heartBeatPort, int dataTransferPort, int configUpdatePort, String CPU, int memory, int disk,
                         String IP, int maxAllowedThreads, boolean online, int trend) throws IOException {
        this.heartBeatPort = heartBeatPort;
        this.dataTransferPort = dataTransferPort;
        this.configUpdatePort = configUpdatePort;
        this.CPU = CPU;
        this.memory = memory;
        this.disk = disk;
        this.IP = IP;
        this.maxAllowedThreads = maxAllowedThreads;
        this.online = online;
        this.trend = trend;

        initStatus();
    }

    private void initSockets(){

        try {
            heartBeatSocket = new ServerSocket(heartBeatPort);
            dataTransferSocket = new ServerSocket(dataTransferPort);
            configUpdateSocket = new ServerSocket(configUpdatePort);
            heartBeatLog.info(getIP()+"心跳监测服务启动成功！");
            dataTransferLog.info(getIP()+"状态传输服务启动成功！");
            configUpdateLog.info(getIP()+"配置更新服务启动成功！");
        } catch (IOException exception){
            errorLog.fatal(getIP()+"心跳监测服务, 状态传输服务, 配置更新服务 启动失败！");
        }
    }

    //初始化状态, 给一个初始的状态
    // CPU 一开始占用20%，内存50%，硬盘20%，当前线程1/10
    private void initStatus(){

            setUsedCPU(20);
            setUsedMemory(getMemory()/2);
            setUsedDisk(getDisk()/5);
            setCurrentThreads(getMaxAllowedThreads()/10);
    }

    /* Getter 和 Setter, 静态数据只允许Get*/

    private int getHeartBeatPort() {
        return heartBeatPort;
    }

    private int getDataTransferPort() {
        return dataTransferPort;
    }

    private int getConfigUpdatePort() {
        return configUpdatePort;
    }

    public String getCPU() {
        return CPU;
    }

    public int getMemory() {
        return memory;
    }

    public int getDisk() {
        return disk;
    }

    private int getTrend() {
        return trend;
    }

    /* Getter 和 Setter, 可变数据可以Get和Set */
    public String getIP() {
        return IP;
    }

    private void setIP(String IP) {
        this.IP = IP;
    }

    public int getMaxAllowedThreads() {
        return maxAllowedThreads;
    }

    private void setMaxAllowedThreads(int maxAllowedThreads) {
        this.maxAllowedThreads = maxAllowedThreads;
    }

    public boolean isOnline() {
        return online;
    }

    private void setOnline(boolean online) {
        this.online = online;
    }

    public int getUsedCPU() {
        return usedCPU;
    }

    private void setUsedCPU(int usedCPU) {
        this.usedCPU = usedCPU;
    }

    public int getUsedMemory() {
        return usedMemory;
    }

    private void setUsedMemory(int usedMemory) {
        this.usedMemory = usedMemory;
    }

    public int getUsedDisk() {
        return usedDisk;
    }

    private void setUsedDisk(int usedDisk) {
        this.usedDisk = usedDisk;
    }

    public int getCurrentThreads() {
        return currentThreads;
    }

    private void setCurrentThreads(int currentThreads) {
        this.currentThreads = currentThreads;
    }

    /* Non-system Functions
     *
     */

    //开启服务器(既开启两个Socket服务，并开始状态刷新)
    public void startServer(){

        synchronized (VirtualServer.this) {
            setOnline(true);
            initStatus();
        }

        startStopLog.info(getIP()+"服务器开启");

    }

    //停止服务器(既停止两个Socket服务，并停止状态刷新)
    public void stopServer(){

        synchronized (VirtualServer.this) {
            setOnline(false);
        }
        startStopLog.info(getIP()+"服务器关闭");
    }

    //更新最大线程数配置
    public void updateConfigMaxAllowedThreads(int maxAllowedThreads){

        synchronized (VirtualServer.this) {
            setMaxAllowedThreads(maxAllowedThreads);
        }

    }

    /* 模拟服务器的状态改变 */
    public void freshStatus(int usedCPU, int usedMemory, int usedDisk, int currentThreads){
        //System.out.println("THREADS:"+currentThreads);

        //保证在范围内
        if (usedCPU>=0 && usedCPU<100 && usedMemory>=0 && usedMemory<getMemory() && usedDisk>=0
                && usedDisk<getDisk() && currentThreads>=0 && currentThreads<getMaxAllowedThreads()){
            setUsedCPU(usedCPU);
            setUsedMemory(usedMemory);
            setUsedDisk(usedDisk);
            setCurrentThreads(currentThreads);
        } else { //不正常的数值，比如硬盘超出范围
            stopServer();
        }

    }

    /* 开始运行 */
    public void ready() {

        initSockets();

        Thread heartBeatThread =  new Thread(new HeartBeatRunnable());
        Thread dataTransferThread =  new Thread(new DataTransferRunnable());
        Thread updateConfigThread =  new Thread(new UpdateConfigRunnable());

        heartBeatThread.start();
        dataTransferThread.start();
        updateConfigThread.start();

        Runnable runnable4 = new VirtualServer.StatusRefreshRunnable();
        //定时运行,每10S，服务器状态会改变一次
        try {
            ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
            service.scheduleAtFixedRate(runnable4, 10, 10, TimeUnit.SECONDS);
            serversStatusLog.info(getIP()+"状态定时更新服务启动成功");
        } catch (Exception e){
            errorLog.fatal(getIP()+"状态定时更新服务启动失败！");
        }
    }


/**************************** 声明内部类 ****************************/
    /* 用于接收心跳检测的线程类
     * 启动线程来无限循环来维持程序一直运行
     */
    class HeartBeatRunnable implements Runnable {

        @Override
        public void run() {
            //该方法会一直无限循环运行，当虚拟服务器online时，Socket持续accept连接；而offline时，线程sleep，直到重新online。
                while(true) {
                    if (VirtualServer.this.isOnline()){ //如果Server是online状态，那么持续接收客户端的信息
                        try {
                            Socket socket = null;

                            socket = VirtualServer.this.heartBeatSocket.accept();

                            //读入流
                            BufferedReader bufferedReader =new BufferedReader(new InputStreamReader(socket.getInputStream()));

                            StringBuffer message = new StringBuffer();
                            String line =null;
                            while((line=bufferedReader.readLine())!=null){
                                message.append(line);
                            }

                            //System.out.println("服务器说："+message);

                            socket.shutdownInput();//关闭输入流

                            //处理，并返回数据
                            JSONObject serverStatus = new JSONObject();
                            serverStatus.put("outcome","online");
                            String response = serverStatus.toJSONString();

                            //System.out.println(response);
                            //获取输出流，响应客户端的请求
                            PrintWriter pw = new PrintWriter(socket.getOutputStream());
                            pw.write(response);
                            pw.flush();
                            socket.shutdownOutput();
                            heartBeatLog.info(getIP()+"监听系统获取了一次心跳");
                        } catch (IOException e) {
                            e.printStackTrace();
                            heartBeatLog.error(getIP()+"心跳监测服务accept请求失败！");
                        }
                    } else {
                        //当offline时，线程sleep5秒。5秒之后while会继续执行，再次判断是否offline
                        try {
                            Thread.sleep(1000*5);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            heartBeatLog.error("心跳监测服务线程sleep失败！");
                        }
                    }
                }

        }
    }

    /* 用于定时处理
     * 启动线程来无限循环来维持程序一直运行
     * 有客户端发起请求，服务端将服务器最新的状态返回给客户端。
     */
    class DataTransferRunnable implements Runnable{

        @Override
        public void run() {
            while (true) {
                if (VirtualServer.this.isOnline()){ //如果Server是online状态，那么持续接收客户端的信息
                    try {
                        Socket socket = null;
                        socket = VirtualServer.this.dataTransferSocket.accept();

                        //这个Socket无需读出客户端传过来的message，收到请求后，就将最新的状态信息
                        BufferedReader bufferedReader =new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        StringBuffer message = new StringBuffer();
                        String line =null;
                        while((line=bufferedReader.readLine())!=null){
                            message.append(line);
                            //message.append("/r");
                        }
                        socket.shutdownInput();//关闭输入流

                        //处并返回数据服务器信息
                        //String response = VirtualServer.this.handleMessageFromClient(message.toString());
                        JSONObject serverStatus = new JSONObject();
                        serverStatus.put("CPU",getCPU());
                        serverStatus.put("memory",getMemory());
                        serverStatus.put("disk",getDisk());
                        serverStatus.put("IP",getIP());
                        serverStatus.put("maxAllowedThreads",getMaxAllowedThreads());
                        serverStatus.put("online",isOnline());
                        serverStatus.put("usedCPU",getUsedCPU());
                        serverStatus.put("usedMemory",getUsedMemory());
                        serverStatus.put("usedDisk",getUsedDisk());
                        serverStatus.put("currentThreads",getCurrentThreads());
                        String jsonString = serverStatus.toJSONString();

                        //获取输出流，响应客户端的请求
                        PrintWriter pw = new PrintWriter(socket.getOutputStream());
                        pw.write(jsonString);
                        pw.flush();
                        socket.shutdownOutput();

                        dataTransferLog.info(getIP()+" 客户端获取了一次状态信息, 状态是:"+jsonString);
                    } catch (IOException e) {
                        e.printStackTrace();
                        dataTransferLog.error(getIP()+"状态传输服务accept请求失败！");
                    }
                } else {
                    //当offline时，线程sleep 5秒。5秒之后while会继续执行，再次判断是否offline
                    try {
                        Thread.sleep(1000*5);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        dataTransferLog.error(getIP()+"状态传输服务线程sleep失败！");
                    }
                }
            }

        }
    }

    /* 用于接收用户指令，@v.1.0 只支持操作：开启/关闭服务器，修改最大线程数
     * 启动线程来无限循环来维持程序一直运行
     * 有客户端发起请求，服务端更改配置，并将结果将结果返回给客户端。
     * 不定时，客户端发起既处理。
     * JSON命令格式：{type:status,value:on}
     */
    class UpdateConfigRunnable implements Runnable {

        @Override
        public void run() {
            while (true) {
                if (VirtualServer.this.isOnline()){ //如果Server是online状态，那么持续接收客户端的信息
                    try {
                        Socket socket = null;
                        socket = VirtualServer.this.configUpdateSocket.accept();

                        //这个Socket无需读出客户端传过来的message，收到请求后，就将最新的状态信息
                        BufferedReader bufferedReader =new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        StringBuffer message = new StringBuffer();
                        String line =null;
                        while((line=bufferedReader.readLine())!=null){
                            message.append(line);
                            //message.append("/r");
                        }
                        socket.shutdownInput();//关闭输入流

                        //收到message，做修改，并返回修改结果
                        JSONObject command = JSON.parseObject(message.toString());
                        String type = command.getString("type");

                        if ("status".equals(type)){
                            //修改状态（online/offline）
                            boolean value = command.getBooleanValue("value");
                            //锁，防止资源被多个线程同时访问
                            synchronized (VirtualServer.this) {
                                //VirtualServer.this.setOnline(value);
                                if (value) {
                                    VirtualServer.this.startServer();
                                } else {
                                    VirtualServer.this.stopServer();
                                }
                            }

                        } else if ("maxAllowedThreads".equals(type)) {
                            //修改最大线程数
                            int value = command.getIntValue("value");
                            updateConfigMaxAllowedThreads(value);
                            startStopLog.info(getIP()+ "客户端修改了最大线程数,新的数值为"+value);
                        } else {
                            configUpdateLog.warn("不支持的命令类型: "+type);
                        }

                        //处并返回数据,服务器信息
                        //String response = VirtualServer.this.handleMessageFromClient(message.toString());
                        JSONObject serverStatus = new JSONObject();
                        serverStatus.put("outcome", "SUCCESS");

                        //获取输出流，响应客户端的请求
                        PrintWriter pw = new PrintWriter(socket.getOutputStream());
                        //pw.write(jsonString);
                        pw.flush();
                        socket.shutdownOutput();

                    } catch (IOException e) {
                        e.printStackTrace();
                        configUpdateLog.error(getIP()+"更新配置服务accept请求失败");
                    }
                } else {
                    //当offline时，线程sleep5秒。5秒之后while会继续执行，再次判断是否offline
                    try {
                        Thread.sleep(1000*5);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        configUpdateLog.error(getIP()+"更新配置服务线程sleep失败！");
                    }
                }
            }
        }
    }

    /* 用于模拟虚拟服务器的状态变化
     * 一直运行
     */
    class StatusRefreshRunnable implements Runnable {

        int times = 0; //10天时间大概会刷新100,000次

        @Override
        public void run() {
            int tread = getTrend();

            if (times > (Integer.MAX_VALUE - 2)) {
                errorLog.error(getIP()+"Already run "+(Integer.MAX_VALUE - 2)+" times, server is dead forever");
            }

            if (VirtualServer.this.isOnline()){

                int newUsedCPU = 0;
                int newUsedMemory = 0;
                int newUsedDisk = 0;
                int newCurrentThread = 0;

                times++;

                switch (tread) {
                    case 1: //模拟load增长
                        //int usedCPU, int usedMemory, int usedDisk, int currentThreads

                        newUsedCPU = (int)(Math.random()*100/2)+50;//CPU随机, 且永远大于50%

                        newUsedMemory = (int)(Math.random()*getMemory()/5)+getMemory()*4/5; //内存随机，且永远大于80%

                        newUsedDisk = getDisk()/5+increaseNewDisk(getDisk()*4/5,100000, times);//100,000次硬盘会涨满，自增

                        newCurrentThread = newUsedMemory*getMaxAllowedThreads()/getMemory();//Thread和Memory成正比

                        break;
                    case 2: //模拟load衰减

                        newUsedCPU = (int)(Math.random()*100/5);//CPU随机, 且永远小于20%

                        newUsedMemory = (int)(Math.random()*getMemory()/5); //内存随机，且永远小于20%

                        newUsedDisk = getDisk()/5+increaseNewDisk(getDisk()*4/5,1000000, times);//1000,000次硬盘会涨满，自增

                        newCurrentThread = newUsedMemory*getMaxAllowedThreads()/getMemory();//Thread和Memory成正比

                        break;
                    case 3: //模拟平稳运行
                        newUsedCPU = (int)(Math.random()*100/10)+30;//CPU随机, 处于30~40之间

                        newUsedMemory = (int)(Math.random()*getMemory()/5)+getMemory()*3/10; //内存随机，处于%30~50%之间

                        newUsedDisk = getDisk()/5+increaseNewDisk(getDisk()*4/5,400000, times);//100,000次硬盘会涨满，自增

                        newCurrentThread = newUsedMemory*getMaxAllowedThreads()/getMemory();//Thread和Memory成正比

                        break;
                    case 4: //模拟极限情况，硬盘满了，server运行崩溃

                        newUsedCPU = (int)(Math.random()*100/10)+30;//CPU随机, 处于30~40之间

                        newUsedMemory = (int)(Math.random()*getMemory()/5)+getMemory()*3/10; //内存随机，处于%30~50%之间

                        newUsedDisk = getDisk()/5+increaseNewDisk(getDisk()*4/5,30, times);//30次硬盘会涨满，自增

                        newCurrentThread = newUsedMemory*getMaxAllowedThreads()/getMemory();//Thread和Memory成正比

                        break;
                    default:
                        serversStatusLog.error(getIP()+" 不支持的tread");
                }

                synchronized (VirtualServer.this) { //修改对象属性，加锁
                    VirtualServer.this.freshStatus(newUsedCPU,newUsedMemory,newUsedDisk,newCurrentThread);
                }

                serversStatusLog.info(getIP()+" 更新服务器状态，完成。usedCPU="+newUsedCPU+";usedMemory="+newUsedMemory
                        +";usedDisk="+newUsedDisk+";currentThread="+newCurrentThread);
            } else {
                serversStatusLog.warn(getIP()+" 是离线状态，状态不会更新");
            }

        }

        private int increaseNewDisk(int wholeData, int millis,int times ){
            return (wholeData*times)/millis;
        }

    }


}
