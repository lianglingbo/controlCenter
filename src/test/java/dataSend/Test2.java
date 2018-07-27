package dataSend;

import com.alibaba.fastjson.JSONObject;
import org.junit.Test;

/**
 * @ClassName Test2
 * @Description TODO
 * @Author liang
 * @Date 2018/7/26 14:12
 * @Version 1.0
 **/
public class Test2 {
    @Test
    public void test(){
        String json = "{   \"accountName\":\"操作人\",   \"deviceId\":\"设备编号1,设备编号2,设备编号3\"}";
        JSONObject jsonData = JSONObject.parseObject(json);
        String deviceIdAll = jsonData.getString("deviceId");
        String[] deviceIds = deviceIdAll.split(",");
        for (String deviceId:deviceIds) {
            System.out.println(deviceId+"   ...");
        }


    }
}
