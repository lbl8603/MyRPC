package com.wherewego.rpc.codec;

import com.wherewego.rpc.transport.Transport;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 编码器和解码器
 *
 * @Author:lbl
 * @Date:Created in 22:29 2020/3/6
 * @Modified By:
 */
public class TransportCodec extends MessageToMessageCodec<ByteBuf, Transport> {
    private static final Logger LOGGER = LoggerFactory.getLogger(TransportCodec.class);

    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, Transport transport, List<Object> list) throws Exception {
        LOGGER.info("编码,帧类型"+transport.getFrameType());
        ByteBuf buf = Unpooled.buffer();
        buf.writeInt(transport.getLength());
        buf.writeByte(transport.getFrameType()|transport.getSerializeType());
        if(transport.getLength()>0){
            buf.writeBytes(transport.getBytes());
        }
        list.add(buf);
    }

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf buf, List<Object> list) throws Exception {
        if (buf.readableBytes() < Transport.HAND_LENGTH) {//不够字节的直接返回，等下一轮读
            return;
        }
        buf.markReaderIndex();//标记读位置
        int length = buf.readInt();//第一个整型标识长度
        byte by = buf.readByte();//第二个是序列化类型和帧类型

        if (buf.readableBytes() < length) {//长度不够，应该是拆包了，等下一轮读
            buf.resetReaderIndex();
            return;
        }
        Transport transport = new Transport();
        transport.setFrameType((byte) (by&0xF0));
        transport.setSerializeType((byte) (by&0x0F));
        if(length>0){
            byte[] bytes = new byte[length];
            buf.readBytes(bytes);
            transport.setLength(length);
            transport.setBytes(bytes);
        }
        list.add(transport);
        LOGGER.info("解码,帧类型"+transport.getFrameType());

    }
}
