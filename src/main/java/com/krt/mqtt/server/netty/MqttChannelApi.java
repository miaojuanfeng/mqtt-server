package com.krt.mqtt.server.netty;

import com.krt.mqtt.server.beans.MqttChannel;
import com.krt.mqtt.server.beans.MqttSendMessage;
import com.krt.mqtt.server.service.DeviceService;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class MqttChannelApi {

    public static final AttributeKey<Boolean> _login = AttributeKey.valueOf("login");

    public static final AttributeKey<Integer> _dbId = AttributeKey.valueOf("dbId");

    public static final AttributeKey<String> _deviceId = AttributeKey.valueOf("deviceId");

    @Autowired
    private DeviceService deviceService;

    /**
     * 已连接到服务器端的通道
     */
    private static ConcurrentHashMap<String, MqttChannel> channels = new ConcurrentHashMap<>();

    public String getChannelDeviceId(ChannelHandlerContext ctx){
        return ctx.channel().attr(_deviceId).get();
    }

    public Integer getChannelDbId(ChannelHandlerContext ctx){
        return ctx.channel().attr(_dbId).get();
    }

    public Boolean getIsLogin(ChannelHandlerContext ctx){
        return ctx.channel().attr(_login).get();
    }

    public void setChannelAttr(ChannelHandlerContext ctx, String deviceId, Integer dbId){
        Channel channel = ctx.channel();
        channel.attr(_login).set(true);
        channel.attr(_dbId).set(dbId);
        channel.attr(_deviceId).set(deviceId);
    }

    public MqttChannel getChannel(String deviceId){
        return channels.get(deviceId);
    }

    public MqttChannel getChannel(ChannelHandlerContext ctx){
        String deviceId = getChannelDeviceId(ctx);
        return channels.get(deviceId);
    }

    public void setChannel(String deviceId, MqttChannel mqttChannel){
        channels.put(deviceId, mqttChannel);
    }

    public ConcurrentHashMap<Integer, MqttSendMessage> getSendMessages(ChannelHandlerContext ctx){
        return channels.get(getChannelDeviceId(ctx)).getSendMessages();
    }

    public ConcurrentHashMap<Integer, MqttSendMessage> getReplyMessages(ChannelHandlerContext ctx){
        return channels.get(getChannelDeviceId(ctx)).getReplyMessages();
    }

    public ConcurrentHashMap<String, MqttChannel> getChannels(){
        return channels;
    }

    public void closeChannel(ChannelHandlerContext ctx){
        Integer id = getChannelDbId(ctx);
        String deviceId = getChannelDeviceId(ctx);
        MqttChannel mqttChannel = channels.get(deviceId);
        if( mqttChannel != null ) {
            deviceService.doLogout(id);
            mqttChannel.getCtx().channel().close();
            channels.remove(deviceId);
        }
    }

    public void updateActiveTime(ChannelHandlerContext ctx){
        MqttChannel mqttChannel = getChannel(ctx);
        mqttChannel.setActiveTime(new Date().getTime());
    }

    public Boolean checkLogin(ChannelHandlerContext ctx){
        if( !hasAttr(ctx, _login) ) {
            return false;
        }
        return getIsLogin(ctx);
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

    private boolean checkOvertime(long activeTime, long keepAlive) {
        return System.currentTimeMillis()-activeTime>=keepAlive*1.5*1000;
    }
}
