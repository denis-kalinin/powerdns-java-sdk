package com.itranga.powerdns.client.model;

import java.util.Collection;

public class ZonePatch {

	private Collection<RRset> rrsets;

	public Collection<RRset> getRrsets() {
		return rrsets;
	}

	public void setRrsets(Collection<RRset> rrsets) {
		this.rrsets = rrsets;
	}
}
