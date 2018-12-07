package com.bank.app.netty;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;

public interface HttpHandler {
    FullHttpResponse handle(FullHttpRequest request) throws Exception;
}
