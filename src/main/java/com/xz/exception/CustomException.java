package com.xz.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;


/**
 * {
 *     "timestamp": "2020-07-10T06:50:12.924+0000",
 *     "path": "/user/getById",
 *     "status": 400,
 *     "error": "Bad Request",
 *     "message": "header-signature must not be null"
 * }
 *
 * 当抛出异常时,默认的DefaultErrorWebExceptionHandler --> renderErrorResponse  ->> getErrorAttributes(request, includeStackTrace)
 *
 * -->this.errorAttributes.getErrorAttributes(request, includeStackTrace); -->DefaultErrorAttributes getErrorAttributes
 *
 * -->determineHttpStatus(error)-->if (error instanceof ResponseStatusException) {(ResponseStatusException) error 将error强转为ResponseStatusException}
 *
 * -->	errorAttributes.put("status", errorStatus.value());     errorAttributes.put("error", errorStatus.getReasonPhrase());
 *
 * -->errorAttributes.put("message", determineMessage(error)); 设置message时也是判断类型进行强转，然后return ((ResponseStatusException) error).getReason();
 *
 * 所以这里自定义CustomException继承ResponseStatusException  并且含有一个字段reason以及一个HttpStatus就可以将自定义的异常信息输出到默认的发生异常时的返回值中
 */

public class CustomException extends ResponseStatusException {
    public CustomException(HttpStatus status) {
        super(status);
    }

    public CustomException(HttpStatus status, String reason) {
        super(status);
        this.code = status;
        this.reason = reason;
    }

    /**
     * http错误状态码
     */
    private HttpStatus code;

    /**
     * 错误消息
     */
    private String reason;


    public HttpStatus getCode() {
        return code;
    }

    public void setCode(HttpStatus code) {
        this.code = code;
    }

    public String getMsg() {
        return reason;
    }

    @Override
    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
