#include <jni.h>
#include <stdio.h>
#include "ir_decode.h"
#include "include/ir_decode.h"

unsigned int crc32( const unsigned int *buf, unsigned int size)
{
	unsigned int i, crc;
	crc = 0xFFFFFFFF;

	for (i = 0; i < size; i++)
		crc = crc32tab[(crc ^ buf[i]) & 0xff] ^ (crc >> 8);

	return crc^0xFFFFFFFF;
}

JNIEXPORT jint JNICALL Java_com_krt_mqtt_server_ir_core_IRDecode_irOpen (JNIEnv *env, jobject this_obj, jint category_id, jint sub_cate, jstring file_name)
{
    const char *n_file_name = (*env)->GetStringUTFChars(env, file_name, 0);
    if (IR_DECODE_FAILED == ir_file_open(category_id, sub_cate, n_file_name))
    {
        ir_close();
        (*env)->ReleaseStringUTFChars(env, file_name, n_file_name);
        return IR_DECODE_FAILED;
    }

    (*env)->ReleaseStringUTFChars(env, file_name, n_file_name);
    return IR_DECODE_SUCCEEDED;
}

JNIEXPORT jintArray JNICALL Java_com_krt_mqtt_server_ir_core_IRDecode_irDecode (JNIEnv *env, jclass this_obj, jint key_code, jobject jni_ac_status, jint change_wind_direction)
{
    UINT16 user_data[USER_DATA_SIZE] = { 0 };
    int i = 0;
    jint copy_array[USER_DATA_SIZE] = { 0 };
    t_remote_ac_status ac_status;

    jclass n_ac_status = (*env)->GetObjectClass(env, jni_ac_status);

    if (NULL != n_ac_status)
    {
        jfieldID ac_power_fid = (*env)->GetFieldID(env, n_ac_status, "acPower", "I");
        jint i_ac_power = (*env)->GetIntField(env, jni_ac_status, ac_power_fid);

        jfieldID ac_mode_fid = (*env)->GetFieldID(env, n_ac_status, "acMode", "I");
        jint i_ac_mode = (*env)->GetIntField(env, jni_ac_status, ac_mode_fid);

        jfieldID ac_temp_fid = (*env)->GetFieldID(env, n_ac_status, "acTemp", "I");
        jint i_ac_temp = (*env)->GetIntField(env, jni_ac_status, ac_temp_fid);

        jfieldID ac_wind_dir_fid = (*env)->GetFieldID(env, n_ac_status, "acWindDir", "I");
        jint i_ac_wind_dir = (*env)->GetIntField(env, jni_ac_status, ac_wind_dir_fid);

        jfieldID ac_wind_speed_fid = (*env)->GetFieldID(env, n_ac_status, "acWindSpeed", "I");
        jint i_ac_wind_speed = (*env)->GetIntField(env, jni_ac_status, ac_wind_speed_fid);

        ac_status.ac_display = 0;
        ac_status.ac_sleep = 0;
        ac_status.ac_timer = 0;
        ac_status.ac_power = i_ac_power;
        ac_status.ac_mode = i_ac_mode;
        ac_status.ac_temp = i_ac_temp;
        ac_status.ac_wind_dir = i_ac_wind_dir;
        ac_status.ac_wind_speed = i_ac_wind_speed;

        ir_printf("ac status is not null : power = %d, mode = %d, "
                  "temp = %d, wind_dir = %d, wind_speed = %d\n",
                  ac_status.ac_power, ac_status.ac_mode,
                  ac_status.ac_temp, ac_status.ac_wind_dir,
                  ac_status.ac_wind_speed);
    }
    else
    {
        ir_printf("ac status is null, error!\n");
    }

    int wave_code_length = ir_decode(key_code, user_data, &ac_status, change_wind_direction);

    jintArray result = (*env)->NewIntArray(env, wave_code_length);
    if (result == NULL)
    {
        return NULL; /* out of memory error thrown */
    }
    for (i = 0; i < wave_code_length; i++)
    {
        copy_array[i] = (int)user_data[i];
    }
    (*env)->SetIntArrayRegion(env, result, 0, wave_code_length, copy_array);
    (*env)->DeleteLocalRef(env, n_ac_status);

    return result;
}

JNIEXPORT jobject JNICALL Java_com_krt_mqtt_server_ir_core_IRDecode_mqttEncode (JNIEnv *env, jobject this_obj, jintArray code)
{
	jint *n_code = (*env)->GetIntArrayElements(env, code, NULL);
	if( NULL == n_code ) return NULL;
	jsize n_len = (*env)->GetArrayLength(env, code);
	ir_printf("code n_len: %d\n", n_len);
	
	unsigned int crc = crc32(n_code, n_len);
	ir_printf("xxx: %u\n", crc);
	
	jint code_array[USER_DATA_SIZE] = { 0 };
	jint dup_array[USER_DATA_SIZE] = { 0 };
	jint array_len = 0;
	int o = 0;
	int c = 0;
	int d = 0;
	int i;
	for(i=0;i<n_len;i++){
		c = n_code[i];
		if( o == c ){
			d++;
		}else{
			if( 0 != o ){
				//ir_printf("%d*%d ", o, d);
				code_array[array_len] = o;
				dup_array[array_len] = d;
				array_len++;
			}
			o = c;
			d = 1;
		}
	}
	//ir_printf("%d*%d ", o, d);
	code_array[array_len] = o;
	dup_array[array_len] = d;
	array_len++;
	
	(*env)->ReleaseIntArrayElements(env, code, n_code, 0);
	
	
	jclass cls_ircode = (*env)->FindClass(env, "com/krt/mqtt/server/ir/entity/IRCode");
	jobject obj_ircode = (*env)->AllocObject(env, cls_ircode);
	
	jfieldID len_fid = (*env)->GetFieldID(env, cls_ircode, "len", "I");
	if( NULL == len_fid ){
		ir_printf("len_fid is null");
		return NULL;
	}
	(*env)->SetIntField(env, obj_ircode, len_fid, n_len);
	
	jfieldID ir_fid = (*env)->GetFieldID(env, cls_ircode, "ir", "[I");
	if( NULL == ir_fid ){
		ir_printf("ir is null");
		return NULL;
	}
	jintArray i_ir = (*env)->NewIntArray(env, array_len);
	(*env)->SetIntArrayRegion(env, i_ir, 0, array_len, code_array);
	(*env)->SetObjectField(env, obj_ircode, ir_fid, i_ir);
	
	jfieldID dup_fid = (*env)->GetFieldID(env, cls_ircode, "dup", "[I");
	if( NULL == dup_fid ){
		ir_printf("dup is null");
		return NULL;
	}
	jintArray i_dup = (*env)->NewIntArray(env, array_len);
	(*env)->SetIntArrayRegion(env, i_dup, 0, array_len, dup_array);
	(*env)->SetObjectField(env, obj_ircode, dup_fid, i_dup);
	
	return obj_ircode;
}