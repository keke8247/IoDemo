package com.wdk.nio;

import com.wdk.util.Calculator;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

/**
 * NIO ServerHandler
 */
public class ServerHandler implements Runnable{
    private Selector selector; //多路复用器

    private ServerSocketChannel serverChannel; //ServerSocket管道

    private volatile boolean started; //线程可见，禁止指令重排序


    /**
     *构造方法
     */
    public ServerHandler(int port){

        try {
            //创建选择器
            selector = Selector.open();  //Selector.open() 返回WindowsSelectorImpl 实例

            //打开监听通道
            serverChannel = ServerSocketChannel.open(); //ServerSocketChannel.open() 返回ServerSocketChannelImpl 实例

            //开启非阻塞模式
            serverChannel.configureBlocking(false); //true为阻塞模式  false为非阻塞模式 该方法线程安全

            //绑定端口 backlog设为1024(请求传入最大长度)
            //serverChannel.socket() 为线程安全的，且是单例模式，感觉此方法可以进一步优化 double checking，此方法返回的是 ServerSocketAdaptor（ServerSocket适配器）
            //bind()方法 是调用的SeverSocketChannelImpl的bind()方法。
            serverChannel.socket().bind(new InetSocketAddress(port),1024);

            //注册  监听连接请求 SelectionKey.OP_ACCEPT为连接请求
            //把serverChannel 注册到 Reactor线程的Selecotr上,监听 请求连接事件
            SelectionKey key = serverChannel.register(selector,SelectionKey.OP_ACCEPT);

            //标记开启服务
            started = true;

            System.out.println("服务已启动，端口号："+port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stop(){
        started = false;
    }


    @Override
    public void run() {
        //当服务开启情况下 遍历selector
        while (started){
            try {
                //无论是否有读写时间发生，selector每隔1秒唤醒一次。
                int readySelectionKeys = selector.select(1000);

                if(readySelectionKeys == 0) continue;;

                Set<SelectionKey> keys = selector.selectedKeys();

                Iterator<SelectionKey> iterator = keys.iterator();

                SelectionKey key = null;
                while (iterator.hasNext()){
                    key = iterator.next();

                    iterator.remove(); //接收到一个请求连接，把当前的SelectionKey 移除Selector 防止把重复的请求分配到同一个SelectionKey

                    try{
                        handleInput(key);
                    }catch (IOException e){ //捕获到异常  关闭SelectionKey的channel
                        if(key != null){
                            key.cancel();
                            if(key.channel() != null){
                                key.channel().close();
                            }
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //selector关闭后会自动释放里面管理的资源
        if(selector != null){
            try{
                selector.close();
            }catch (Exception e) {
                e.printStackTrace();
            }
        }
}

    private void handleInput(SelectionKey key) throws IOException{
        if(key.isValid()){ //key是有效的

            //处理新接入请求消息
            if(key.isAcceptable()){
                ServerSocketChannel ssc = (ServerSocketChannel) key.channel();

                //接收客户端的请求并创建socketChannel实例.
                //通过ServerSocketChannel的accept创建SocketChannel实例
                //完成该操作意味着完成TCP请求的三次握手，TCP物理链路成功建立
                SocketChannel socketChannel = ssc.accept();

                //设置为非阻塞
                socketChannel.configureBlocking(false);

                //连接建立 注册为读
                //将新接入的客户端注册到Reactor线程的多路复用器上,监听读操作.读取客户端发送过来的网络消息
                socketChannel.register(selector,SelectionKey.OP_READ);
            }

            //处理读消息
            if(key.isReadable()){
                SocketChannel socketChannel = (SocketChannel) key.channel();

                //创建ByteBuffer 开辟1M的缓冲区
                ByteBuffer buffer = ByteBuffer.allocate(1024);

                //处理请求码流,把消息读到缓冲区  返回读取到的字节数
                int readBytes = socketChannel.read(buffer);

                //读取到的字节不为空，对字节进行编解码
                if(readBytes>0){

                    //将缓冲区当前的limit设置为position=0，用于后续对缓冲区的读写操作。由于channel是双向的
                    buffer.flip();

                    //根据缓冲区可读字节数 创建字节数组
                    byte[] bytes = new byte[buffer.remaining()];

                    //将缓冲区可读字节数组复制到新建的数组中
                    buffer.get(bytes);

                    String expression = new String(bytes,"UTF-8");
                    System.out.println("服务器收到消息：" + expression);
                    //处理数据
                    String result = null;
                    try{
                        result = Calculator.cal(expression).toString();
                    }catch(Exception e){
                        result = "计算错误：" + e.getMessage();
                    }
                    //发送应答消息
                    doWrite(socketChannel,result);
                }else if(readBytes<0){//链路已关闭 关闭通道 释放资源
                    key.cancel();
                    socketChannel.close();
                }
            }
        }
    }

    private void doWrite(SocketChannel socketChannel,String result) throws IOException {
        //将消息编码为字节数组
        byte[] bytes = result.getBytes();
        //根据数组容量创建ByteBuffer
        ByteBuffer writeBuffer = ByteBuffer.allocate(bytes.length);
        //将字节数组复制到缓冲区
        writeBuffer.put(bytes);
        //flip操作
        writeBuffer.flip();
        //发送缓冲区的字节数组
        socketChannel.write(writeBuffer);
        //****此处不含处理“写半包”的代码  暂不考虑拆包操作。
    }
}
