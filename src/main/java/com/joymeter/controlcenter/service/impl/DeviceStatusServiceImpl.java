package com.joymeter.controlcenter.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.joymeter.controlcenter.dao.mapper.DeviceInfoMapper;
import com.joymeter.controlcenter.domain.DeviceInfo;
import com.joymeter.controlcenter.domain.QueryParam;
import com.joymeter.controlcenter.service.DeviceStatusService;
import com.joymeter.controlcenter.utils.CommonsUtils;
import com.joymeter.controlcenter.utils.PropertiesUtils;
import com.joymeter.controlcenter.utils.RedisUtils;
import com.joymeter.controlcenter.utils.httpclient.HttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @ClassName DeviceStatusServiceImpl
 * @Description TODO
 * @Author liang
 * @Date 2018/7/16 15:42
 * @Version 1.0
 **/
@Service
public class DeviceStatusServiceImpl implements DeviceStatusService {
    private static final Logger logger = Logger.getLogger(DeviceStatusServiceImpl.class.getName());
    private static String callBackUrl = PropertiesUtils.getProperty("callBackUrl", "");

    @Autowired
    private DeviceInfoMapper deviceInfoMapper;
    @Autowired
    private RedisUtils redisUtils;

    /**
     * 修改阀门状态,给业务层提供接口
     * 数据格式为：
     *  {
     *     "accountName":"操作人",
     *     "id":"dtuId",
     *     "valveId":"阀门编号",
     *     "action":"Close/Open"
     * }
     * 协议,有线 无线
     *
     */
    @Override
    public void changeValveState(String data) {
        if (StringUtils.isEmpty(data))return;
         try {
            JSONObject jsonData = JSONObject.parseObject(data);
            String accountName = jsonData.getString("accountName");
            String id = jsonData.getString("id");
            String valveId = jsonData.getString("valveId");
            String action = jsonData.getString("action");
             //通过阀门号获取设备信息
             DeviceInfo deviceInfo = deviceInfoMapper.getDeviceInfoByValveId(valveId);
             if(StringUtils.isEmpty(deviceInfo))return;
             //获取协议，category，接口url，
             String category = deviceInfo.getCategory();
             String ValveProtocol = deviceInfo.getValveProtocol()+"_"+action;
             if(CommonsUtils.isEmpty(ValveProtocol)) return;
             if(ValveProtocol.contains("JLAA")){
                //无线表
                changeJLAAValveState(valveId,action);
                return;
             }
             String gateWayurl = deviceInfo.getGatewayUrl();
             gateWayurl = "http://"+gateWayurl+"/arm/api/reading";
            //回调函数
             String callbackurl = "http://47.93.21.73:18081/deviceStatus/dtuCallBack";
             /*===============================*/
             //构建查询json
             QueryParam queryParam = new QueryParam();
             queryParam.setIsAutoClear("10");
             queryParam.setAccountName(accountName);
             queryParam.setDtuId(id);
             queryParam.setMeter(valveId);
             queryParam.setCallbackurl(callbackurl);
             queryParam.setCategory(category);
             queryParam.setProtocol(ValveProtocol);
             JSONObject dtuObj = queryParam.queryJsonFormat();
             ///-----------
             String sendMessage = dtuObj.toJSONString();

              HttpClient.sendPost(gateWayurl, sendMessage);
             logger.log(Level.INFO,"sendMessage:"+sendMessage);

         }catch (Exception e){
             logger.log(Level.SEVERE,null,e);
         }

    }

    /**
     * 无线：
     * 开阀：http://60.205.218.69:1841/joy/saas/v1/device/open
     * 关阀：http://60.205.218.69:1841/joy/saas/v1/device/close
     * {
     * "client_id":"123",
     * "access_token":"",
     * "device_id":"12345678"
     * }
     * @param valveId
     */
    public void changeJLAAValveState(String valveId,String action){
        String url = "http://60.205.218.69:1841/joy/saas/v1/device/";
        if("Close".equals(action)){
            url = url + "close";
        }else if("Open".equals(action)){
            url = url +"open";
        }
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("client_id"," ");
        jsonObject.put("access_token"," ");
        jsonObject.put("device_id",valveId);
        String s = HttpClient.sendPost(url, jsonObject.toJSONString());


    }


