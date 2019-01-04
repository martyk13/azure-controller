package com.amplify.apc.domain;

import java.util.Arrays;

public enum StorageAccountType {

    STANDARD_LRS("Standard_LRS"),
    STANDARD_GRS("Standard_GRS"),
    STANDARD_ZRS("Standard_ZRS"),
    PREMIUM_LRS("Premium_LRS");

	private String value;

	private StorageAccountType(String value) {
		this.value = value;
	}

	public static StorageAccountType fromValue(String value) {
		for (StorageAccountType type : values()) {
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