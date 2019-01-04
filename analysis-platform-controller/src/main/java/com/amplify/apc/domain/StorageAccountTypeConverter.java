package com.amplify.apc.domain;

import java.beans.PropertyEditorSupport;

public class StorageAccountTypeConverter extends PropertyEditorSupport{

	 public void setAsText(final String text) throws IllegalArgumentException {
	        setValue(StorageAccountType.fromValue(text));
	    }

}