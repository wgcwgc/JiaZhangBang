package com.runcom.jiazhangbang.login;

import android.util.Base64;

/**
 * 使用Base64来保存和获取密码数据
 */
public class Base64Utils
{

	/**
	 * Base64编码
	 * 
	 * @param 要编码的字符串
	 * @return 编码完成的字符串码
	 */
	public static String getEncodeStr(String str )
	{
		byte byteArr[] = Base64.encode(str.getBytes() ,Base64.DEFAULT);
		return new String(byteArr);
	}

	/**
	 * Base64解码
	 * 
	 * @param 被编码的字符串
	 * @return 解码完成，输出原本字符串
	 */
	public static String getDecodeStr(String encodeStr )
	{
		byte byteArr[] = Base64.decode(encodeStr ,Base64.DEFAULT);
		return new String(byteArr);
	}

}
