package com.aust.airbon;

import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException{
	    // write your code here
        VirtualServer server = new VirtualServer(10086,10087,
                "I5",50,500,"10.134.1.108",100);

        server.startServer();
        server.ready();
    }
}
