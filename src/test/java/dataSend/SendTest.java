package dataSend;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.joymeter.controlcenter.service.impl.DeviceStatusServiceImpl;
import com.joymeter.controlcenter.utils.httpclient.HttpClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;



/**
 * @ClassName send
 * @Description TODO
 * @Author liang
 * @Date 2018/7/16 16:44
 * @Version 1.0
 **/
@RunWith(SpringRunner.class)
@SpringBootTest(classes = SendTest.class)
public class SendTest {

    @Test
    public void send(){
        String url = "http://127.0.0.1:18081/deviceStatus/changeValve";
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("data", "201707103885");

        HttpClient.sendPost(url, jsonObject.toString());


    }

    /**
     *
     * 关阀协议：protocol:'com.joymeter.dtu.data.other.valve.ParseElecValveData_001_Close'
     * 读表协议：com.joymeter.dtu.data.other.ParseElecData_001
     *  {
     *     "DTU":[
     *         {
     *             "options":{
     *                 "isAutoClear":"1",
     *                 "balanceWarnning":"null",
     *                 "protocol":"com.joymeter.dtu.data.other.ParseElecData_001",
     *                 "concentrator_model":"2",
     *                 "accountName":"joy000001",
     *                 "dtuId":"",
     *                 "valveClose":"null",
     *                 "sendTime":"2018-07-17 11:15:23"
     *             },
     *             "id":"944CD3EF",
     *             "url":"http://47.93.21.73:18081/deviceStatus/dtuCallBack",
     *             "meters":[
     *                 {
     *                     "protocol":"com.joymeter.dtu.data.other.ParseElecData_001",
     *                     "meter":"201703001320",
     *                     "category":"40"
     *                 }
     *             ]
     *         }
     *     ]
     * }
     *
     * 测试表
     *
     *      id: dtu id<br>
     *      options: 可选参数,回调时原样返回 <br>
     *      isAutoClear: 是否自动结算 (0 -- 自动， 1 -- 手动)<br>
     *      accountName: 操作员帐户<br>
     *      url: 回调地址<br>
     *      category: 表计类型(10 -- 水表， 20 -- 热表, 30 -- 燃气, 40 -- 电表)<br>
     *      protocol：带包名的类名,根据不同厂商实现的命令解析类<br>
     *
     *
     *
     */
    @Test
    public void sendToGateway(){
        //String url = "http://115.29.173.196:8888/arm/api/reading";

        //String url = "http://127.0.0.1:18081/deviceStatus/changeValve";
        String url = "http://47.93.21.73:18081/deviceStatus/changeValve";

        String callbackurl = "http://47.93.21.73:18081/deviceStatus/dtuCallBack";
        //添加嵌套元素
        JSONObject dtuObj = new JSONObject();
        JSONArray dtuValueArr = new JSONArray();
        JSONObject optionsObj = new JSONObject();
        //配置项参数
        optionsObj.put("isAutoClear","1");
        optionsObj.put("protocol","com.joymeter.dtu.data.other.valve.ParseElecValveData_001_Close");
        optionsObj.put("dtuId","");
        optionsObj.put("accountName","joy000001");
        optionsObj.put("sendTime","2018-07-17 11:15:23");
        optionsObj.put("balanceWarnning","null");
        optionsObj.put("valveClose","null");
        optionsObj.put("concentrator_model","2");
        optionsObj.put("action","关阀门");
        optionsObj.put("meter","表号");

        //表参数
        JSONArray metersArray = new JSONArray();
        JSONObject meters1 = new JSONObject();
        meters1.put("meter","201703001320");
        meters1.put("category","40");
        meters1.put("protocol","com.joymeter.dtu.data.other.ParseElecData_001");
        metersArray.add(meters1);
        //DTU数组对应的值，复杂形式json对象
        JSONObject info = new JSONObject();
        info.put("id","944CD3EF");
        info.put("url",callbackurl);
        info.put("options",optionsObj);
        info.put("meters",metersArray);
        dtuValueArr.add(info);
        dtuObj.put("DTU",dtuValueArr);
        //-------------------
        System.out.println(dtuObj.toString());
        String result = HttpClient.sendPost(url, dtuObj.toJSONString());
        System.out.println(result);
    }

    /**
     * 解析结果
     *
     * parameters={"data9":{},"date":"2018-07-17 14:50:00","data8":"0","data7":"0","data6":"0","data5":"0","data4":"0","meter":"201803290075","data3":"0","data2":"0","data1":"3.84","result":"0","options":{"isAutoClear":"1","balanceWarnning":"null","protocol":"com.joymeter.dtu.data.other.ParseElecData_001","concentrator_model":"2","accountName":"joy000001","dtuId":"","valveClose":"null","sendTime":"2018-07-17 11:15:23"},"category":"40"}
     *
     */
    @Test
     public void parseJson(){
        String json = "{\"data9\":{},\"date\":\"2018-07-17 14:50:00\",\"data8\":\"0\",\"data7\":\"0\",\"data6\":\"0\",\"data5\":\"0\",\"data4\":\"0\",\"meter\":\"201803290075\",\"data3\":\"0\",\"data2\":\"0\",\"data1\":\"3.84\",\"result\":\"1\",\"options\":{\"isAutoClear\":\"1\",\"balanceWarnning\":\"null\",\"protocol\":\"com.joymeter.dtu.data.other.ParseElecData_001\",\"concentrator_model\":\"2\",\"accountName\":\"joy000001\",\"dtuId\":\"\",\"valveClose\":\"null\",\"sendTime\":\"2018-07-17 11:15:23\"},\"category\":\"40\"}\n";
        //转json
        JSONObject callBackObject = JSONObject.parseObject(json);
        if(callBackObject.containsKey("result")){
            String result =  callBackObject.getString("result");
            if("0".equals(result)){
                System.out.println("11111");
            }
        }

    }

    /* 修改阀门状态,给业务层提供接口
     * 数据格式为：
     *  {
     *     "accountName":"操作人",
     *     "id":"dtuId",
     *     "valveId":"阀门编号",
     *     "action":"Close/Open"
     * }
     */
    @Test
    public void send2(){
        String json = " { \"accountName\":\"操作人\",\"id\":\"944CD3EF\", \"valveId\":\"201703001320\",\"action\":\"Open\"}";
        String url = "http://47.93.21.73:18081/deviceStatus/changeValve";
        HttpClient.sendPost(url, json);


    }

    @Test
    public void parse2(){
        String json = "{\"data9\":{},\"date\":\"2018-07-17 17:50:42\",\"data8\":\"0\",\"data7\":\"0\",\"data6\":\"0\",\"data5\":\"0\",\"data4\":\"0\",\"meter\":\"201703001320\",\"data3\":\"0\",\"data2\":\"0\",\"data1\":\"3306.71\",\"result\":\"0\",\"options\":{\"isAutoClear\":\"1\",\"balanceWarnning\":\"null\",\"protocol\":\"com.joymeter.dtu.data.other.valve.ParseElecValveData_001_Open\",\"concentrator_model\":\"2\",\"accountName\":\"操作人\",\"valveId\":\"201703001320\",\"dtuId\":\"944CD3EF\",\"action\":\"Open\",\"valveClose\":\"null\",\"sendTime\":1531821042627},\"category\":\"40\"}\n";
        DeviceStatusServiceImpl dev = new DeviceStatusServiceImpl();
        dev.dtuCallBack(json);

    }
}
