package com.leyou.common.exceptions;

import com.leyou.common.enums.ExceptionEnum;
import lombok.Getter;

/**
 * @author 虎哥
 */
@Getter
public class LyException extends RuntimeException {
    private int status;

    public LyException(int status, String message) {
        super(message);
        this.status = status;
    }

    public LyException(int status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public LyException(ExceptionEnum em) {
        super(em.getMessage());
        this.status = em.getStatus();
    }

    public LyException(ExceptionEnum em, Throwable cause) {
        super(em.getMessage(), cause);
        this.status = em.getStatus();
    }
}
