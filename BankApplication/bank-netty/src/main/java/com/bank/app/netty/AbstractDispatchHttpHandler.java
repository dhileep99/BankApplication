package com.bank.app.netty;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.QueryStringDecoder;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractDispatchHttpHandler implements HttpHandler {
	
    @Override
    public final FullHttpResponse handle(FullHttpRequest request) throws Exception {
        QueryStringDecoder query = new QueryStringDecoder(request.getUri());
        String requestUri = query.path();
        HttpHandler handler = getHandler(requestUri);
        if (handler == null) {
            return handleMissingHandler(requestUri);
        }
        return handler.handle(request);
    }

    protected FullHttpResponse handleMissingHandler(String requestUri) {
        return HttpUtils.createTextPlainHttpResponseUtf8(HttpResponseStatus.NOT_FOUND, requestUri + " not found");
    }

    @Nullable
    protected abstract HttpHandler getHandler(String requestUri);
}
