package com.wdk.bio;

import com.wdk.util.Calculator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * SocketServer处理Client连接
 * */
public class ServerHandler implements Runnable{
    private Socket socket;

    public ServerHandler(Socket socket){
        this.socket = socket;
    }

    public void run() {
        BufferedReader bufferedReader = null;
        PrintWriter printWriter = null;

        try{
            bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            printWriter = new PrintWriter(socket.getOutputStream(),true);

            String expression;
            String result;

            while(true) {
                //通过BufferedReader读取一行
                //如果已经读到输入流尾部，返回null,退出循环
                //如果得到非空值，就尝试计算结果并返回

                if ((expression = bufferedReader.readLine()) == null) {
                    break;
                }

                System.out.println("服务器收到消息：" + expression);

                try{
                    result = Calculator.cal(expression)+"";
                }catch(Exception e){
                    result = "计算错误：" + e.getMessage();
                }
                printWriter.println(result);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if(bufferedReader != null){
                try {
                    bufferedReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                bufferedReader = null;
            }

            if(printWriter != null){
                printWriter.close();
                printWriter = null;
            }
            if(socket != null){
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                socket = null;
            }
        }
    }
}
