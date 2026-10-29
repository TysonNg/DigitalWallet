package com.tyson.digitalwallet.ddd.controller.exception;

import com.tyson.digitalwallet.ddd.application.media.MediaException;
import com.tyson.digitalwallet.ddd.controller.common.ApiResponse;
import com.tyson.digitalwallet.ddd.controller.resource.MediaController;
import com.tyson.digitalwallet.ddd.controller.resource.ReceiveQrController;
import com.tyson.digitalwallet.ddd.application.exception.AppException;
import java.time.Instant;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

@RestControllerAdvice(assignableTypes = {MediaController.class, ReceiveQrController.class})
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MediaExceptionHandler {
  @ExceptionHandler(MediaException.class)
  public ResponseEntity<ApiResponse<Void>> media(MediaException ex) {
    return error(ex.status(), ex.code(), ex.getMessage());
  }

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  public ResponseEntity<ApiResponse<Void>> tooLarge(MaxUploadSizeExceededException ex) {
    return error(413, "MEDIA_TOO_LARGE", "Maximum file size is 5 MiB");
  }

  @ExceptionHandler({
    MissingServletRequestPartException.class,
    MethodArgumentTypeMismatchException.class
  })
  public ResponseEntity<ApiResponse<Void>> badRequest(Exception ex) {
    return error(400, "MEDIA_INVALID_REQUEST", "Invalid media request");
  }

  @ExceptionHandler({
    DataAccessException.class,
    org.springframework.transaction.TransactionException.class
  })
  public ResponseEntity<ApiResponse<Void>> database(RuntimeException ex) {
    return error(503, "MEDIA_UNAVAILABLE", "Media storage is unavailable");
  }

  @ExceptionHandler(AppException.class)
  public ResponseEntity<ApiResponse<Void>> policy(AppException ex) {
    return new GlobalExceptionHandler().handleAppException(ex);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Void>> unexpected(Exception ex) {
    return error(500, "INTERNAL_SERVER_ERROR", "An unexpected internal server error occurred");
  }

  private ResponseEntity<ApiResponse<Void>> error(int status, String code, String message) {
    return ResponseEntity.status(status)
        .body(new ApiResponse<>(false, status, message, code, null, Instant.now()));
  }
}
