package com.gd.grdrpc.bean;

import lombok.Data;

@Data
public class RpcResponse {

    private String requestId;
    private Throwable error;
    private Object result;

    public boolean isError() {
        if (null != error.getCause() ) {
            return false;
        }else {
            return true;
        }
    }
}
