package com.krt.mqtt.server.netty;

import com.krt.mqtt.server.beans.MqttChannel;
import com.krt.mqtt.server.beans.MqttSendMessage;
import com.krt.mqtt.server.constant.CommonConst;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

@Slf4j
@Component
public class MqttChannelApi {

    public static final AttributeKey<Boolean> _LOGIN = AttributeKey.valueOf("login");

    public static final AttributeKey<String> _DEVICE_ID = AttributeKey.valueOf("deviceId");

    public static final AttributeKey<Integer> _DB_ID = AttributeKey.valueOf("dbId");

    @Autowired
    private MqttTopicApi mqttTopicApi;

    /**
     * 已连接到服务器端的通道
     */
    private static ConcurrentHashMap<String, MqttChannel> channels = new ConcurrentHashMap<>();

    public String getDeviceId(ChannelHandlerContext ctx){
        return ctx.channel().attr(_DEVICE_ID).get();
    }

    public void setDeviceId(ChannelHandlerContext ctx, String deviceId){ ctx.channel().attr(_DEVICE_ID).set(deviceId); }

    public Integer getDbId(ChannelHandlerContext ctx){
        return ctx.channel().attr(_DB_ID).get();
    }

    public void setDbId(ChannelHandlerContext ctx, Integer dbId){ ctx.channel().attr(_DB_ID).set(dbId); }

    public Boolean isLogin(ChannelHandlerContext ctx){
        return ctx.channel().attr(_LOGIN).get();
    }

    public MqttChannel getChannel(String deviceId){
        return channels.get(deviceId);
    }

    public void setChannelAttr(ChannelHandlerContext ctx, String deviceId, Integer dbId){
        Channel channel = ctx.channel();
        channel.attr(_LOGIN).set(true);
        channel.attr(_DEVICE_ID).set(deviceId);
        channel.attr(_DB_ID).set(dbId);
    }

    public MqttChannel getChannel(ChannelHandlerContext ctx){ return channels.get(getDeviceId(ctx)); }

    public void setChannel(String deviceId, MqttChannel mqttChannel){
        channels.put(deviceId, mqttChannel);
    }

    public ConcurrentHashMap<Integer, MqttSendMessage> getSendMessages(ChannelHandlerContext ctx){ return channels.get(getDeviceId(ctx)).getSendMessages(); }

    public ConcurrentHashMap<Integer, MqttSendMessage> getReplyMessages(ChannelHandlerContext ctx){ return channels.get(getDeviceId(ctx)).getReplyMessages(); }

    public ConcurrentHashMap<String, MqttChannel> getChannels(){
        return channels;
    }

    public void closeChannel(ChannelHandlerContext ctx){
        String deviceId = getDeviceId(ctx);
        MqttChannel mqttChannel = channels.get(deviceId);
        if( mqttChannel != null ) {
            ConcurrentSkipListSet<String> topicNames = mqttChannel.getTopics();
            for(String topicName : topicNames){
                mqttTopicApi.remove(deviceId, topicName);
            }
            //
            mqttChannel.getCtx().channel().close();
            channels.remove(deviceId);
        }
    }

    public void updateActiveTime(ChannelHandlerContext ctx){
        MqttChannel mqttChannel = getChannel(ctx);
        mqttChannel.setActiveTime(new Date().getTime());
    }

    public Boolean checkLogin(ChannelHandlerContext ctx){
        if( !hasAttr(ctx, _LOGIN) ) {
            return false;
        }
        return isLogin(ctx);
    }

    public void checkAlive(){
        for (String deviceId: channels.keySet()) {
            MqttChannel mqttChannel = channels.get(deviceId);
            if( checkOvertime(mqttChannel.getActiveTime(), mqttChannel.getKeepAlive()) ){
                /**
                 * 在1.5个心跳周期内没有收到心跳包，则断开与客户端的链接
                 */
                log.info("客户端（"+mqttChannel.getDeviceId()+"）心跳超时，强制断开链接");
                closeChannel(mqttChannel.getCtx());
            }
        }
    }

    <T> boolean hasAttr(ChannelHandlerContext ctx, AttributeKey<T> attr){
        return ctx.channel().hasAttr(attr);
    }

    private boolean checkOvertime(long activeTime, long keepAlive) { return System.currentTimeMillis()-activeTime>=keepAlive*1.5*1000; }

}
