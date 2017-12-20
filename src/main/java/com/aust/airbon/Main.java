package com.aust.airbon;

import org.apache.log4j.PropertyConfigurator;
import org.ini4j.Wini;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Main {

    public static void main(String[] args) throws IOException{

        Wini ini = null;

        try {

            String log4jConf = "log4j.properties"; // could also be a constant
            String serverIni = "servers.ini"; // could also be a constant

            ClassLoader loader = Thread.currentThread().getContextClassLoader();

            Properties props = new Properties();
            try(InputStream resourceStream = loader.getResourceAsStream(log4jConf)) {
                props.load(resourceStream);
            }
            PropertyConfigurator.configure(props);

            try(InputStream resourceStream = loader.getResourceAsStream(serverIni)) {

                ini = new Wini(resourceStream);
            }

        } catch (FileNotFoundException exception) {
            System.err.println("FILE NOT FOUND");
            exception.printStackTrace();
            System.exit(1);
        }



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

            //System.out.println(i+" "+IP+" "+disk);
            //System.out.println(IP+online2);

            VirtualServer server = new VirtualServer(heartBeatPort,dataTransferPort,configUpdatePort,
                    CPU,memory,disk,IP,maxAllowedThreads, online,tread);

            server.ready();

        }

    }
}
