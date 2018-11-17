package com.wdk.bio;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * BIO Server
 * */
public class BioServer {
    private static final int port = 10086;

    private static ServerSocket serverSocket;

    public static void start() throws IOException {
        start(port);
    }

    private synchronized static void start(int port) throws IOException {
        if(serverSocket != null){
            return;
        }
        try{
            //构建serverSocket
            serverSocket = new ServerSocket(port);

            System.out.println("ServerSocket已启动，端口号："+ port);

            while(true){ //死循环  阻塞等待client连接
                Socket socket = serverSocket.accept(); //等待连接   BIO 这里会一直阻塞等待Client进来，如果没有连接进来 就持续等待。有链接进来则处理连接请求信息。
                new Thread(new ServerHandler(socket)).start();
            }
        }finally{
            if(serverSocket != null){
                System.out.println("关闭socket");
                serverSocket.close();
                serverSocket = null;
            }
        }
    }

    public static void main(String[] args) throws IOException {
        start(port);
    }
}
