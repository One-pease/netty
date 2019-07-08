package io.netty.example.yunai.nio;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * 应用模块名称<p>
 * 代码描述<p>
 * Copyright: Copyright (C) 2016 咪咕互动娱乐有限公司， All rights reserved. <p>
 * Company: 咪咕互动娱乐有限公司<p>
 *
 * @author Mr Lu
 * @since 2019/7/8
 */
public class NioClient
{
    private SocketChannel clientSocketChannel;

    private Selector selector;

    private final List<String> responseQueue = new ArrayList<String>();

    private CountDownLatch connected = new CountDownLatch(1);

    public NioClient() throws IOException, InterruptedException
    {
        //打开client  socket channel
        clientSocketChannel = SocketChannel.open();
        //配置为非阻塞
        clientSocketChannel.configureBlocking(false);
        //创建selector
        selector = Selector.open();
        //注册Client socket channel 到 selector
        clientSocketChannel.register(selector, SelectionKey.OP_CONNECT);
        //连接服务器
        clientSocketChannel.connect(new InetSocketAddress(8080));

        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    handlekeys();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        }).start();

        /** 实现阻塞等待客户端成功连接上服务端 */
        if(connected.getCount() !=0)
        {
            connected.await();
        }

        /**
         * 打印到控制台
         **/
        System.out.println("Client 启动完成");
    }

    private void handlekeys () throws IOException
    {
        while(true)
        {
            //通过 Selector 选择Channel
            int selectNums = selector.select(30 * 1000L);
            if(selectNums == 0)
            {
                continue;
            }

            //遍历可选择的 Channel 的 selectionkey集合
            Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
            while(iterator.hasNext())
            {
                SelectionKey key = iterator.next();
                iterator.remove();
                if(!key.isValid())
                {
                    continue;
                }

                handlekey(key);
            }
        }
    }

    private synchronized void handlekey(SelectionKey key) throws IOException
    {
        //接受连接就绪
        if(key.isConnectable())
        {
            handlerConnectablekey(key);
        }
        //读就绪
        if(key.isReadable())
        {
            handleReadableKey(key);
        }
        //写就绪
        if(key.isWritable())
        {
            handleWritableKey(key);
        }
    }

    private void handlerConnectablekey(SelectionKey key) throws ClosedChannelException
    {
        /** 判断客户端的 SocketChannel 上是否正在进行连接的操作，若是，则完成连接 */
        if(!clientSocketChannel.isConnectionPending())
        {
            return;
        }
        //log
        System.out.println("接受新的 Channel");
        //注册Client Socket channel 到 selector
        clientSocketChannel.register(selector, SelectionKey.OP_READ, responseQueue);
        //标记为已连接
        /** 结束 NioClient 构造方法中的阻塞等待连接完成。 */
        connected.countDown();
    }

    private void handleReadableKey(SelectionKey key) throws IOException
    {
        //Clieant Socket Channel
        SocketChannel clientSocketChannel = (SocketChannel) key.channel();
        //读取数据
        ByteBuffer readBuffer = CodecUtil.read(clientSocketChannel);
        //打印数据
        if(readBuffer.position() > 0)
        {
            String content = CodecUtil.newString(readBuffer);

            System.out.println("读取数据" + content);
        }
    }

    private void handleWritableKey(SelectionKey key) throws ClosedChannelException
    {
        //Client Socket Channel
        SocketChannel clientSocketChannel = (SocketChannel) key.channel();
        //遍历响应队列
        List<String> responseQueue = (ArrayList<String>) key.attachment();
        for(String content:responseQueue)
        {
            //打印数据
            System.out.println("写入数据："+ content);
            //返回
            CodecUtil.write(clientSocketChannel, content);
        }
        responseQueue.clear();

        //注册Client Socket Channel 到 selector
        clientSocketChannel.register(selector, SelectionKey.OP_READ, responseQueue);
    }

    public synchronized void send(String content) throws ClosedChannelException
    {
        //添加到响应队列
        responseQueue.add(content);
        //打印数据
        System.out.println("写入数据" + content);
        //注册ClientSocketChannel 到 Selector
        clientSocketChannel.register(selector, SelectionKey.OP_WRITE,responseQueue);
        selector.wakeup();
    }

    public static void main(String[] args) throws IOException, InterruptedException
    {
        NioClient client = new NioClient();
        for(int i = 0; i < 30; i++)
        {
            client.send("nihao:" + i);
            Thread.sleep(1000L);
        }
    }

}
