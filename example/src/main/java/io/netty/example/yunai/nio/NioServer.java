package io.netty.example.yunai.nio;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 应用模块名称<p>
 * 代码描述<p>
 * Copyright: Copyright (C) 2016 咪咕互动娱乐有限公司， All rights reserved. <p>
 * Company: 咪咕互动娱乐有限公司<p>
 *
 * @author Mr Lu
 * @since 2019/7/5
 */
public class NioServer
{
    private ServerSocketChannel serverSocketChannel;
    private Selector selector;

    /** 构造方法：初始化NIO服务端 */

    public NioServer() throws IOException
    {
        //打开server Socket Channel
        serverSocketChannel = ServerSocketChannel.open();
        //配置为非阻塞
        serverSocketChannel.configureBlocking(false);
        //绑定Server port
        serverSocketChannel.socket().bind(new InetSocketAddress(8080));
        //创建selector
        selector = Selector.open();
        //注册server Socket Channel 到 selector
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

        /**
         * 打印到控制台
         **/
        System.out.println("server 启动完成");

        handleKeys();
    }

    /** handleKeys 方法：基于selector处理IO操作 */
    private void handleKeys() throws IOException
    {
        while (true)
        {
            /**通过selector 选择channel
              阻塞时间为30秒 每30秒等待就绪*/
            int selectNums = selector.select(30 * 1000L);

            /**不存在就绪的IO事件，继续下一次阻塞等待*/
            if(selectNums == 0){
                continue;
            }

            /**
             * 打印到控制台
             **/
            System.out.println("选择channel数量" + selectNums);

            //遍历可选择的 channel 的 SelectionKey集合
            Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
            while(iterator.hasNext())
            {
                /** 进行逐个 SelectionKey 处理。重点注意下，处理完需要进行移除 */
                SelectionKey key = iterator.next();
                iterator.remove();
                if(!key.isValid())
                {
                    //忽略无效的Selectionkey
                    continue;
                }
                handlekey(key);
            }
        }
    }

    private void handlekey(SelectionKey key) throws  IOException
    {
        //接受连接就绪
        if(key.isAcceptable())
        {
            handlerAcceptablekey(key);
        }
        //读就绪
        if(key.isReadable())
        {
            handleReadablekey(key);
        }
        //写就绪
        if(key.isWritable())
        {
            handleWritableKey(key);
        }
    }
    /** main 方法：创建NIO服务端 */

    private void handlerAcceptablekey(SelectionKey key) throws IOException
    {
        //接受 Client Socket Channel
        SocketChannel clientSocketChannel = ((ServerSocketChannel) key.channel()).accept();
        //配置为非阻塞
        clientSocketChannel.configureBlocking(false);
        //log
        /**
         * 打印到控制台
         **/
        System.out.println("接受新的 channel");
        //注册client Socket channel 到 selector
        /** 对 SelectionKey.OP_READ 事件感兴趣。这样子，在客户端发送消息( 数据 )到服务端时，我们就可以处理该 IO 事件 */
        clientSocketChannel.register(selector, SelectionKey.OP_READ, new ArrayList<String>());
    }

    private void handleReadablekey(SelectionKey key) throws IOException
    {
        //client Socket Channel
        SocketChannel clientSocketChaneel = (SocketChannel) key.channel();
        //读取数据
        ByteBuffer readBuffer = CodecUtil.read(clientSocketChaneel);
        //处理链接已断开的情况
        if(readBuffer == null)
        {

            /**
             * 打印到控制台
             **/
            System.out.println(" 断开channel");
            /** 取消注册 */
            clientSocketChaneel.register(selector, 0);
            return;
        }
        //打印数据
        /** 判断实际读取到数据 */
        if(readBuffer.position() > 0)
        {
            String content = CodecUtil.newString(readBuffer);

            /**
             * 打印到控制台
             **/
            System.out.println("读取数据：" + content);

            //添加到响应队列
            ArrayList<String> responseQueue = (ArrayList<String>) key.attachment();
            responseQueue.add("响应：" + content);
            //注册client Socket Channel 到 selector
            clientSocketChaneel.register(selector, SelectionKey.OP_WRITE, key.attachment());
        }
    }
    
    private void handleWritableKey(SelectionKey key) throws ClosedChannelException
    {
        //client socket channel
        SocketChannel clientSocketChannel = (SocketChannel) key.channel();

        //遍历响应队列
        List<String> responseQueue = (ArrayList<String>) key.attachment();
        for(String content:responseQueue)
        {
            /**
             * 打印到控制台
             **/
            System.out.println("写入数据 ：" + content);
            //返回
            CodecUtil.write(clientSocketChannel, content);
        }
        responseQueue.clear();

        //注册 Client Socket Channel 到 Selector
        clientSocketChannel.register(selector, SelectionKey.OP_READ, responseQueue);
    }
    
    public static void main(String[] args) throws IOException
    {
        NioServer server = new NioServer();
    }

}
