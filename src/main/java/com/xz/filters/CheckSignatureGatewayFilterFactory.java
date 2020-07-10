package com.xz.filters;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.xz.exception.CustomException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;

@Slf4j
@Component
public class CheckSignatureGatewayFilterFactory extends AbstractGatewayFilterFactory<Object> {
    static final String DEFAULT_JOIN1 = "=";
    static final String DEFAULT_JOIN2 = "&";
    static final String EMPTY = "";
    static final Base64.Encoder ENCODER = java.util.Base64.getEncoder();

    @Override
    public GatewayFilter apply(Object config) {
        return (exchange, chain) -> {
            //请求时需要在头部放入验签的结果
            String signature = exchange.getAttribute("signature");
            //拿到请求的参数
            Map<String, Object> cachedRequestBodyObject = exchange.getAttribute("cachedRequestBodyObject");
            checkSignature(signature, cachedRequestBodyObject);
            return chain.filter(exchange);
        };
    }

    private void checkSignature(String signature, Map<String, Object> cachedRequestBodyObject) {
        //对cachedRequestBodyObject进行加密
        TreeMap<String, Object> treeMap = new TreeMap<>();
        treeMap.putAll(cachedRequestBodyObject);
        String rawData = transform(treeMap);
        String verifySign = DigestUtils.md5Hex(ENCODER.encode(rawData.getBytes()));
        log.info("signature:{},verifySign:{}", signature, verifySign);
        if (!verifySign.equalsIgnoreCase(signature)) {
            log.info("signature not match");
            throw new CustomException(HttpStatus.BAD_REQUEST, "signature not match");
        }
    }

    private static String transform(TreeMap<String, Object> treeMap) {
        StringBuilder stringBuilder = new StringBuilder();
        int index = 0;
        for (Map.Entry<String, ? extends Object> entry : treeMap.entrySet()) {
            if (entry.getValue() == null || entry.getValue().equals(EMPTY)) {
                continue;
            }

            if (index++ > 0) {
                stringBuilder.append(DEFAULT_JOIN2);
            }

            stringBuilder.append(entry.getKey()).append(DEFAULT_JOIN1);
            if (entry.getValue() instanceof String) {
                stringBuilder.append(entry.getValue());
            } else {
                stringBuilder.append(JSON.toJSONString(entry.getValue(), SerializerFeature.MapSortField));
            }
        }
        return stringBuilder.toString();
    }
}
