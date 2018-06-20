package com.itranga.powerdns.client.vertx;

import java.util.Collection;

import com.itranga.powerdns.client.model.RRset;

public class BatchRRset{
	private Long futureId;
	private Collection<RRset> rrset;
	public Long getFutureId() {
		return futureId;
	}
	public void setFutureId(Long futureId) {
		this.futureId = futureId;
	}
	public Collection<RRset> getRrset() {
		return rrset;
	}
	public void setRrset(Collection<RRset> rrset) {
		this.rrset = rrset;
	}
	
}