    /**
     * 接收dtu回调信息，解析操作是否成功
     * result: 0 成功 1 失败
     * 回调json格式：
     *  {"data9":{},"date":"2018-07-17 15:36:44","data8":"0","data7":"0","data6":"0","data5":"0","data4":"0","meter":"201703001320","data3":"0","data2":"0","data1":"3304.15","result":"0","options":{"isAutoClear":"1","balanceWarnning":"null","protocol":"com.joymeter.dtu.data.other.valve.ParseElecValveData_001_Close","concentrator_model":"2","accountName":"joy000001","dtuId":"","valveClose":"null","sendTime":"2018-07-17 11:15:23"},"category":"40"}
     * @param data
     * @return
     */
    @Override
    public Boolean dtuCallBack(String data) {
        try{
            //--解码
            String decoded = CommonsUtils.toURLDecoded(data);
            logger.log(Level.INFO,"回调结果为："+decoded);
            //转json
            JSONObject callBackObject = JSONObject.parseObject(decoded);
            if(callBackObject.containsKey("result")){
                String result =  callBackObject.getString("result");
                if("0".equals(result)){
                    //获取设备参数options，修改数据库
                    JSONObject options =callBackObject.getJSONObject("options");
                    String valveId = options.getString("valveId");
                    String action = options.getString("action");
                    String key = "DEVICESTATUS_"+valveId;
                    String status="{\"status\":\""+action +"\"}";

                    try {
                        //更新redis数据库
                        redisUtils.setKV(key,status);
                        //更新mysql数据库,valveState阀门状态(0 关, 1 开)
                        String valveState = "";
                        if ("Close".equals(action)) {
                            valveState = "0";
                        } else if ("Open".equals(action)){
                            valveState = "1";
                        }
                        logger.log(Level.INFO,"------测试：valveId："+valveId+"    valveState  :"+valveState);
                        deviceInfoMapper.updateValeStatus(valveId, valveState);
                    }catch (Exception e){
                        logger.log(Level.SEVERE,null,e);
                    }
                    return true;
                }
            }
            return null;
        }catch (Exception e){
            logger.log(Level.SEVERE,null,e);
            return null;
        }
    }

    /**
     * 获取设备实时数据状态
     * 数据格式为：
     *   {
     *      "accountName":"操作人",
     *      "dtuId":"dtuId",
     *      "deviceId":"设备编号",
     *      "callbackurl":callbackurl
     *  }
     * @param data
     */
    @Override
    public void getDeviceState(String data) {
        if (StringUtils.isEmpty(data))return;
        try {
            JSONObject jsonData = JSONObject.parseObject(data);
            String accountName = jsonData.getString("accountName");
            String dtuId = jsonData.getString("dtuId");
            String deviceId = jsonData.getString("deviceId");
            //回调函数
            String callbackurl = "http://47.93.21.73:18081/deviceStatus/dtuCallBack";
            //通过设备编号获取设备信息
            DeviceInfo deviceInfo = deviceInfoMapper.getDeviceInfoByValveId(deviceId);
            if(StringUtils.isEmpty(deviceInfo))return;
            //获取协议，category，接口url，
            String category = deviceInfo.getCategory();
            String deviceProtocol = deviceInfo.getDeviceProtocol();
            if(CommonsUtils.isEmpty(deviceProtocol)) return;
            if(deviceProtocol.contains("JLAA")){
                //无线表
                 getJLAADeviceState(deviceId);
                return;
            }
            String gateWayurl = deviceInfo.getGatewayUrl();
            gateWayurl = "http://"+gateWayurl+"/arm/api/reading";
            //构建查询json
            QueryParam queryParam = new QueryParam();
            queryParam.setIsAutoClear("1");
            queryParam.setAccountName(accountName);
            queryParam.setDtuId(dtuId);
            queryParam.setMeter(deviceId);
            queryParam.setCallbackurl(callbackurl);
            queryParam.setCategory(category);
            queryParam.setProtocol(deviceProtocol);
            JSONObject dtuObj = queryParam.queryJsonFormat();
            ///-----------
            String sendMessage = dtuObj.toJSONString();
            HttpClient.sendPost(gateWayurl, sendMessage);
            logger.log(Level.INFO,"sendMessage:"+sendMessage);

        }catch (Exception e){
            logger.log(Level.SEVERE,null,e);
        }
    }
    /**

     * {
     * "client_id":"123",
     * "access_token":"",
     * "device_id":"12345678"
     * }
     * @param
     */
    public String getJLAADeviceState(String deviceId){
        String url = "http://60.205.218.69:1841/joy/saas/v1/device/get";

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("client_id"," ");
        jsonObject.put("access_token"," ");
        jsonObject.put("device_id",deviceId);
        String s = HttpClient.sendPost(url, jsonObject.toJSONString());
        return s;

    }


