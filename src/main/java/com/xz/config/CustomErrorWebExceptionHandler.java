package com.xz.config;

import com.xz.exception.CustomException;
import org.springframework.boot.autoconfigure.web.ErrorProperties;
import org.springframework.boot.autoconfigure.web.ResourceProperties;
import org.springframework.boot.autoconfigure.web.reactive.error.DefaultErrorWebExceptionHandler;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.server.*;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class CustomErrorWebExceptionHandler extends DefaultErrorWebExceptionHandler {

    public CustomErrorWebExceptionHandler(ErrorAttributes errorAttributes,
                                          ResourceProperties resourceProperties,
                                          ErrorProperties errorProperties,
                                          ApplicationContext applicationContext) {
        super(errorAttributes, resourceProperties, errorProperties, applicationContext);
    }

    /**
     * 自定义errorAttributes
     */
    @Override
    protected Map<String, Object> getErrorAttributes(ServerRequest request, boolean includeStackTrace) {
        // 这里其实可以根据异常类型进行定制化逻辑
        Throwable error = super.getError(request);
        Map<String, Object> errorAttributes = new HashMap<>(8);
        HttpStatus code = HttpStatus.INTERNAL_SERVER_ERROR;
        if (error instanceof CustomException) {
            CustomException customException = (CustomException) error;
            errorAttributes.put("timestamp", new Date());
            errorAttributes.put("path", request.path());
            code = customException.getCode();
            errorAttributes.put("message", customException.getReason());
        }
        errorAttributes.put("status", code.value());
        errorAttributes.put("error", code.getReasonPhrase());
        //errorAttributes.put("name", "zjw");
        return errorAttributes;
    }

    @Override
    protected RouterFunction<ServerResponse> getRoutingFunction(ErrorAttributes errorAttributes) {
        return RouterFunctions.route(RequestPredicates.all(), this::renderErrorResponse);
    }

    /**
     * 这里返回的HttpStatus在后面AbstractErrorWebExceptionHandler handle   logError(request, response, throwable)
     * <p>
     * 会判断如果是500 则打印e,否则不打印,这个地方的statusCode是捕捉到异常时返回给客户端的http状态码
     * <p>
     * 而出现异常时返回给客户端的body里面的status是
     * <p>
     * 在上面的getErrorAttributes构建的和这个HttpStatus没有关系
     */
    @Override
    protected HttpStatus getHttpStatus(Map<String, Object> errorAttributes) {
        // 这里其实可以根据errorAttributes里面的属性定制HTTP响应码
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }
}
