package com.wdk.nio;

/**
 * NIO server
 */
public class NioServer {
    private static final int PORT= 10086;

    private static ServerHandler serverHandle;
    public static void start(){
        start(PORT);
    }
    public static synchronized void start(int port){
        if(serverHandle!=null)
            serverHandle.stop();
        serverHandle = new ServerHandler(port);
        new Thread(serverHandle,"Server").start();
    }
    public static void main(String[] args){
        start();
    }
}
