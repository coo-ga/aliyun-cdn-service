package com.cooperzhu.aliyuncdnservice.response;

import lombok.Data;
import org.slf4j.MDC;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

@Data
public class BizResponse<T> implements Serializable {
    @Serial
    private static final long serialVersionUID = 466609991138097833L;

    //region 属性
    private static final int CODE_SUCCESS = 200;
    private static final int CODE_ERROR = 400;
    /** traceId。全局唯一 */
    private String traceId;
    /** 状态码。200-成功；非200-失败； */
    private int code;
    /** 提示信息 */
    private String message;
    /** 扩展提示信息 */
    private String extendMessage;
    /** 数据 */
    private T data;
    //endregion

    //region 构造方法
    private BizResponse(int code) {
        this.traceId = UUID.randomUUID().toString();
        this.code = code;
    }

    private BizResponse(int code, String message, String extendMessage, T data) {
        this(code);
        this.message = message;
        this.extendMessage = extendMessage;
        this.data = data;
    }
    //endregion

    //region methods

    /**
     * 构造一个成功且不带数据的实例
     *
     * @param <T>
     * @return
     */
    public static <T> BizResponse<T> newSuccessInstance() {
        return new BizResponse<>(CODE_SUCCESS, "成功", null, null);
    }

    /**
     * 构造一个成功且带数据的实例
     *
     * @param data
     * @param <T>
     * @return
     */
    public static <T> BizResponse<T> newSuccessInstance(T data) {
        return new BizResponse<>(CODE_SUCCESS, "成功", null, data);
    }

    /**
     * 构造一个失败的实例
     *
     * @param message
     * @return
     */
    public static <T> BizResponse<T> newErrorInstance(String message) {
        return new BizResponse<>(CODE_ERROR, message, null, null);
    }

    public static <T> BizResponse<T> newErrorInstance(String message, T data) {
        return new BizResponse<>(CODE_ERROR, message, null, data);
    }

    public static <T> BizResponse<T> newErrorInstance(String message, String extendMessage) {
        return new BizResponse<>(CODE_ERROR, message, extendMessage, null);
    }
    //endregion
}
