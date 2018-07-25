package com.joymeter.controlcenter.web.controller;

import com.joymeter.controlcenter.service.DeviceStatusService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

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
    private static final Logger logger = Logger.getLogger(DeviceStatusController.class.getName());


    /**
     * {DTU: [{id:'dtu Id', url:'url', options:{isAutoClear: '0', accountName:
     *      * '张三'}, meters:[{meter:'表号', category:'10', protocol:'188'}
     * @param data
     */
    //更改阀门状态 ， 阀门状态(0 关, 1 开)
    @RequestMapping("/changeValve")
    @ResponseBody
    public void changeValveState(@RequestBody String data){
        deviceStatusService.changeValveState(data);
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
        deviceStatusService.dtuCallBack(data);
    }






}
