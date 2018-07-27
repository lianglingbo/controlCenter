package com.joymeter.controlcenter.service;





public interface DeviceStatusService {

     void changeValveState(String data);
     Boolean dtuCallBack(String data);

    void getDeviceState(String data);

    String getDeviceData(String data);

    void getDeviceDataCallBack(String data);

    String getDeviceDataBatch(String data);

    void getDeviceDataBatchCallBack(String data);
}
