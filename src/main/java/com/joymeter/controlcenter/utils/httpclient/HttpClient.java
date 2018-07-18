package com.joymeter.controlcenter.utils.httpclient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.SocketConfig;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

/**
 * 1. 通过HttpClient实现Get方法响应 <br>
 * 2. 通过HttpClient实现Post方法带参数传入的响应
 */
public class HttpClient {

    private static final CloseableHttpClient HTTP_CLIENT;

    static {
        RequestConfig.Builder requestConfigBuilder = RequestConfig.custom();
        requestConfigBuilder.setConnectTimeout(30000);//客户端和服务器建立连接的timeout
        requestConfigBuilder.setConnectionRequestTimeout(30000);//从连接池获取连接的timeout
        requestConfigBuilder.setSocketTimeout(30000);//连接建立后，request没有回应的timeout
        HttpClientBuilder clientBuilder = HttpClientBuilder.create();
        clientBuilder.setDefaultRequestConfig(requestConfigBuilder.build());
        clientBuilder.setDefaultSocketConfig(SocketConfig.custom().setSoTimeout(30000).build()); //连接建立后，request没有回应的timeout  
        clientBuilder.setKeepAliveStrategy(new DefaultConnectionKeepAliveStrategy());
        clientBuilder.setMaxConnTotal(500);
        clientBuilder.setMaxConnPerRoute(100);
        HTTP_CLIENT = clientBuilder.build();
    }

    /**
     * 检测Url的合法性
     *
     * @param url
     * @return
     */
    public static boolean checkUrl(final String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }
        String regex = "^([hH][tT]{2}[pP]:/*|[hH][tT]{2}[pP][sS]:/*|[fF][tT][pP]:/*)(([A-Za-z0-9-~]+).)+([A-Za-z0-9-~\\/])+(\\?{0,1}(([A-Za-z0-9-~]+\\={0,1})([A-Za-z0-9-~]*)\\&{0,1})*)$";
        Pattern pattern = Pattern.compile(regex);
        return pattern.matcher(url).matches();
    }

    /**
     * 向指定URL发送GET方法的请求
     *
     * @param url 发送请求的URL
     * @param param 请求参数 此方法用/拼接地址和字符
     * @return
     */
    public static String sendGet(String url, String param) {
        if (!HttpClient.checkUrl(url)) {
            return null;
        }
        StringBuilder result = new StringBuilder();
        BufferedReader in = null;
        try {
            String urlNameString = url + "/" + param;
            URL realUrl = new URL(urlNameString);
            URLConnection connection = realUrl.openConnection();
            connection.setRequestProperty("accept", "*/*");
            connection.setRequestProperty("connection", "Keep-Alive");
            connection.setRequestProperty("user-agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
            connection.setRequestProperty("Accept-Charset", "utf-8");
            connection.setRequestProperty("contentType", "utf-8");
            connection.connect();
            in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                result.append(line);
            }
        } catch (IOException e) {
            Logger.getLogger(HttpClient.class.getName()).log(Level.SEVERE, null, e);
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException e) {
                Logger.getLogger(HttpClient.class.getName()).log(Level.SEVERE, null, e);
            }
        }
        return result.toString();
    }

    /**
     * 向服务器发送post请求
     *
     * @param url
     * @param params
     * @return
     */
    public static String sendPost(String url, Map<String, String> params) {
        if (url == null || url.isEmpty() || params == null || params.isEmpty()) {
            return null;
        }
        if (!HttpClient.checkUrl(url)) {
            return null;
        }
        org.apache.http.client.methods.CloseableHttpResponse response = null;
        HttpPost request = new HttpPost(url);
        List<NameValuePair> nameValues = new ArrayList<>();
        try {
            params.entrySet().forEach((entry) -> {
                nameValues.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
            });
            request.setEntity(new UrlEncodedFormEntity(nameValues, "utf-8"));
            response = HTTP_CLIENT.execute(request);
            if (response.getStatusLine().getStatusCode() == 200) {
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    return EntityUtils.toString(entity, "utf-8");
                }
            }
        } catch (IOException ex) {
            request.abort();
            Logger.getLogger(HttpClient.class.getName()).log(Level.SEVERE, null, ex);
            String msg = String.format("sendPost failed url: %s, json: %s", url, params.toString());
            HttpClient.log(msg);
        } finally {
            if (null != response) {
                try {
                    response.close();
                } catch (IOException ex) {
                    Logger.getLogger(HttpClient.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        return null;
    }

    /**
     * 向服务器发送post请求
     *
     * @param url
     * @param json
     * @return
     */
    public static String sendPost(final String url, final String json) {
        Map<String, String> headers = new HashMap<>();
        headers.put("content-type", "application/json");
        return HttpClient.sendPost(url, json, headers);
    }

    /**
     * 向服务器发送post请求
     *
     * @param url
     * @param json
     * @param header
     * @return
     */
    public static String sendPost(final String url, final String json, Map<String, String> header) {
        if (url == null || url.isEmpty() || json == null || json.isEmpty() || header == null || header.isEmpty()) {
            return null;
        }
        if (!HttpClient.checkUrl(url)) {
            return null;
        }
        HttpPost request = new HttpPost(url);
        StringEntity params = new StringEntity(json, "utf-8");
        HttpClient.setHttpHeader(request, header);
        request.setEntity(params);
        org.apache.http.client.methods.CloseableHttpResponse response = null;
        try {
            response = HTTP_CLIENT.execute(request);
            if (response.getStatusLine().getStatusCode() == 200) {
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    return EntityUtils.toString(entity, "utf-8");
                }
            }
        } catch (IOException ex) {
            request.abort();
            Logger.getLogger(HttpClient.class.getName()).log(Level.SEVERE, null, ex);
            String msg = String.format("sendPost failed url: %s, json: %s", url, json);
            HttpClient.log(msg);
        } finally {
            if (null != response) {
                try {
                    response.close();
                } catch (IOException ex) {
                    Logger.getLogger(HttpClient.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        return null;
    }

    /**
     * 日志输出
     *
     * @param msg
     */
    private static void log(String msg) {
        //Logger.LogTrace(msg);
        Logger.getLogger(HttpClient.class.getName()).log(Level.SEVERE, msg);
    }

    /**
     * 设置http的头信息
     *
     * @param request
     * @param map
     */
    public static void setHttpHeader(HttpPost request, Map<String, String> map) {
        map.entrySet().forEach((entry) -> {
            String key = entry.getKey();
            String value = entry.getValue();
            request.addHeader(key, value);
        });
    }
}
