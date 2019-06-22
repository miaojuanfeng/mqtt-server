package com.krt.mqtt.server.netty;

import com.krt.mqtt.server.beans.MqttChannel;
import com.krt.mqtt.server.entity.Device;
import com.krt.mqtt.server.service.DeviceService;
import com.krt.mqtt.server.utils.SpringUtil;
import io.netty.channel.*;
import io.netty.handler.codec.mqtt.*;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@ChannelHandler.Sharable
@Slf4j
@Component
public class NettyServerHandler extends SimpleChannelInboundHandler<MqttMessage> {

    private MqttMessageService mqttMessageService;

    private MqttChannelApi mqttChannelApi;

    private MqttMessageApi mqttMessageApi;

    private DeviceService deviceService;

    public NettyServerHandler(){
        mqttMessageService = SpringUtil.getBean(MqttMessageService.class);
        mqttChannelApi = SpringUtil.getBean(MqttChannelApi.class);
        mqttMessageApi = SpringUtil.getBean(MqttMessageApi.class);
        deviceService = SpringUtil.getBean(DeviceService.class);
    }

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
         * 处理客户端连接报文
         */
        if( mqttMessage.fixedHeader().messageType().equals(MqttMessageType.CONNECT) ){
            MqttConnectMessage mqttConnectMessage = (MqttConnectMessage) mqttMessage;
            if( mqttConnectMessage.variableHeader().version() != MqttVersion.MQTT_3_1_1.protocolLevel() ){
                mqttMessageApi.CONNACK(ctx, mqttConnectMessage.fixedHeader().isDup(), MqttConnectReturnCode.CONNECTION_REFUSED_UNACCEPTABLE_PROTOCOL_VERSION);
                return;
            }
            if( !mqttConnectMessage.variableHeader().hasUserName() || !mqttConnectMessage.variableHeader().hasPassword() ){
                mqttMessageApi.CONNACK(ctx, mqttConnectMessage.fixedHeader().isDup(), MqttConnectReturnCode.CONNECTION_REFUSED_NOT_AUTHORIZED);
                return;
            }
            if( "".equals(deviceId) ){
                mqttMessageApi.CONNACK(ctx, mqttConnectMessage.fixedHeader().isDup(), MqttConnectReturnCode.CONNECTION_REFUSED_IDENTIFIER_REJECTED);
                return;
            }
            String userName = mqttConnectMessage.payload().userName();
            String password = new String(mqttConnectMessage.payload().passwordInBytes(), CharsetUtil.UTF_8);
            Integer dbId = deviceService.doLogin(deviceId, userName, password);
            if( dbId == null ){
                mqttMessageApi.CONNACK(ctx, mqttConnectMessage.fixedHeader().isDup(), MqttConnectReturnCode.CONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD);
                return;
            }
            /**
             * 设置客户端通道属性
             */
            mqttChannelApi.setChannelAttr(ctx, deviceId, dbId);

            mqttMessageService.replyCONNECT(ctx, (MqttConnectMessage) mqttMessage);
            return;
        }
        /**
         * 收到客户端发来的任何报文，包括但不限于PINGREQ，
         * 则证明客户端存活，需要更新客户端活跃时间
         */
        mqttChannelApi.updateActiveTime(ctx);
        /**
         * 处理客户端发来的其他类型报文
         */
        switch (mqttMessage.fixedHeader().messageType()){
            case DISCONNECT:
                mqttChannelApi.closeChannel(ctx);
                break;
            case PINGREQ:
                mqttMessageApi.PINGRESP(ctx);
                break;
            case PUBLISH:
                mqttMessageService.replyPUBLISH(ctx, (MqttPublishMessage) mqttMessage);
                break;
            case PUBACK:
                mqttMessageService.replyPUBACK(ctx, (MqttPubAckMessage) mqttMessage);
                break;
            case PUBREC:
                mqttMessageService.replyPUBREC(ctx, (MqttPubAckMessage) mqttMessage);
                break;
            case PUBREL:
                mqttMessageService.replyPUBREL(ctx, mqttMessage);
                break;
            case PUBCOMP:
                mqttMessageService.replyPUBCOMP(ctx, mqttMessage);
                break;
            case SUBSCRIBE:
                mqttMessageService.replySUBSCRIBE(ctx, (MqttSubscribeMessage) mqttMessage);
                break;
            case UNSUBSCRIBE:
                mqttMessageService.replyUNSUBSCRIBE(ctx, (MqttUnsubscribeMessage) mqttMessage);
                break;
        }
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