    /**
     * 发送请求，如果请求失败返回错误代码，单表查询
     * @return
     */
    public String requestReadData(String data){
        //回调地址：callBackUrl
        try {
            JSONObject jsonData = JSONObject.parseObject(data);
            String accountName = jsonData.getString("accountName");
            String deviceId = jsonData.getString("deviceId");
            //回调函数
            String callbackurl = callBackUrl + "/deviceStatus/getDeviceDataCallBack";
            //通过设备编号获取设备信息
            DeviceInfo deviceInfo = deviceInfoMapper.getDeviceInfoById(deviceId);
            if(CommonsUtils.isEmpty(deviceInfo)){
                logger.log(Level.SEVERE,"查询设备信息为空:"+data);
                return "data erro";
            }
            //获取协议，category，接口url，
            String category = deviceInfo.getCategory();
            String deviceProtocol = deviceInfo.getDeviceProtocol();
            String dtuId = deviceInfo.getGatewayId();
            String dataUsed = deviceInfo.getDataUsed();
            if(CommonsUtils.isEmpty(deviceProtocol)) {
                logger.log(Level.SEVERE,"设备协议为空:"+data);
                return "dataUsed:"+dataUsed;
            }
            if(deviceProtocol.contains("JLAA")){
                //无线表，返回表id，给上层处理 {"valveId":valveId}
                JSONObject JLAAJson = JSONObject.parseObject(data);
                JLAAJson.put("deviceId",deviceId);
                JLAAJson.put("JLAA","JLAA");
                logger.log(Level.INFO,"无线表JLAA 查询接口调用:"+data);
                return JLAAJson.toString();
              }
            String gateWayurl = deviceInfo.getGatewayUrl();
            if(CommonsUtils.isEmpty(gateWayurl)) {
                logger.log(Level.SEVERE,"gatewayurl为空:"+data);
                return "dataUsed:"+dataUsed;
            }
            gateWayurl = "http://"+gateWayurl+"/arm/api/reading";
            //构建查询json
            QueryParam queryParam = new QueryParam();
            queryParam.setIsAutoClear("1");
            queryParam.setAccountName(accountName);
            queryParam.setDtuId(dtuId);
            queryParam.setMeter(deviceId);
            queryParam.setCallbackurl(callbackurl);
            queryParam.setCategory(category);
            queryParam.setProtocol(deviceProtocol);
            JSONObject dtuObj = queryParam.queryJsonFormat();
            ///-----------
            String sendMessage = dtuObj.toJSONString();
            HttpClient.sendPost(gateWayurl, sendMessage);
            logger.log(Level.INFO,"方法结束，发送请求信息：sendMessage:"+sendMessage);
            return "success";
        }catch (Exception e){
            logger.log(Level.SEVERE,"data:"+data,e);
            return "data erro";
        }
    }



