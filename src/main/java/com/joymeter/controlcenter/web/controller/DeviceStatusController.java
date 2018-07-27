package com.joymeter.controlcenter.web.controller;

import com.joymeter.controlcenter.service.DeviceStatusService;
import com.joymeter.controlcenter.service.DeviceValveService;
import com.joymeter.controlcenter.utils.CommonsUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * @ClassName DeviceStatusController
 * @Description TODO
 * 设备状态修改
 * 业务层调用
 * @Author liang
 * @Date 2018/6/11 17:41
 * @Version 1.0
 **/
@CrossOrigin("*")
@Controller
@RequestMapping("deviceStatus")
public class DeviceStatusController {
    @Autowired
    private DeviceStatusService deviceStatusService;
    @Autowired
    private DeviceValveService deviceValveService;
    private static final Logger logger = Logger.getLogger(DeviceStatusController.class.getName());


    /**
     * {DTU: [{id:'dtu Id', url:'url', options:{isAutoClear: '0', accountName:
     *      * '张三'}, meters:[{meter:'表号', category:'10', protocol:'188'}
     * @param data
     */
    //更改阀门状态 ， 阀门状态(0 关, 1 开)
    @RequestMapping("/changeValve")
    @ResponseBody
    public String changeValveState(@RequestBody String data){
        return deviceValveService.changeValveState(data);
    }


    /**
     * {DTU: [{id:'dtu Id', url:'url', options:{isAutoClear: '0', accountName:
     *      * '张三'}, meters:[{meter:'表号', category:'10', protocol:'188'}
     * @param data
     */
    //获取设备状态 ，
    @RequestMapping("/getDeviceState")
    @ResponseBody
    public void getDeviceState(@RequestBody String data){
        deviceStatusService.getDeviceState(data);
    }

    //给dtu提供回调接口，返回成功消息
    //result: 0 成功 1 失败
    @RequestMapping("/dtuCallBack")
    @ResponseBody
    public void dtuCallBack(@RequestParam("parameters")String data){
        deviceValveService.dtuCallBack(data);
    }


    /**
     * 对外提供同步查询接口
     * 返回设备最新读数，阀门状态
     */
    @RequestMapping("/getDeviceData")
    @ResponseBody
    public String getDeviceData(@RequestBody String data){
        return deviceStatusService.getDeviceData(data);
    }

    /**
     * 对外提供同步查询接口,批量
     * 返回设备最新读数，阀门状态
     */
    @RequestMapping("/getDeviceDataBatch")
    @ResponseBody
    public String getDeviceDataBatch(@RequestBody String data){
        return deviceStatusService.getDeviceDataBatch(data);
    }




    /**
     * 对外提供同步查询接口 的回调接口
     * 返回设备最新读数，阀门状态
     */
    @RequestMapping("/getDeviceDataCallBack")
    @ResponseBody
    public void getDeviceDataCallBack(@RequestParam("parameters") String data){
        deviceStatusService.getDeviceDataCallBack(data);
    }

    /**
     * 对外提供同步查询接口 的回调接口,批量
     * 返回设备最新读数，阀门状态
     */
    @RequestMapping("/getDeviceDataBatchCallBack")
    @ResponseBody
    public void getDeviceDataBatchCallBack(@RequestParam("parameters") String data){
        deviceStatusService.getDeviceDataBatchCallBack(data);
    }

    public static String dataInfo ;
    @RequestMapping("/test01")
    @ResponseBody
    public String test01(){
        System.out.println(Thread.currentThread().getName()+"test01 start  .. ");
        try {
            System.out.println("等待 。。。 ");
            Thread.sleep(15000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        synchronized (this){
            System.out.println(Thread.currentThread().getName()+"test01  into  locked ..  and wait ");
            try {
                this.wait();
                System.out.println(Thread.currentThread().getName()+dataInfo);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println(Thread.currentThread().getName()+"test01 end ");
            return dataInfo;
        }
     }

    @RequestMapping("/test02")
    @ResponseBody
    public void test02(@RequestBody String data){
        System.out.println(Thread.currentThread().getName()+"test02 start .. ");
        try {
            System.out.println("等待 。。。 ");
            Thread.sleep(15000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        synchronized (this){
            dataInfo = data;
            System.out.println(Thread.currentThread().getName()+"test02 into locked .. notify test01 data: "+data);
            this.notify();
            System.out.println(Thread.currentThread().getName()+"test02 end");

        }
    }

}
