package com.aust.airbon;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created on 2017/12/15.
 */
public class TestClient implements Runnable{



    public void run() {
        int port = 10200;

        for (int i=0; i<10; i++){
            try {
                Socket socket = new Socket("localhost",port+i*3+1);
                System.out.println(socket.getSoTimeout());
                socket.setSoTimeout(1000*10);
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
                /*JSONObject serverStatus = JSON.parseObject(stringBuffer.toString());
                String outcome = serverStatus.getString("outcome");
                if ("online".equals(outcome)) {
                    System.out.println("Server"+i+" online");
                }*/
            } catch (SocketTimeoutException e){
                System.out.println("This server is offline!!");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


    }

    public static void main(String[] args) throws IOException{

        TestClient client = new TestClient();

        //定时运行,10S一次
        ScheduledExecutorService service = Executors
                .newSingleThreadScheduledExecutor();
        // 第二个参数为首次执行的延时时间，第三个参数为定时执行的间隔时间
        service.scheduleAtFixedRate(client, 10, 10, TimeUnit.SECONDS);

    }
}
