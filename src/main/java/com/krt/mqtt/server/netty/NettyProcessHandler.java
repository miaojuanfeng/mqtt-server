package com.krt.mqtt.server.netty;

import com.alibaba.fastjson.JSONObject;
import com.krt.mqtt.server.beans.MqttChannel;
import com.krt.mqtt.server.constant.CommonConst;
import com.krt.mqtt.server.constant.MqttMessageStateConst;
import com.krt.mqtt.server.constant.SystemTopicConst;
import com.krt.mqtt.server.entity.Device;
import com.krt.mqtt.server.entity.DeviceCommand;
import com.krt.mqtt.server.entity.DeviceData;
import com.krt.mqtt.server.entity.ExistLog;
import com.krt.mqtt.server.ir.constant.Constants;
import com.krt.mqtt.server.ir.core.IRDecode;
import com.krt.mqtt.server.ir.entity.ACStatus;
import com.krt.mqtt.server.service.DeviceService;
import com.krt.mqtt.server.service.ExistLogService;
import com.krt.mqtt.server.utils.MessageIdUtil;
import io.netty.channel.Channel;
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

    @Autowired
    private ExistLogService existLogService;

    @Autowired
    private MqttResendApi mqttResendApi;

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
            /**
             * 回复登录确认
             */
            mqttMessageService.replyCONNECT(ctx, (MqttConnectMessage) mqttMessage, insertTime);
            /**
             * 上线日志
             */
            mqttChannelApi.updateDeviceState(dbId, CommonConst.DEVICE_STATE_ONLINE);
            existLogService.insert(new ExistLog(deviceId, CommonConst.DEVICE_ONLINE, insertTime));
            return;
        }
        /**
         * 处理客户端发来的其他类型报文
         */
        switch (mqttMessage.fixedHeader().messageType()){
            case DISCONNECT:
                mqttChannelApi.closeChannel(ctx, insertTime);
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
            switch (segmentName[1]){
                case SystemTopicConst.PREFIX_SYS:
                    // 这里用位移掩码来做
                    switch (segmentName[5]){
                        case "data":
                            switch (segmentName[6]){
                                case "post":
                                case "ack":
                                    cacheData(new DeviceData(mqttChannelApi.getDeviceId(ctx), subjectContent, mqttChannelApi.getDbId(ctx), insertTime));
                                    break;
                            }
                            break;
                        case "cmd":
                            switch (segmentName[6]){
                                case "set":
                                case "ack":
                                    cacheCommand(new DeviceCommand(mqttChannelApi.getDeviceId(ctx), subjectContent, mqttChannelApi.getDbId(ctx), insertTime));
                                    break;
                            }
                            break;
                        case "ir":
                            switch (segmentName[6]){
                                case "set":
                                    cacheData(new DeviceData(mqttChannelApi.getDeviceId(ctx), subjectContent, mqttChannelApi.getDbId(ctx), insertTime));
                                    JSONObject obj = null;
                                    try {
                                        obj = JSONObject.parseObject(subjectContent);
                                    }catch (Exception e){
                                        log.error("主题内容错误：" + subjectContent);
                                        e.printStackTrace();
                                        return;
                                    }
                                    Integer categoryID = obj.getInteger("CI");
                                    Integer BinaryType = obj.getInteger("BT");
                                    String fileName = obj.getString("FN") + Constants.FILE_SUFFIX;
                                    Integer keyCode = obj.getInteger("KC");
                                    JSONObject acObj = obj.getJSONObject("AC");
                                    ACStatus acStatus = new ACStatus(
                                            acObj.getInteger("PW"),
                                            acObj.getInteger("MD"),
                                            acObj.getInteger("TP"),
                                            acObj.getInteger("WS"),
                                            acObj.getInteger("WD"),
                                            0,
                                            0,
                                            0
                                    );
                                    Integer acSwing = acObj.getInteger("WD");
                                    Integer VER = obj.getInteger("VER");

                                    String irContent = IRDecode.decode(
                                            categoryID,
                                            BinaryType,
                                            fileName,
                                            keyCode,
                                            acStatus,
                                            acSwing,
                                            VER
                                    );
                                    log.info(irContent);
                                    /**
                                     * 默认采用AT_LEAST_ONCE服务级别
                                     */
                                    MqttChannel channel = mqttChannelApi.getChannel(segmentName[3]);
                                    if( null == channel ){
                                        log.info("设备不在线：" + segmentName[3]);
                                        return;
                                    }
                                    Integer messageId = MessageIdUtil.messageId();
                                    String cmdSubjectName = "/sys/productId/"+segmentName[3]+"/thing/cmd/set";
                                    mqttResendApi.saveSendMessage(channel.getCtx(),
                                            messageId,
                                            cmdSubjectName,
                                            irContent.getBytes(),
                                            MqttQoS.AT_LEAST_ONCE,
                                            MqttMessageStateConst.PUB);
                                    mqttMessageApi.PUBLISH(channel.getCtx(), cmdSubjectName, irContent.getBytes(), messageId, MqttQoS.AT_LEAST_ONCE, false);
                                    cacheCommand(new DeviceCommand(segmentName[3], irContent, mqttChannelApi.getDbId(ctx), insertTime));
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

//    public static void cacheExistLog(ExistLog existLog){
//        long time = existLog.getTime().getTime();
//        CommonConst.DEVICE_DATA_THREAD_ARRAY[getIndex(time)].insertExistLog(existLog);
//    }

    private static int getIndex(long time){
        return ((int)(time&(1<<10)-1)%CommonConst.DEVICE_DATA_THREAD_SIZE);
    }
}
