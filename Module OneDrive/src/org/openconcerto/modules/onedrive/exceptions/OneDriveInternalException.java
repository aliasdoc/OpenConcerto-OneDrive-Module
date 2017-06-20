package org.openconcerto.modules.onedrive.exceptions;

public class OneDriveInternalException extends RuntimeException implements OneDriveException {
	public OneDriveInternalException(String message, Throwable cause) {
		super(message, cause);
	}

	public OneDriveInternalException(String message) {
		super(message);
	}
}