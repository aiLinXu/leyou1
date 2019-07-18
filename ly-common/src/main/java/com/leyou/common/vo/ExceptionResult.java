package com.leyou.common.vo;

import com.leyou.common.exceptions.LyException;
import lombok.Getter;

import java.util.Date;

/**
 * @author 虎哥
 */
@Getter
public class ExceptionResult {
    private int status;
    private String message;
    private String timestamp;

    public ExceptionResult(LyException e) {
        this.status = e.getStatus();
        this.message = e.getMessage();
        this.timestamp = new Date().toLocaleString();
    }
}
