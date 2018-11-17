package com.wdk.nio;

/**
 * Nio 客户端
 */
public class NioClient {

    private static final String HOST = "127.0.0.1";

    private static final int PORT= 10086;

    private static ClientHandler clientHandle;
    public static void start(){
        start(HOST,PORT);
    }
    public static synchronized void start(String ip,int port){
        if(clientHandle!=null)
            clientHandle.stop();
        clientHandle = new ClientHandler(ip,port);
        new Thread(clientHandle,"Server").start();
    }
    //向服务器发送消息
    public static boolean sendMsg(String msg) throws Exception{
        if(msg.equals("q")) return false;
        clientHandle.sendMsg(msg);
        return true;
    }
    public static void main(String[] args){
        start();
    }

}
