package com.amplify.ap.domain;

import java.beans.PropertyEditorSupport;

public class ResourceTypeConverter extends PropertyEditorSupport{

	 public void setAsText(final String text) throws IllegalArgumentException {
	        setValue(ResourceType.fromValue(text));
	    }

}