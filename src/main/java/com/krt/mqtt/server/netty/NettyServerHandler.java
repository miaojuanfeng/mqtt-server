package com.krt.mqtt.server.netty;

import com.krt.mqtt.server.beans.MqttChannel;
import com.krt.mqtt.server.constant.CommonConst;
import com.krt.mqtt.server.entity.Device;
import com.krt.mqtt.server.service.DeviceService;
import io.netty.channel.*;
import io.netty.handler.codec.mqtt.*;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;

@ChannelHandler.Sharable
@Slf4j
@Component
public class NettyServerHandler extends SimpleChannelInboundHandler<MqttMessage> {

    @Autowired
    private MqttMessageService mqttMessageService;

    @Autowired
    private MqttChannelApi mqttChannelApi;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, MqttMessage mqttMessage) throws Exception {
        /**
         * 客户端到服务端的网络连接建立后，客户端发送给服务端的第一个报文必须是CONNECT报文
         * 否则断开与该客户端的链接
         */
        if( !mqttMessage.fixedHeader().messageType().equals(MqttMessageType.CONNECT) && !mqttChannelApi.checkLogin(ctx) ){
            /**
             * 如果客户端在未登录成功的状态下发送CONNECT以外的报文，
             * 该行为违反协议规定，服务器立即断开与该客户端的连接
             */
            ctx.channel().close();
            return;
        }
        /**
         * 输出报文日志
         */
        String deviceId = "";
        if( mqttChannelApi.hasAttr(ctx, MqttChannelApi._DEVICE_ID) ) {
            deviceId = mqttChannelApi.getDeviceId(ctx);
        }else{
            deviceId = ((MqttConnectMessage) mqttMessage).payload().clientIdentifier();
            mqttChannelApi.setDeviceId(ctx, deviceId);
        }
        log.info("客户端（" + deviceId + "）发来报文: " + mqttMessage);
        /**
         * 收到客户端发来的任何报文，包括但不限于PINGREQ，
         * 则证明客户端存活，需要更新客户端活跃时间
         */
        mqttChannelApi.updateActiveTime(ctx);
        /**
         * 处理引用计数问题
         */
        if( mqttMessage.fixedHeader().messageType() == MqttMessageType.PUBLISH ){
            ((MqttPublishMessage)mqttMessage).payload().retain();
        }
        /**
         * 处理器线程处理
         */
        CommonConst.PROCESS_MANAGE_THREAD.insertMessage(ctx, mqttMessage, new Date());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("捕获通道异常: "+cause);
        cause.printStackTrace();
        mqttMessageService.broadcastWILL(ctx);
        mqttChannelApi.closeChannel(ctx);
//        super.exceptionCaught(ctx, cause);
    }
}
