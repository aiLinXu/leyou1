package com.leyou.common.advices;

import com.leyou.common.exceptions.LyException;
import com.leyou.common.vo.ExceptionResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * @author 虎哥
 */
@ControllerAdvice
public class BaseExceptionAdvice {

    @ExceptionHandler(LyException.class)
    public ResponseEntity<ExceptionResult> handleException(LyException e){
        e.printStackTrace();
        return ResponseEntity.status(e.getStatus()).body(new ExceptionResult(e));
    }
}
