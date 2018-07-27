package com.joymeter.controlcenter.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.joymeter.controlcenter.dao.mapper.DeviceInfoMapper;
import com.joymeter.controlcenter.domain.DeviceInfo;
import com.joymeter.controlcenter.domain.QueryParam;
import com.joymeter.controlcenter.service.DeviceValveService;
import com.joymeter.controlcenter.utils.CommonsUtils;
import com.joymeter.controlcenter.utils.PropertiesUtils;
import com.joymeter.controlcenter.utils.httpclient.HttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @ClassName DeviceValveServiceImpl
 * @Description TODO
 * @Author liang
 * @Date 2018/7/27 14:22
 * @Version 1.0
 **/
@Service
public class DeviceValveServiceImpl implements DeviceValveService {

    private static final Logger logger = Logger.getLogger(DeviceValveServiceImpl.class.getName());
    private static String callBackUrl = PropertiesUtils.getProperty("callBackUrl", "");

    @Autowired
    private DeviceInfoMapper deviceInfoMapper;

    /**
     * 发送开关阀请求动作,无线表的解析还需要完善
     * @param data
     * @return
     */
    public String requestValveData(String data){
        if (StringUtils.isEmpty(data))return null;
        try {
            JSONObject jsonData = JSONObject.parseObject(data);
            String accountName = jsonData.getString("accountName");
            String id = jsonData.getString("id");
            String valveId = jsonData.getString("valveId");
            String action = jsonData.getString("action");
            //通过阀门号获取设备信息
            DeviceInfo deviceInfo = deviceInfoMapper.getDeviceInfoByValveId(valveId);
            if(StringUtils.isEmpty(deviceInfo))return null;
            //获取协议，category，接口url，
            String category = deviceInfo.getCategory();
            String ValveProtocol = deviceInfo.getValveProtocol()+"_"+action;
            if(CommonsUtils.isEmpty(ValveProtocol)) return null;
            if(ValveProtocol.contains("JLAA")){
                //无线表，返回表id，给上层处理 {"valveId":valveId}
                JSONObject JLAAJson = JSONObject.parseObject(data);
                JLAAJson.put("valveId",valveId);
                JLAAJson.put("action",action);
                JLAAJson.put("JLAA","JLAA");
                return JLAAJson.toString();
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
            queryParam.setAction(action);
            JSONObject dtuObj = queryParam.queryJsonFormat();
            ///-----------
            String sendMessage = dtuObj.toJSONString();

            HttpClient.sendPost(gateWayurl, sendMessage);
            logger.log(Level.INFO,"sendMessage:"+sendMessage);
            return "ok";
        }catch (Exception e){
            logger.log(Level.SEVERE,"开关阀控制异常",data+e);
            return null;
        }

    }
    public String changeJLAAValveState(String valveId,String action){
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
        return s;

    }
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
     * 返回值：
     * {
     *     "result":"1",  //1:控制失败，异常； 0：控制成功
     *     "valveState":"Close",    /Open
     *     "valveId":"阀门编号"
     * }
     *
     */
    private static String GlobalValveState = null;
    private static String GlobalValveId = null;
    private static String GlobalResult = null;

    @Override
    public String changeValveState(String data ) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("result","1");
         synchronized (this){
            String sendResult = requestValveData(data);
            logger.log(Level.INFO,"阀门控制sendResult:"+sendResult);
            if("ok".equals(sendResult)){
                //数据发送成功，等待回调
                if(CommonsUtils.isEmpty(GlobalValveId)){
                    try {
                        this.wait();
                        //被唤醒后继续执行
                        logger.log(Level.INFO,"被唤醒后，获取到读数"+GlobalValveId+"***********"+GlobalValveState+"result"+GlobalResult);
                        jsonObject.put("valveId",GlobalValveId);
                        jsonObject.put("valveState",GlobalValveState);
                        jsonObject.put("result",GlobalResult);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        logger.log(Level.INFO,"阀门控制线程异常:"+e);
                    }finally {
                        GlobalValveState = null;
                        GlobalValveId = null;
                        GlobalResult = null;
                     }
                }
                return  jsonObject.toJSONString();
            }else{
                if(!CommonsUtils.isEmpty(sendResult) && sendResult.contains("JLAA")){
                    //无线表的处理
                    JSONObject json = JSONObject.parseObject(sendResult);
                    String valveId = json.getString("valveId");
                    String action = json.getString("action");
                    String s = changeJLAAValveState(valveId, action);
                    logger.log(Level.INFO,"阀门控制，无线表"+data+"无线表接口返回数据为："+s);
                }
                 logger.log(Level.SEVERE,"阀门控制失败"+data);
                 return jsonObject.toJSONString();
             }
        }


    }


    /**
     *回调函数
     * @param data
     */
    @Override
    public synchronized void  dtuCallBack(String data) {
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
                    String valveId = options.getString("meter");
                    String action = options.getString("action");
                    GlobalResult = "0";
                    GlobalValveState = action;
                    GlobalValveId = valveId;
                    try {
                         //更新mysql数据库,valveState阀门状态(0 关, 1 开)
                        String valveState = "";
                        if ("Close".equals(action)) {
                            valveState = "0";
                        } else if ("Open".equals(action)){
                            valveState = "1";
                        }
                        logger.log(Level.INFO,"阀门控制，数据库写入：valveId："+valveId+"    valveState  :"+valveState);
                        deviceInfoMapper.updateValeStatus(valveId, valveState);
                    }catch (Exception e){
                        logger.log(Level.SEVERE,"阀门控制，数据库写入失败",e);
                    }
                }else {
                    GlobalResult = "1";
                    logger.log(Level.SEVERE,"阀门控制解析结果为1",decoded);
                }
            }
        }catch (Exception e){
            logger.log(Level.SEVERE,"阀门控制回调解析异常",e);
        }finally {
            this.notify();
            logger.log(Level.SEVERE,"阀门控制解析完成 唤醒方法1");
        }
    }


}
