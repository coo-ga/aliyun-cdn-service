package com.cooperzhu.aliyuncdnservice.exception;

/**
 * Exception thrown when the operation is not implemented or not supported/enabled in this service.
 */
public class UnimplementedException extends RuntimeException {

    public UnimplementedException() {
        super();
    }

    public UnimplementedException(String message) {
        super(message);
    }

    public UnimplementedException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnimplementedException(Throwable cause) {
        super(cause);
    }
}
