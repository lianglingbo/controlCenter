package com.joymeter.controlcenter.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.joymeter.controlcenter.dao.mapper.DeviceInfoMapper;
import com.joymeter.controlcenter.domain.DeviceInfo;
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
             String gateWayurl = deviceInfo.getGatewayUrl();
             gateWayurl = "http://"+gateWayurl+"/arm/api/reading";
            //回调函数
             String callbackurl = "http://47.93.21.73:18081/deviceStatus/dtuCallBack";
             /*===============================*/
             //添加嵌套元素
             JSONObject dtuObj = new JSONObject();
             JSONArray dtuValueArr = new JSONArray();
             JSONObject optionsObj = new JSONObject();
             //配置项参数
             optionsObj.put("isAutoClear","1");
             optionsObj.put("protocol", ValveProtocol);
             optionsObj.put("dtuId",id);
             optionsObj.put("accountName", accountName);
             optionsObj.put("sendTime",System.currentTimeMillis());
             optionsObj.put("balanceWarnning","null");
             optionsObj.put("valveClose","null");
             optionsObj.put("concentrator_model","2");
             //回调时判断那只表做的操作
             optionsObj.put("action",action);
             optionsObj.put("valveId",valveId);
             //表参数
             JSONArray metersArray = new JSONArray();
             JSONObject meters1 = new JSONObject();
             meters1.put("meter",valveId);
             meters1.put("category",category);
             meters1.put("protocol",ValveProtocol);
             metersArray.add(meters1);
             //DTU数组对应的值，复杂形式json对象
             JSONObject info = new JSONObject();
             info.put("id",id);
             info.put("url",callbackurl);
             info.put("options",optionsObj);
             info.put("meters",metersArray);
             dtuValueArr.add(info);
             dtuObj.put("DTU",dtuValueArr);
             /*===============================*/
             String sendMessage = dtuObj.toJSONString();
             System.out.println(sendMessage);
             HttpClient.sendPost(gateWayurl, sendMessage);


         }catch (Exception e){
             logger.log(Level.INFO,null,e);
         }

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
                    String value = action;

                    try {
                        //更新redis数据库
                        redisUtils.setKV(key,value);
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
}
