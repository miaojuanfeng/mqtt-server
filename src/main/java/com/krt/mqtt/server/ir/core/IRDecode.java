package com.krt.mqtt.server.ir.core;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.krt.mqtt.server.ir.constant.Constants;
import com.krt.mqtt.server.ir.entity.ACStatus;
import com.krt.mqtt.server.ir.entity.IRCode;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class IRDecode {

    public static native int irOpen(int category, int subCate, String fileName);

    public static native int[] irDecode(int keyCode, ACStatus acStatus, int changeWindDirection);

    public static native IRCode mqttEncode(int[] code);

    static {
        System.load(Constants.LIB_FILE);
    }

    public static String decode(Integer categoryID, Integer subCate, String fileName, Integer keyCode, ACStatus acStatus, Integer cwd){
        if( Constants.ERROR_CODE_SUCCESS != irOpen(categoryID, subCate, Constants.CODE_PATH + fileName) ){
            log.error("红外码库文件打开失败：" + Constants.CODE_PATH + fileName);
            return null;
        }
        int[] code = irDecode(keyCode, acStatus, cwd);
        IRCode irCode = mqttEncode(code);
        JSONArray retval = new JSONArray();
        retval.add(irCode.len);
        retval.add(irCode.ir);
        retval.add(irCode.dup);
        return retval.toString();
    }

//    public static void main(String[] args){
//        System.out.println(irOpen(Constants.CategoryID.TV.getValue(), 1, Constants.DIR_PATH + "irda_upd6124_remote_tv_322.bin"));
//        int[] code = irDecode(3, new ACStatus(), 0);
//        System.out.println(code.length);
//        IRCode ir = mqttEncode(code);
//        System.out.println("ir.len: "+ir.len);
//        for( int i=0; i<ir.len; i++ ) {
//            System.out.print(ir.ir[i]+" ");
//        }
//        System.out.println();
//        for( int i=0; i<ir.len; i++ ) {
//            System.out.print(ir.dup[i]+" ");
//        }
//    }
}
