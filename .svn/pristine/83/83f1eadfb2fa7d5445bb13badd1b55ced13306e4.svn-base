package com.krt.mqtt.server.utils;

import org.springframework.util.Base64Utils;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * 
　* 项目名称：get-data-netty   
　* 类名称：AesUtils   
　* 类描述：   AES加密解密
　* 创建人：郭明德  
　* 创建时间：2019-4-17 下午4:37:15
 */
public class AesUtil {

    /**
     * @param args
     */
    /**
     * 将二进制转换成16进制
     *
     * @param buf
     * @return
     */
    public static String parseByte2HexStr(byte[] buf) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < buf.length; i++) {
            String hex = Integer.toHexString(buf[i] & 0xFF);
            if (hex.length() == 1) {
                hex = '0' + hex;
            }
            sb.append(hex.toUpperCase());
        }
        return sb.toString();
    }

    /**
     * 将16进制转换为二进制
     *
     * @param hexStr
     * @return
     */
    public static byte[] parseHexStr2Byte(String hexStr) {
        int temp = 2;
        if (hexStr.length() < 1) {
            return null;
        }
        byte[] result = new byte[hexStr.length() / temp];
        for (int i = 0; i < hexStr.length() / temp; i++) {
            int high = Integer.parseInt(hexStr.substring(i * 2, i * 2 + 1), 16);
            int low = Integer.parseInt(hexStr.substring(i * 2 + 1, i * 2 + 2), 16);
            result[i] = (byte) (high * 16 + low);
        }
        return result;
    }

    /**
     * 加密
     *
     * @param content
     * @param aes
     * @return
     */
    public static byte[] getAESEncrypt(byte[] content, byte[] aes) {
        if (content == null || content.length < 0 || aes == null || aes.length < 0) {
            System.out.println("key or content is null");
            return null;
        }
        try {
            //补齐16位
            byte[] contentTemp;
            int w = content.length % 16;
            if (w != 0) {
                contentTemp = new byte[content.length + 16 - w];
                for (int i = 0; i < content.length; i++) {
                    contentTemp[i] = content[i];
                }
                ;
            } else {
                contentTemp = content;
            }
            SecretKeySpec sks = new SecretKeySpec(aes, "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, sks);
            byte[] jiamihou = cipher.doFinal(contentTemp);
            return jiamihou;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 解密
     *
     * @param content
     * @param aes
     * @return
     */
    public static byte[] getAESDecrypt(byte[] content, byte[] aes) {
        if (content == null || content.length == 0 || aes == null || aes.length < 0) {
            System.out.println("key or content is null");
            return null;
        }
        try {
            SecretKeySpec sks = new SecretKeySpec(aes, "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, sks);
            byte[] jiemihou = cipher.doFinal(content);
            //去除多余空字节(0)
            return jiemihou;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 加密
     *
     * @param content
     * @param aes
     * @return
     * @throws UnsupportedEncodingException
     */
    public static String getAESEncrypt(String content, String aes) throws UnsupportedEncodingException {
        return parseByte2HexStr(getAESEncrypt(content.getBytes("UTF-8"), aes.getBytes()));

    }

    /**
     * 加密
     *
     * @param content
     * @param aes
     * @return
     * @throws UnsupportedEncodingException
     */
    public static String getAESAndBase64Encrypt(String content, String aes) throws UnsupportedEncodingException {
        content = Base64Util.encode(content.getBytes("UTF-8"), 0, content.getBytes("UTF-8").length);
        return parseByte2HexStr(getAESEncrypt(content.getBytes("UTF-8"), aes.getBytes()));

    }

    /**
     * 解密
     *
     * @param content
     * @param aes
     * @return
     * @throws IOException
     * @throws UnsupportedEncodingException
     */
    public static String getAESAndBase64Decrypt(String content, String aes) throws UnsupportedEncodingException, IOException {
        content = new String(getAESDecrypt(parseHexStr2Byte(content), aes.getBytes()), "UTF-8").replaceAll("\0", "");
        return new String(Base64Util.decode(content.toString()), "UTF-8");
    }

    /**
     * 解密
     *
     * @param content
     * @param aes
     * @return
     * @throws IOException
     * @throws UnsupportedEncodingException
     */
    public static String getAESDecrypt(String content, String aes) throws UnsupportedEncodingException, IOException {
        return new String(getAESDecrypt(parseHexStr2Byte(content), aes.getBytes()), "UTF-8").replaceAll("\0", "");
    }

//    public static void main(String[] args) {
//        try {
//            // 加密
//            String a = AesUtil.getAESEncrypt("T100011282", "1234567890123456");
//            System.out.println(a);
//            // 解密
//            System.out.println(AesUtil.getAESDecrypt(a, "1234567890123456"));
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
}