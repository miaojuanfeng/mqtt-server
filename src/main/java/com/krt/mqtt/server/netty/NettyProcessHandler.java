package com.krt.mqtt.server.netty;

import com.alibaba.fastjson.JSONObject;
import com.krt.mqtt.server.constant.CommonConst;
import com.krt.mqtt.server.constant.SystemTopicConst;
import com.krt.mqtt.server.entity.DeviceCommand;
import com.krt.mqtt.server.entity.DeviceData;
import com.krt.mqtt.server.service.DeviceService;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.*;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;

@Slf4j
@Component
public class NettyProcessHandler {

    @Autowired
    private MqttMessageService mqttMessageService;

    @Autowired
    private MqttChannelApi mqttChannelApi;

    @Autowired
    private MqttMessageApi mqttMessageApi;

    @Autowired
    private DeviceService deviceService;

    public void process(ChannelHandlerContext ctx, MqttMessage mqttMessage, Date insertTime){
        /**
         * 通道deviceId
         */
        String deviceId = mqttChannelApi.getDeviceId(ctx);
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
                mqttMessageService.replyPUBLISH(ctx, (MqttPublishMessage) mqttMessage, insertTime);
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

    public void publish(ChannelHandlerContext ctx, String subjectName, String subjectContent, Date insertTime){
        if( subjectName != null && subjectContent != null ){
            String[] segmentName = subjectName.split("/");
            if( segmentName.length < 7 ){
                log.error("主题名称格式错误："+subjectName);
                return;
            }
            JSONObject obj = null;
            try {
                switch (segmentName[1]){
                    case SystemTopicConst.PREFIX_SYS:
                        // 这里用位移掩码来做
                        switch (segmentName[5]){
                            case "data":
                                switch (segmentName[6]){
                                    case "post":
                                        cacheData(new DeviceData(mqttChannelApi.getDeviceId(ctx), subjectContent, mqttChannelApi.getDbId(ctx), insertTime));
                                        break;
                                }
                                break;
                            case "cmd":
                                switch (segmentName[6]){
                                    case "set":
                                        cacheCommand(new DeviceCommand(mqttChannelApi.getDeviceId(ctx), subjectContent, mqttChannelApi.getDbId(ctx), insertTime));
                                        break;
                                }
                                break;
                        }
                        break;
//                    case SystemTopicConst.PREFIX_DATA:
//                        obj = JSONObject.parseObject(subjectContent);
//                        if( obj != null ) {
//                            log.info(obj.toString());
//                        }
//                        break;
//                    case SystemTopicConst.PREFIX_OTA:
//                        obj = JSONObject.parseObject(subjectContent);
//                        if( obj != null ) {
//                            log.info(obj.toString());
//                        }
//                        break;
//                    case SystemTopicConst.PREFIX_SHADOW:
//                        obj = JSONObject.parseObject(subjectContent);
//                        if( obj != null ) {
//                            log.info(obj.toString());
//                        }
//                        break;
                }
            }catch (Exception e){
                log.error("捕获异常："+e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void cacheData(DeviceData deviceData){
        long time = deviceData.getInsertTime().getTime();
        CommonConst.DEVICE_DATA_THREAD_ARRAY[getIndex(time)].insertDeviceData(deviceData);
    }

    private void cacheCommand(DeviceCommand deviceCommand){
        long time = deviceCommand.getInsertTime().getTime();
        CommonConst.DEVICE_DATA_THREAD_ARRAY[getIndex(time)].insertDeviceCommand(deviceCommand);
    }

    private int getIndex(long time){
        return ((int)(time&(1<<10)-1)%CommonConst.DEVICE_DATA_THREAD_SIZE);
    }
}
