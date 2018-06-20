package com.itranga.powerdns.client.model.inner;

import java.util.Collection;

import com.itranga.powerdns.client.model.RRset;

public class HostnameBatchResult {
	private long hostnameResultFutureid;
	private long batchId;
	private boolean succedded;
	private Collection<String> ns;
	private String errorMessage;
	private Collection<RRset> rrset;
	
	public long getHostnameResultFutureid() {
		return hostnameResultFutureid;
	}
	public void setHostnameResultFutureid(long hostnameResultFutureid) {
		this.hostnameResultFutureid = hostnameResultFutureid;
	}
	public long getBatchId() {
		return batchId;
	}
	public void setBatchId(long batchId) {
		this.batchId = batchId;
	}
	public boolean isSuccedded() {
		return succedded;
	}
	public void setSuccedded(boolean succedded) {
		this.succedded = succedded;
	}
	public Collection<String> getNs() {
		return ns;
	}
	public void setNs(Collection<String> ns) {
		this.ns = ns;
	}
	public String getErrorMessage() {
		return errorMessage;
	}
	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}
	public Collection<RRset> getRrset() {
		return rrset;
	}
	public void setRrset(Collection<RRset> rrset) {
		this.rrset = rrset;
	}
	
	
	
}
