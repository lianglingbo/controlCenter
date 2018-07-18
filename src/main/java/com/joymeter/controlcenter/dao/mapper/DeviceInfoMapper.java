package com.joymeter.controlcenter.dao.mapper;


import com.joymeter.controlcenter.domain.DeviceInfo;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;



@Repository
public interface DeviceInfoMapper {
	//通过deviceId查
	@Select("SELECT * FROM device_info where deviceId = #{deviceId}")
    DeviceInfo getDeviceInfoById(String deviceId);

	//阀门编号
	@Select("SELECT * FROM device_info where valveId = #{valveId}")
	DeviceInfo getDeviceInfoByValveId(String valveId);

	//更新阀门状态
	@Update(" update   device_info  set valveState = #{valveState}  where valveId = #{valveId} ")
	void updateValeStatus(String valveId , String valveState);

}
