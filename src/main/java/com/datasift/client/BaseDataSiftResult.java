package com.datasift.client;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Optional;

/**
 * @author Courtney Robinson <courtney.robinson@datasift.com>
 */
public class BaseDataSiftResult implements DataSiftResult {
    @JsonProperty
    protected String error;
    //
    protected Response response;
    private Throwable cause;
    private boolean failed;

    @Override
    public boolean isSuccessful() {
        return !failed
                && response != null
                && error == null
                && response.status() > 199
                && response.status() < 400;
    }

    @Override
    public Response getResponse() {
        return response;
    }

    @Override
    public void setResponse(Response response) {
        this.response = response;
        //setResponse can happen after a result has been marked as failed
        //so if it is already failed then keep it as a failure
        failed = failed ? failed : response.hasFailed();
        //likewise if already failed keep the existing cause
        cause = cause != null ? cause : response.failureCause();
    }

    @Override
    public Throwable failureCause() {
        return cause;
    }

    @Override
    public boolean isAuthorizationSuccesful() {
        return isSuccessful() && response.status() != 401;
    }

    /**
     * {@link #rateLimit() rateLimit}, {@link #rateLimitRemaining() rateLimitRemaining}, and
     * {@link #rateLimitCost() rateLimitCost} may throw {@link IllegalStateException}, but not if
     * {@link #isSuccessful()} returns true.
     *
     * @return the value of the header X-RateLimit-Limit
     */
    @Override
    public Optional<Integer> rateLimit() {
        return intHeader("X-RateLimit-Limit");
    }
    /** @return the value of the header X-RateLimit-Remaining */
    @Override
    public Optional<Integer> rateLimitRemaining() {
        return intHeader("X-RateLimit-Remaining");
    }
    /** @return the value of the header X-RateLimit-Cost */
    @Override
    public Optional<Integer> rateLimitCost() {
        return intHeader("X-RateLimit-Cost");
    }
    private Optional<Integer> intHeader(String headerName) {
        if (response == null)
            throw new IllegalStateException("response not yet filled");
        List<String> limit = response.headers().get(headerName);
        if (limit == null || limit.isEmpty())
            return Optional.empty();
        Integer ret = new Integer(limit.get(0));
        return Optional.of(ret);
    }

    @Override
    public String getError() {
        return error;
    }

    @Override
    public String toString() {
        if (response == null) return "unfulfilled response";
        return response.toString();
    }

    @Override
    public void failed(Throwable e) {
        failed = e != null;
        cause = e;
    }

    @Override
    public void successful() {
        failed = false;
    }
}
