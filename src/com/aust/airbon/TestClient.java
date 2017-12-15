package com.aust.airbon;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created on 2017/12/15.
 */
public class TestClient {

    public static void main(String[] args) throws IOException{

        //新建一个线程，无限循环，模拟服务器状态的改变
        Runnable runnable1;

        runnable1 = () -> {
            //将输出流包装成打印流
            try {
                Socket socket = new Socket("localhost",10086);
                PrintWriter pw = new PrintWriter(socket.getOutputStream());
                BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                pw.write("HELLO!");
                pw.flush();

                socket.shutdownOutput();

                //3、获取输入流，并读取服务器端的响应信息
                String info = "";
                StringBuilder stringBuffer = new StringBuilder("");
                while((info=br.readLine())!=null){
                    //System.out.println("在while之中: "+info);
                    stringBuffer.append(info);
                }
                socket.shutdownInput();
                System.out.println("服务器说："+stringBuffer.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }

        };

        //定时运行,10S一次
        ScheduledExecutorService service = Executors
                .newSingleThreadScheduledExecutor();
        // 第二个参数为首次执行的延时时间，第三个参数为定时执行的间隔时间
        service.scheduleAtFixedRate(runnable1, 10, 10, TimeUnit.SECONDS);




    }
}
