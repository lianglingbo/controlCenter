package com.joymeter.controlcenter.domain;

import com.alibaba.fastjson.JSONObject;

import java.sql.Timestamp;

public class DeviceInfo {
	private String deviceId;
	private String simId;
	private String gatewayId;
	private String gatewayUrl;
	private String valveId;
	private String category;
	private String deviceProtocol;
	private String valveProtocol;
	private String project;
	private String province;
	private String city;
	private String district;
	private String community;
	private String address;
	private String readState;
	private String valveState;
	private String deviceState;
	private String simState;
	private String dataUsed;
	private Timestamp updateTime;

	public Timestamp getUpdateTime() {
		return updateTime;
	}

	public void setUpdateTime(Timestamp updateTime) {
		this.updateTime = updateTime;
	}

	public DeviceInfo() {

	}

	public DeviceInfo(JSONObject jsonObject) {
		super();
		this.deviceId = jsonObject.getString("deviceId");
		this.simId = jsonObject.getString("simId");
		this.gatewayId = jsonObject.getString("gatewayId");
		this.project = jsonObject.getString("project");
		this.province = jsonObject.getString("province");
		this.city = jsonObject.getString("city");
		this.district = jsonObject.getString("district");
		this.community = jsonObject.getString("community");
		this.address = jsonObject.getString("address");
		this.readState = jsonObject.getString("readState");
		this.valveState = jsonObject.getString("valveState");
		this.deviceState = jsonObject.getString("deviceState");
		this.simState = jsonObject.getString("simState");
		this.dataUsed = jsonObject.getString("dataUsed");
		this.category = jsonObject.getString("category");
		this.valveId = jsonObject.getString("valveId");
		this.valveProtocol = jsonObject.getString("valveProtocol");
		this.deviceProtocol = jsonObject.getString("deviceProtocol");
	}

	public String getDeviceId() {
		return deviceId;
	}

	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
	}

	public String getReadState() {
		return readState;
	}

	public void setReadState(String readState) {
		this.readState = readState;
	}

	public String getSimId() {
		return simId;
	}

	public void setSimId(String simId) {
		this.simId = simId;
	}

	public String getValveState() {
		return valveState;
	}

	public void setValveState(String valveState) {
		this.valveState = valveState;
	}

	public String getDeviceState() {
		return deviceState;
	}

	public void setDeviceState(String deviceState) {
		this.deviceState = deviceState;
	}

	public String getSimState() {
		return simState;
	}

	public void setSimState(String simState) {
		this.simState = simState;
	}

	public String getDataUsed() {
		return dataUsed;
	}

	public void setDataUsed(String dataUsed) {
		this.dataUsed = dataUsed;
	}

	public String getGatewayId() {
		return gatewayId;
	}

	public void setGatewayId(String gatewayId) {
		this.gatewayId = gatewayId;
	}

	public String getProject() {
		return project;
	}

	public void setProject(String project) {
		this.project = project;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public String getProvince() {
		return province;
	}

	public void setProvince(String province) {
		this.province = province;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getDistrict() {
		return district;
	}

	public void setDistrict(String district) {
		this.district = district;
	}

	public String getCommunity() {
		return community;
	}

	public void setCommunity(String community) {
		this.community = community;
	}
	
	public String getValveId() {
		return valveId;
	}

	public void setValveId(String valveId) {
		this.valveId = valveId;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public String getDeviceProtocol() {
		return deviceProtocol;
	}

	public void setDeviceProtocol(String deviceProtocol) {
		this.deviceProtocol = deviceProtocol;
	}

	public String getValveProtocol() {
		return valveProtocol;
	}

	public void setValveProtocol(String valveProtocol) {
		this.valveProtocol = valveProtocol;
	}
	

	public String getGatewayUrl() {
		return gatewayUrl;
	}

	public void setGatewayUrl(String gatewayUrl) {
		this.gatewayUrl = gatewayUrl;
	}

	@Override
	public String toString() {
		return "DeviceInfo [deviceId=" + deviceId + ", simId=" + simId + ", gatewayId=" + gatewayId + ", project="
				+ project + ", province=" + province + ", city=" + city + ", district=" + district + ", community="
				+ community + ", address=" + address + ", readState=" + readState + ", valveState=" + valveState
				+ ", deviceState=" + deviceState + ", simState=" + simState + ", dataUsed=" + dataUsed + "]";
	}
	
}
