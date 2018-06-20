package com.itranga.powerdns.client.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

public class RRset {
	
	private List<String> comments;
	private String name;
	private List<Record> records;
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private Integer ttl = 300;
	private String type;
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private ChangeType changetype;
	
	
	public List<String> getComments() {
		return comments;
	}


	public void setComments(List<String> comments) {
		this.comments = comments;
	}


	public String getName() {
		return name;
	}


	public void setName(String name) {
		this.name = name;
	}


	public List<Record> getRecords() {
		return records;
	}


	public void setRecords(List<Record> records) {
		this.records = records;
	}


	public Integer getTtl() {
		return ttl;
	}


	public void setTtl(int ttl) {
		this.ttl = ttl;
	}


	public String getType() {
		return type;
	}


	public void setType(String type) {
		this.type = type;
	}
	
	public ChangeType getChangetype() {
		return changetype;
	}


	public void setChangetype(ChangeType changetype) {
		this.changetype = changetype;
	}

	public static enum ChangeType{
		DELETE,
		REPLACE
	}


	public static class Record{
		private String content;
		private boolean disabled;
		public Record(){}
		public Record(String content){
			this.content = content;
		}
		public Record(String content, boolean disabled){
			this.content = content;
			this.disabled = disabled;
		}
		public String getContent() {
			return content;
		}
		public void setContent(String content) {
			this.content = content;
		}
		public boolean isDisabled() {
			return disabled;
		}
		public void setDisabled(boolean disabled) {
			this.disabled = disabled;
		}
		
	}

}
