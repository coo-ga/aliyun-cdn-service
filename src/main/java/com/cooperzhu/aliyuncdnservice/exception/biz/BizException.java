package com.cooperzhu.aliyuncdnservice.exception.biz;

import lombok.Data;

import java.io.Serial;

@Data
public class BizException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 6577165386030770043L;
    private Throwable cause;
    public BizException(String message) {
        super(message);
    }

    public BizException(String message, Throwable e) {
        super(message);
        this.cause = e;
    }
}

