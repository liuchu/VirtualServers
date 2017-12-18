package com.aust.airbon;

import org.ini4j.Wini;

import java.io.File;
import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException{
	    // write your code here
        /*VirtualServer server = new VirtualServer(10086,10087,10088,
                "I5",50,500,"10.134.1.108",100, true,1);

        server.startServer();
        server.ready();*/
        Wini ini = new Wini(new File("src/com/aust/airbon/servers.ini"));

        for (int i=1; i<11; i++) {
            int heartBeatPort = ini.get("server"+i,"heartBeatPort",int.class);
            int dataTransferPort = ini.get("server"+i,"dataTransferPort",int.class);
            int configUpdatePort = ini.get("server"+i,"configUpdatePort",int.class);
            String CPU = ini.get("server"+i,"CPU");
            int memory = ini.get("server"+i,"memory",int.class);
            int disk = ini.get("server"+i,"disk",int.class);
            String IP = ini.get("server"+i,"IP");
            int maxAllowedThreads = ini.get("server"+i,"maxAllowedThreads",int.class);
            boolean online = ini.get("server"+i,"online", boolean.class);
            int tread = ini.get("server"+i,"tread",int.class);

            //System.out.println(i+" "+IP+" "+online);
            //System.out.println(IP+online2);

            VirtualServer server = new VirtualServer(heartBeatPort,dataTransferPort,configUpdatePort,
                    CPU,memory,disk,IP,maxAllowedThreads, online,tread);

            server.ready();

        }

    }
}
