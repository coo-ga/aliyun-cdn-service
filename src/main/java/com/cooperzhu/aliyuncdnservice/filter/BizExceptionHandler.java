package com.cooperzhu.aliyuncdnservice.filter;

import com.cooperzhu.aliyuncdnservice.exception.biz.BizException;
import com.cooperzhu.aliyuncdnservice.response.BizResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

@Slf4j
@ControllerAdvice
public class BizExceptionHandler {
    @ResponseBody
    @ExceptionHandler(BizException.class)
    public BizResponse<Object> handle(HttpServletResponse httpServletResponse, BizException ex) {

        log.error("==============11111111111111111==============");
        if (ex.getMessage().equalsIgnoreCase("无效token")) {
            httpServletResponse.setStatus(HttpStatus.UNAUTHORIZED.value());
        } else {
            httpServletResponse.setStatus(HttpStatus.OK.value());
        }
        log.error(ex.getMessage());
        return BizResponse.newErrorInstance(ex.getMessage(), null == ex.getCause() ? null : ex.getCause().getMessage());
    }

    @ResponseBody
    @ExceptionHandler(Exception.class)
    public BizResponse handle(HttpServletResponse httpServletResponse, Exception ex) {
        log.error("==============222222222222222222==============");
        BizException bex = includeBizException(ex, 0);
        if (null == bex) {
            httpServletResponse.setStatus(HttpStatus.OK.value());
            log.error(ex.getMessage(), ex);
            return BizResponse.newErrorInstance("系统错误，请重试", ex.getMessage());
        }

        if (bex.getMessage().equalsIgnoreCase("无效token")) {
            httpServletResponse.setStatus(HttpStatus.UNAUTHORIZED.value());
        } else {
            httpServletResponse.setStatus(HttpStatus.OK.value());
        }
        log.error(bex.getMessage());
        return BizResponse.newErrorInstance(bex.getMessage());
    }

    /**
     * 有些场景下，BizException会被其他组件/代码重新封装，所以这里通过查找n层异常来判断是否包含BizException
     *
     * @param ex
     * @param level
     * @return
     */
    private BizException includeBizException(Throwable ex, int level) {
        if (null == ex || level >= 5) {
            return null;
        }

        if (ex instanceof BizException) {
            return (BizException)ex;
        }

        return includeBizException(ex.getCause(), ++level);
    }
}