    /**
     * 发送请求， 批量查询
     *
     * {
     *               "accountName":"操作人",
     *               "deviceId":"设备编号1,设备编号2,设备编号3",
     *               
     *       }
     *
     *
     * @return
     */
    public String requestReadDataBatch(String data){
        //回调地址：callBackUrl
        try {
            JSONObject jsonData = JSONObject.parseObject(data);
            String deviceIdAll = jsonData.getString("deviceId");
            String accountName = jsonData.getString("accountName");

            //添加嵌套元素
            JSONObject dtuObj = new JSONObject();
            JSONArray dtuValueArr = new JSONArray();
            JSONObject optionsObj = new JSONObject();
            String dtuId = null;
            String gateWayurl = null;
            String callbackurl = callBackUrl + "/deviceStatus/getDeviceDataBatchCallBack";

            //配置项参数，isAutoClear为10表示开关阀，1表示抄表
            optionsObj.put("isAutoClear","1");
            optionsObj.put("sendTime",System.currentTimeMillis());
            optionsObj.put("balanceWarnning","null");
            optionsObj.put("valveClose","null");
            optionsObj.put("concentrator_model","2");
            optionsObj.put("accountName",accountName);
            //表参数
            JSONArray metersArray = new JSONArray();
            //-------添加多表             getDeviceInfo();//获取表信息
            String[] devices = deviceIdAll.split(",");
            for (String deviceId:devices) {
                QueryParam deviceInfo = getDeviceInfo(deviceId);
                if(CommonsUtils.isEmpty(deviceInfo)){
                    logger.log(Level.SEVERE,"设备协议为空，deviceId为:"+deviceId);
                    continue;
                }
                if(!CommonsUtils.isEmpty(deviceInfo.getGatewayUrl())){
                    //gateWayurl = "http://"+deviceInfo.getGatewayUrl()+"/arm/api/reading";

                }
                 dtuId = deviceInfo.getDtuId();
                gateWayurl = "http://115.29.173.196:8888/arm/api/reading";
                JSONObject meters = new JSONObject();
                meters.put("meter",deviceId);
                meters.put("category",deviceInfo.getCategory());
                meters.put("protocol",deviceInfo.getProtocol());
                metersArray.add(meters);

            }

            //DTU数组对应的值，复杂形式json对象
            JSONObject info = new JSONObject();
            info.put("id",dtuId);
            info.put("url",callbackurl);
            info.put("options",optionsObj);
            info.put("meters",metersArray);
            dtuValueArr.add(info);
            dtuObj.put("DTU",dtuValueArr);
            String sendMessage = dtuObj.toJSONString();

            HttpClient.sendPost(gateWayurl, sendMessage);
            logger.log(Level.INFO,"sendMessage:"+sendMessage);
            return "success";
        }catch (Exception e){
            logger.log(Level.SEVERE,"data:"+data,e);
            return "data erro";
        }
    }

    /**
     * 根据设备编号查询，category，protocol，dtuId（只需要一个dtuid）
     * @param deviceId
     * @return
     */
    public QueryParam getDeviceInfo(String deviceId){
        QueryParam queryParam = new QueryParam();
        DeviceInfo deviceInfo = deviceInfoMapper.getDeviceInfoById(deviceId);
        if(CommonsUtils.isEmpty(deviceInfo) || CommonsUtils.isEmpty(deviceInfo.getCategory()) || CommonsUtils.isEmpty(deviceInfo.getDeviceProtocol())) return null;
        queryParam.setProtocol(deviceInfo.getDeviceProtocol());
        queryParam.setCategory(deviceInfo.getCategory());
        queryParam.setDtuId(deviceInfo.getGatewayId());
        queryParam.setGatewayUrl(deviceInfo.getGatewayUrl());
        queryParam.setDtuId(deviceInfo.getGatewayId());
         return queryParam;
    }

    /**
     * 获取最新读数，查询抄表接口
      {
              "accountName":"操作人",
              "deviceId":"设备编号"
      }
     返回格式：{
        "deviceId":"",
        "data":"",
        "dateTime":"",
        "result":"0" 成功 1 失败

     }
     使用同步锁解决异步问题
     * @param data
     */
    private static String GlobalData1 = null;
    private static String GlobalDeviceId = null;
    private static String GlobalDateTime = null;

