package org.genepattern.server.webservice;
import java.util.Vector;

import org.genepattern.analysis.WebServiceException;

public class WebServiceErrorMessageException extends WebServiceException {
	Vector errors;
	
	public WebServiceErrorMessageException(Vector errors) {
		this.errors = errors;
	}
	public Vector getErrors() {
		return errors;	
	}
	
	public String getMessage() {
		StringBuffer buf = new StringBuffer();
		buf.append("Errors:");
		for(int i = 0, size = errors.size(); i < size; i++) {
			if(i > 0) {
				buf.append(" ");
			}
			buf.append(String.valueOf(i+1));
			buf.append(".");
			buf.append(errors.get(i));
		}
		return buf.toString();
	}
}