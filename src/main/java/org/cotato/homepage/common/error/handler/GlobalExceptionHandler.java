package org.cotato.homepage.common.error.handler;

import java.sql.SQLException;
import java.util.List;

import org.cotato.homepage.common.error.ErrorCode;
import org.cotato.homepage.common.error.exception.AppException;
import org.cotato.homepage.common.error.exception.ImageException;
import org.cotato.homepage.common.error.response.ErrorResponse;
import org.cotato.homepage.common.error.response.MethodArgumentErrorResponse;
import org.cotato.homepage.common.error.response.MethodArgumentErrorResponse.FieldErrorResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.amazonaws.services.s3.model.AmazonS3Exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	@ExceptionHandler(AppException.class)
	public ResponseEntity<ErrorResponse> handleAppCustomException(AppException exception, HttpServletRequest request) {
		log.error("AppException 발생: {}", exception.getErrorCode().getMessage());
		log.error("에러가 발생한 지점 {}, {}", request.getMethod(), request.getRequestURI());
		ErrorResponse errorResponse = ErrorResponse.of(exception.getErrorCode(), request);
		return ResponseEntity.status(exception.getErrorCode().getHttpStatus())
			.body(errorResponse);
	}

	@ExceptionHandler(ImageException.class)
	public ResponseEntity<ErrorResponse> handleImageException(ImageException exception, HttpServletRequest request) {
		log.error("이미지 처리 실패 예외 발생: {}", exception.getErrorCode().getMessage());
		log.error("에러가 발생한 지점 {}, {}", request.getMethod(), request.getRequestURI());
		ErrorResponse errorResponse = ErrorResponse.of(exception.getErrorCode(), request);
		return ResponseEntity.status(exception.getErrorCode().getHttpStatus())
			.body(errorResponse);
	}

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
		HttpHeaders headers, HttpStatusCode status,
		WebRequest request) {
		ServletWebRequest servletWebRequest = (ServletWebRequest)request;
		HttpServletRequest httpServletRequest = servletWebRequest.getRequest();
		String requestUri = httpServletRequest.getRequestURI();

		List<FieldErrorResponse> fieldErrorResponses = ex.getBindingResult().getFieldErrors().stream()
			.map(FieldErrorResponse::of)
			.toList();

		List<String> errorFields = fieldErrorResponses.stream()
			.map(FieldErrorResponse::getField)
			.toList();

		log.error("[Method Argument Not Valid Execption 발생]: {}", errorFields);
		log.error("에러가 발생한 지점 {}, {}", httpServletRequest.getMethod(), requestUri);

		MethodArgumentErrorResponse errorResponse = MethodArgumentErrorResponse.of(
			ErrorCode.INVALID_INPUT, httpServletRequest, fieldErrorResponses);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
	}

	@ExceptionHandler(SQLException.class)
	public ResponseEntity<ErrorResponse> handleSqlException(SQLException exception, HttpServletRequest request) {
		log.error("발생한 에러: {}", exception.getErrorCode());
		log.error("에러가 발생한 지점 {}, {}", request.getMethod(), request.getRequestURI());
		ErrorResponse errorResponse = ErrorResponse.of(ErrorCode.INTERNAL_SQL_ERROR, request);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
	}

	@ExceptionHandler(AmazonS3Exception.class)
	public ResponseEntity<ErrorResponse> handleAmazonS3Exception(AmazonS3Exception exception,
		HttpServletRequest request) {
		log.error("발생한 에러: {}", exception.getErrorCode());
		log.error("에러가 발생한 지점 {}, {}", request.getMethod(), request.getRequestURI());
		ErrorResponse errorResponse = ErrorResponse.of(ErrorCode.IMAGE_PROCESSING_FAIL, request);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleException(Exception exception, HttpServletRequest request) {
		log.error("Unhandled Exception 발생: {}", exception.getMessage());
		log.error("에러가 발생한 지점 {}, {}", request.getMethod(), request.getRequestURI());
		ErrorResponse errorResponse = ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR, request);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
	}
}
