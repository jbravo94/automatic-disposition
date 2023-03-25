package org.openmrs.module.automaticdisposition.exception;

public class DispositionPreconditionNotMetException extends DispositionAbortedException {
	
	public DispositionPreconditionNotMetException(String cause) {
		super(cause);
	}
}
