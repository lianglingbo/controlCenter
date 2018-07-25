package com.joymeter.controlcenter.domain;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

/**
 * @ClassName QueryParam
 * @Description TODO
 * 查询网关各种参数
 * @Author liang
 * @Date 2018/7/25 14:30
 * @Version 1.0
 **/
public class QueryParam {
    private String isAutoClear;
    private String protocol;
    private String dtuId;
    private String accountName;
    private String meter;
    private String category;
    private String action;
    private String valveId;
    private String callbackurl;

    public String getIsAutoClear() {
        return isAutoClear;
    }

    public void setIsAutoClear(String isAutoClear) {
        this.isAutoClear = isAutoClear;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getDtuId() {
        return dtuId;
    }

    public void setDtuId(String dtuId) {
        this.dtuId = dtuId;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public String getMeter() {
        return meter;
    }

    public void setMeter(String meter) {
        this.meter = meter;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getValveId() {
        return valveId;
    }

    public void setValveId(String valveId) {
        this.valveId = valveId;
    }

    public String getCallbackurl() {
        return callbackurl;
    }

    public void setCallbackurl(String callbackurl) {
        this.callbackurl = callbackurl;
    }


    /**
     * 返回一个封装好了的json对象
     * json格式为：
     *{
     *      "DTU":[
     *          {
     *              "options":{
     *                  "isAutoClear":"1",
     *                  "balanceWarnning":"null",
     *                  "protocol":"com.joymeter.dtu.data.other.ParseElecData_001",
     *                  "concentrator_model":"2",
     *                  "accountName":"joy000001",
     *                  "dtuId":"",
     *                  "valveClose":"null",
     *                  "sendTime":"2018-07-17 11:15:23"
     *              },
     *              "id":"944CD3EF",
     *              "url":"http://47.93.21.73:18081/deviceStatus/dtuCallBack",
     *              "meters":[
     *                  {
     *                      "protocol":"com.joymeter.dtu.data.other.ParseElecData_001",
     *                      "meter":"201703001320",
     *                      "category":"40"
     *                  }
     *              ]
     *          }
     *      ]
     *  }
     * @return
     */
    public  JSONObject queryJsonFormat(){
        //添加嵌套元素
        JSONObject DTUObj = new JSONObject();
        JSONArray DTUValueArr = new JSONArray();
        JSONObject optionsObj = new JSONObject();
        //配置项参数，isAutoClear为10表示开关阀，1表示抄表
        optionsObj.put("isAutoClear",isAutoClear);
        optionsObj.put("protocol", protocol);
        optionsObj.put("dtuId",dtuId);
        optionsObj.put("sendTime",System.currentTimeMillis());
        optionsObj.put("balanceWarnning","null");
        optionsObj.put("valveClose","null");
        optionsObj.put("concentrator_model","2");
        //回调时观察
        optionsObj.put("meter",meter);
        optionsObj.put("accountName",accountName);
         optionsObj.put("action",action);
         //表参数
        JSONArray metersArray = new JSONArray();
        JSONObject meters1 = new JSONObject();
        meters1.put("meter",meter);
        meters1.put("category",category);
        meters1.put("protocol",protocol);
        metersArray.add(meters1);
        //DTU数组对应的值，复杂形式json对象
        JSONObject info = new JSONObject();
        info.put("id",dtuId);
        info.put("url",callbackurl);
        info.put("options",optionsObj);
        info.put("meters",metersArray);
        DTUValueArr.add(info);
        DTUObj.put("DTU",DTUValueArr);
        return  DTUObj;
    }





    @Override
    public String toString() {
        return "QueryParam{" +
                "isAutoClear='" + isAutoClear + '\'' +
                ", protocol='" + protocol + '\'' +
                ", dtuId='" + dtuId + '\'' +
                ", accountName='" + accountName + '\'' +
                ", meter='" + meter + '\'' +
                ", category='" + category + '\'' +
                ", action='" + action + '\'' +
                ", valveId='" + valveId + '\'' +
                ", callbackurl='" + callbackurl + '\'' +
                '}';
    }
}
