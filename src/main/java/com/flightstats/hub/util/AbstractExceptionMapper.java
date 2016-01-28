package com.flightstats.hub.util;

import com.flightstats.hub.metrics.ActiveTraces;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

public abstract class AbstractExceptionMapper<T extends Throwable> implements ExceptionMapper<T> {
    private static final Logger logger = LoggerFactory.getLogger(AbstractExceptionMapper.class);

    public AbstractExceptionMapper() {
    }

    public Response toResponse(T exception) {
        logger.info("{} {}", exception.getMessage(), exception.getClass());
        logger.trace("exception", exception);
        Response.ResponseBuilder builder = Response.status(this.getResponseCode());
        builder.entity(exception.getMessage());
        ActiveTraces.end();
        return builder.build();
    }

    protected abstract Response.Status getResponseCode();
}