package com.krt.mqtt.server.ir.core;

import com.krt.mqtt.server.ir.constant.Constants;
import com.krt.mqtt.server.ir.entity.ACStatus;
import com.krt.mqtt.server.ir.entity.IRCode;

public class IRDecode {

    public static final String dirPath = "/home/krt/iot/test/";

    public static native int irOpen(int category, int subCate, String fileName);

    public static native int[] irDecode(int keyCode, ACStatus acStatus, int changeWindDirection);

    public static native IRCode mqttEncode(int[] code);

    static {
        System.load(dirPath + "libirda_decoder.so");
    }

    public static void main(String[] args){
        System.out.println(irOpen(Constants.CategoryID.TV.getValue(), 1, dirPath + "irda_upd6124_remote_tv_322.bin"));
        int[] code = irDecode(3, new ACStatus(), 0);
        System.out.println(code.length);
        IRCode ir = mqttEncode(code);
        System.out.println("ir.len: "+ir.len);
        for( int i=0; i<ir.len; i++ ) {
            System.out.print(ir.ir[i]+" ");
        }
        System.out.println();
        for( int i=0; i<ir.len; i++ ) {
            System.out.print(ir.dup[i]+" ");
        }
    }
}