    public String getDataFromMysql(String data){
        try{
            JSONObject jsonData = JSONObject.parseObject(data);
            String deviceId = jsonData.getString("deviceId");
            DeviceInfo deviceInfo = deviceInfoMapper.getDeviceInfoById(deviceId);
            String dataUsed = deviceInfo.getDataUsed();
            return dataUsed;
        }catch (Exception e){
            logger.log(Level.INFO,"查询mysql异常"+data+e);
            return null;
        }

     }
    @Override
    public String getDeviceData(String data) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("result","1");
        if(CommonsUtils.isEmpty(data) || CommonsUtils.isEmpty(getDataFromMysql(data))) return  jsonObject.toJSONString();
        jsonObject.put("result","0");
        synchronized (this){
            //发送请求
            String sendResult = requestReadData(data);
            logger.log(Level.INFO,"抄表sendResult:"+sendResult);

            if("success".equals(sendResult)){
                //发送成功，等待数据回调
                if(CommonsUtils.isEmpty(GlobalDeviceId)){
                    try {
                        this.wait();
                        //被唤醒后继续执行
                        logger.log(Level.INFO,"被唤醒后，获取到读数"+GlobalDeviceId+"***********"+GlobalData1+"****"+GlobalDateTime);
                        jsonObject.put("deviceId",GlobalDeviceId);
                        jsonObject.put("data",GlobalData1);
                        jsonObject.put("dateTime",GlobalDateTime);
                        jsonObject.put("result","0");
                    } catch (InterruptedException e) {
                        logger.log(Level.SEVERE,"线程异常"+data+e);
                        return jsonObject.toJSONString();

                    }finally {
                        GlobalData1 = null;
                        GlobalDeviceId = null;
                        GlobalDateTime = null;
                    }
                }
                return jsonObject.toJSONString();
            } else {
                if(!CommonsUtils.isEmpty(sendResult) && sendResult.contains("JLAA")) {
                    //无线表
                    JSONObject json = JSONObject.parseObject(sendResult);
                    String deviceId = json.getString("deviceId");
                    String s = getJLAADeviceState(deviceId);
                    logger.log(Level.INFO, "抄表，无线表" + data + "无线表接口返回数据为：" + s);
                }
                String used = getDataFromMysql(data);
                jsonObject.put("data",used);
                logger.log(Level.SEVERE,"发送查询请求失败"+data+"返回上次用量："+used);
                return jsonObject.toJSONString();
            }
        }


    }


    /***
     * 解析回调内容中的最新读数，data1
     * ：{"data9":{},"date":"2018-07-26 10:37:34","data8":"0","data7":"0","data6":"0","data5":"0","data4":"0","meter":"201703001320","data3":"0","data2":"0","data1":"3667.7","result":"0","payload":"FEFE684020130003172068810643C370673600A31600","options":{"isAutoClear":"1","balanceWarnning":"null","protocol":"com.joymeter.dtu.data.other.ParseElecData_001","concentrator_model":"2","accountName":"操作人","meter":"201703001320","dtuId":"944CD3EF","valveClose":"null","sendTime":1532572655151},"category":"40"}
     * @param data
     */
    @Override
    public void getDeviceDataCallBack(String data) {
        //解析回调函数，获取最新读数
        QueryParam queryParam = new QueryParam();
        //--解码
        try{
            logger.log(Level.INFO,"回调结果为："+CommonsUtils.toURLDecoded(data));
            queryParam.parseCallBaskJson(CommonsUtils.toURLDecoded(data));
        }catch (Exception e){
            logger.log(Level.SEVERE,"解析回调内容失败"+data,e);
        }
        String result = queryParam.getResult();
        if(CommonsUtils.isEmpty(result) || "1".equals(result)){
            logger.log(Level.SEVERE,"回调内容为空"+data);
            return;
        }
        //最新数据
        String data1 = queryParam.getData1();
        String deviceId = queryParam.getMeter();
        String dateTime = queryParam.getDate();
        logger.log(Level.INFO,"设备编号为:"+deviceId+"获取最新读数data为"+data1);
        //写入
        synchronized (this){
            GlobalData1 = data1;
            GlobalDeviceId = deviceId;
            GlobalDateTime = dateTime;
            //通知
            logger.log(Level.INFO,"拿到数据，准备唤醒方法1，"+GlobalDeviceId+"***********"+GlobalData1);
            this.notify();
        }


    }

    @Override
    public String getDeviceDataBatch(String data) {

        requestReadDataBatch(data);
        return null;
    }

    @Override
    public void getDeviceDataBatchCallBack(String data) {
        logger.log(Level.INFO,"批量接口回调结果为："+CommonsUtils.toURLDecoded(data));

    }


}
