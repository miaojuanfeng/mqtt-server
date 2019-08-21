package com.krt.mqtt.server.ir.core;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.krt.mqtt.server.ir.constant.Constants;
import com.krt.mqtt.server.ir.entity.ACStatus;
import com.krt.mqtt.server.ir.entity.IRCode;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;

import java.io.*;

@Slf4j
public class IRDecode {

    private static native int irOpen(Logger log, int category, int subCate, String fileName);

    private static native int irOpenBinary(Logger log, int category, int subCate, byte[] binaries, int binLength);

    private static native int[] irDecode(Logger log, int keyCode, ACStatus acStatus, int changeWindDirection);

    private static native IRCode mqttEncode(Logger log, int[] code);

    private static Object lock = new Object();

    static {
        System.load(Constants.LIB_FILE);
    }

    public static String decode(Integer categoryID, Integer subCate, String fileName, Integer keyCode, ACStatus acStatus, Integer cwd, Integer VER){
        synchronized (lock) {
            if (Constants.ERROR_CODE_SUCCESS != irOpen(log, categoryID, subCate, Constants.CODE_PATH + fileName)) {
                log.error("红外码库文件打开失败：" + Constants.CODE_PATH + fileName);
                return null;
            }
            int[] code = irDecode(log, keyCode, acStatus, cwd);
            IRCode irCode = mqttEncode(log, code);
            JSONObject retval = new JSONObject();
            retval.put("CMD", Constants.IR_CMD);
            retval.put("LEN", irCode.len);
            retval.put("DATA", irCode.ir);
            retval.put("REP", irCode.dup);
            retval.put("CRC", irCode.crc);
            retval.put("VER", VER);
            return retval.toString();
        }
    }

//    public static byte[] toByteArray(String filename) throws IOException {
//
//        File f = new File(filename);
//        if (!f.exists()) {
//            throw new FileNotFoundException(filename);
//        }
//
//        ByteArrayOutputStream bos = new ByteArrayOutputStream((int) f.length());
//        BufferedInputStream in = null;
//        try {
//            in = new BufferedInputStream(new FileInputStream(f));
//            int buf_size = 1024;
//            byte[] buffer = new byte[buf_size];
//            int len = 0;
//            while (-1 != (len = in.read(buffer, 0, buf_size))) {
//                bos.write(buffer, 0, len);
//            }
//            return bos.toByteArray();
//        } catch (IOException e) {
//            e.printStackTrace();
//            throw e;
//        } finally {
//            try {
//                in.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//            bos.close();
//        }
//    }

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
