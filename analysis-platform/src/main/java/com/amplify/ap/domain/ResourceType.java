package com.amplify.ap.domain;

import java.util.Arrays;

public enum ResourceType {

	COMPUTE("COMPUTE"), STORAGE("STORAGE");

	private String value;

	private ResourceType(String value) {
		this.value = value;
	}

	public static ResourceType fromValue(String value) {
		for (ResourceType type : values()) {
			if (type.value.equalsIgnoreCase(value)) {
				return type;
			}
		}
		throw new IllegalArgumentException(
				"Unknown enum type " + value + ", Allowed values are " + Arrays.toString(values()));
	}
	
	public String getValue() {
		return this.value;
	}
}