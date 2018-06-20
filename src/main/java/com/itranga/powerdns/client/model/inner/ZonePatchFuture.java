package com.itranga.powerdns.client.model.inner;

import com.itranga.powerdns.client.model.ZonePatch;

public class ZonePatchFuture extends ZonePatch{
	
	private long futureResultId;
	private String zoneId;
	
	public long getFutureResultId(){
		return futureResultId;
	}
	
	public void setFutureResultId(long futureId){
		this.futureResultId = futureId;
	}

	public String getZoneId() {
		return zoneId;
	}

	public void setZoneId(String zoneId) {
		this.zoneId = zoneId;
	}
	
}
