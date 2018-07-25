package com.joymeter.controlcenter.service;





public interface DeviceStatusService {

     void changeValveState(String data);
     Boolean dtuCallBack(String data);

    void getDeviceState(String data);
}
