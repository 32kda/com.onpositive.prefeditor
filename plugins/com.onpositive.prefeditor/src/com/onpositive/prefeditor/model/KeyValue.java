package com.onpositive.prefeditor.model;

public class KeyValue {
	
	private String parentNode;
	private String key;
	private String value;
	
	public KeyValue(String parentNode, String key, String value) {
		super();
		this.parentNode = parentNode;
		this.key = key;
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getKey() {
		return key;
	}

	public String getParentNode() {
		return parentNode;
	}
	
	@Override
	public String toString() {
		return key + " = " + value;
	}

}
