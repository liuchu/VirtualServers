package com.aust.airbon;

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
    private String CPU;             //模拟的CPU型号
    private int memory;             //模拟的内存大小
    private int disk;               //模拟的磁盘空间

    /*服务器的动态数据。这部分数据可以被用户修改 */
    private String IP;              //虚拟服务器IP地址
    private int maxAllowedThreads;  //能运行的最大线程数

    /*运行状态，会随系统随时改变 */
    private boolean online;         //虚拟服务器的状态，online时Socket服务正常运行，offline关闭
    private int usedCPU;         //CPU使用率
    private int usedMemory;         //内存状态
    private int usedDisk;           //磁盘使用量
    private int currentThreads;     //当前运行的线程数

    /*  */
    private ServerSocket heartBeatSocket = null;
    private ServerSocket dataTransferSocket = null;

    /* 空的构造函数 */
    public VirtualServer() {

    }

    /* 传入基本服务器数据，构造函数 */
    public VirtualServer(int heartBeatPort, int dataTransferPort, String CPU, int memory, int disk,
                         String IP, int maxAllowedThreads) throws IOException {
        this.heartBeatPort = heartBeatPort;
        this.dataTransferPort = dataTransferPort;
        this.CPU = CPU;
        this.memory = memory;
        this.disk = disk;
        this.IP = IP;
        this.maxAllowedThreads = maxAllowedThreads;

        heartBeatSocket = new ServerSocket(heartBeatPort);
        dataTransferSocket = new ServerSocket(dataTransferPort);

        initStatus(10);
    }

    //初始化状态
    public void initStatus(int currentThreads){
        setCurrentThreads(currentThreads);
    }

    /* Getter 和 Setter, 静态数据只允许Get*/

    public int getHeartBeatPort() {
        return heartBeatPort;
    }

    public int getDataTransferPort() {
        return dataTransferPort;
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
        setOnline(true);
    }

    //停止服务器(既停止两个Socket服务，并停止状态刷新)
    public void stopServer(){
        setOnline(false);
    }

    //更新IP配置
    public void updateConfigIP(String IP){
        setIP(IP);
    }

    //更新最大线程数配置
    public void updateConfigMaxAllowedThreads(int maxAllowedThreads){
        setMaxAllowedThreads(maxAllowedThreads);
    }

    /* 模拟服务器的状态改变
     * 每一次改变，都将数据记录在文件当中 */
    public void freshStatus(int usedCPU, int usedMemory, int usedDisk, int currentThreads){
        //System.out.println("THREADS:"+currentThreads);
        setUsedCPU(usedCPU);
        setUsedMemory(usedMemory);
        setUsedDisk(usedDisk);
        setCurrentThreads(currentThreads);
    }

    /* 开始运行 */
    public void ready(){


        new Thread(new Runnable() {
            @Override
            public void run() {
                //一直运行，检查


            }
        }).start();

        //启动一个线程，无限循环，来接收客户端传过来的请求。
        //间隔很长,10 min一次，传回的是过去10分钟数据的平均值。平均值通过读取文件来计算
       /* new Thread(new Runnable() {
            @Override
            public void run() {
                //一直运行，检查

            }
        }).start();*/


        Runnable runnable4 = new VirtualServer.StatusRefreshRunnable();
        //定时运行,每10S，服务器状态会改变一次
        ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
        service.scheduleAtFixedRate(runnable4, 10, 10, TimeUnit.SECONDS);
    }

    //处理来自Client的json信息
    //1. GET 请求获取当前服务器的数据
    //2. PUT 请求更新当前服务器的配置
    public String handleMessageFromClient(String message){
        System.out.println("收到来自客户端的信息:"+message);

        int threadNum = getCurrentThreads();
        return "服务器当前线程数为："+threadNum;
    }

/**************************** 声明内部类 ****************************/
    /* 用于接收心跳检测的线程类
     * 启动线程来无限循环来维持程序一直运行
     * 检测心跳，并且每次都将服务器最新的状态返回给客户端。
     * 定时，间隔十分短，比如5s，实时数据。
     */
    class HeartBeatRunnable implements Runnable {

        @Override
        public void run() {
            while (VirtualServer.this.isOnline()){ //如果Server是online状态，那么持续接收客户端的信息
                try {
                    Socket socket = null;
                    socket = VirtualServer.this.heartBeatSocket.accept();

                    //读入流
                    BufferedReader bufferedReader =new BufferedReader(new InputStreamReader(socket.getInputStream()));

                    StringBuffer message = new StringBuffer();
                    String line =null;
                    while((line=bufferedReader.readLine())!=null){
                        message.append(line);
                        //message.append("/r");
                    }

                    //System.out.println("服务器说："+message);

                    socket.shutdownInput();//关闭输入流

                    //处理，并返回数据
                    String response = VirtualServer.this.handleMessageFromClient(message.toString());

                    System.out.println(response);
                    //获取输出流，响应客户端的请求
                    PrintWriter pw = new PrintWriter(socket.getOutputStream());
                    pw.write(response);
                    pw.flush();
                    socket.shutdownOutput();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /* 用于定时向客户端发送服务器状态的线程类
     * 启动线程来无限循环来维持程序一直运行
     * 有客户端发起请求，服务端将服务器最新的状态返回给客户端。
     * 定时，间隔长，比如10min，用于统计数据 */
    class DataTransferRunnable implements Runnable{

        @Override
        public void run() {
                while (VirtualServer.this.isOnline()){ //如果Server是online状态，那么持续接收客户端的信息
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

                        //处理，并返回数据
                        String response = VirtualServer.this.handleMessageFromClient(message.toString());

                        //获取输出流，响应客户端的请求
                        PrintWriter pw = new PrintWriter(socket.getOutputStream());
                        pw.write(response);
                        pw.flush();
                        socket.shutdownOutput();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
        }
    }

    /* 用于接收用户指令，比如 关闭服务器，修改最大线程数
     * 启动线程来无限循环来维持程序一直运行
     * 有客户端发起请求，服务端更改配置，并将结果将结果返回给客户端。
     * 不定时，客户端发起既处理。
     */
    class UpdateConfigRunnable implements Runnable {

        @Override
        public void run() {

        }
    }

    /* 用于模拟虚拟服务器的状态变化
     * 一直运行，将输入的数据设置为状态值
     */
    class StatusRefreshRunnable implements Runnable {

        @Override
        public void run() {

        }
    }


}
