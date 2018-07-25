package com.joymeter.controlcenter.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.joymeter.controlcenter.dao.mapper.DeviceInfoMapper;
import com.joymeter.controlcenter.domain.DeviceInfo;
import com.joymeter.controlcenter.domain.QueryParam;
import com.joymeter.controlcenter.service.DeviceStatusService;
import com.joymeter.controlcenter.utils.CommonsUtils;
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
             logger.log(Level.INFO,null,e);
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
                        logger.log(Level.INFO,null,e);
                    }
                    return true;
                }
            }
            return null;
        }catch (Exception e){
            logger.log(Level.INFO,null,e);
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
            System.out.println(sendMessage);
            HttpClient.sendPost(gateWayurl, sendMessage);
            logger.log(Level.INFO,"sendMessage:"+sendMessage);

        }catch (Exception e){
            logger.log(Level.INFO,null,e);
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
    public void getJLAADeviceState(String deviceId){
        String url = "http://60.205.218.69:1841/joy/saas/v1/device/get";

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("client_id"," ");
        jsonObject.put("access_token"," ");
        jsonObject.put("device_id",deviceId);
        String s = HttpClient.sendPost(url, jsonObject.toJSONString());


    }
}
