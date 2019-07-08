package io.netty.example.yunai.nio;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.channels.SocketChannel;

import java.nio.ByteBuffer;

/**
 * 编解码工具类<p>
 * 代码描述<p>
 * Copyright: Copyright (C) 2016 咪咕互动娱乐有限公司， All rights reserved. <p>
 * Company: 咪咕互动娱乐有限公司<p>
 *
 * @author Mr Lu
 * @since 2019/7/8
 */
public class CodecUtil
{
    public static ByteBuffer read(SocketChannel channel) throws IOException
    {
        //注意，不考虑拆包的处理
        ByteBuffer buffer = ByteBuffer.allocate(1024);

        try
        {
            int count = channel.read(buffer);

            if(count == -1)
            {
                /** 若返回的结果( count ) 为 -1 ，意味着客户端连接已经断开 */
                return null;
            }
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
        return buffer;
    }

    public static void write(SocketChannel channel, String content)
    {
        //写入Buffer
        ByteBuffer buffer = ByteBuffer.allocate(1024);

        try
        {
            buffer.put(content.getBytes("UTF-8"));
        }
        catch (UnsupportedEncodingException e)
        {
            throw new RuntimeException(e);
        }
        //写入Channel
        buffer.flip();
        try
        {
            //注意，不考虑写入超过 channel 缓存区上限
            channel.write(buffer);
        }
        catch (IOException e)
        {
            throw new  RuntimeException(e);
        }
    }

    public static String newString(ByteBuffer buffer)
    {
        buffer.flip();
        byte[] bytes = new byte[buffer.remaining()];
        System.arraycopy(buffer.array(), buffer.position(), bytes, 0 , buffer.remaining());
        try
        {
            return new String(bytes, "UTF-8");
        }
        catch (UnsupportedEncodingException e)
        {
            throw new RuntimeException(e);
        }
    }
}
