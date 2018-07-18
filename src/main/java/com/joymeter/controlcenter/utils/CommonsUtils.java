package com.joymeter.controlcenter.utils;

import java.net.URLDecoder;
import java.net.URLEncoder;

/**
 * @ClassName CommonsUtils
 * @Description TODO
 * @Author liang
 * @Date 2018/7/17 14:30
 * @Version 1.0
 **/
public class CommonsUtils {

    public static String toURLEncoded(String paramString) {
        if (paramString == null || paramString.equals("")) {
            System.out.println("toURLEncoded error:"+paramString);
            return "";
        }

        try
        {
            String str = new String(paramString.getBytes(), "UTF-8");
            str = URLEncoder.encode(str, "UTF-8");
            return str;
        }
        catch (Exception localException)
        {
            //LogE("toURLEncoded error:"+paramString, localException);
        }

        return "";
    }

    //json解码
    public static String toURLDecoded(String paramString) {
        if (paramString == null || paramString.equals("")) {
            //LogD("toURLDecoded error:"+paramString);
            return "";
        }

        try
        {
            String str = new String(paramString.getBytes(), "UTF-8");
            str = URLDecoder.decode(str, "UTF-8");
            return str;
        }
        catch (Exception localException)
        {
            //LogE("toURLDecoded error:"+paramString, localException);
        }

        return "";
    }
}
