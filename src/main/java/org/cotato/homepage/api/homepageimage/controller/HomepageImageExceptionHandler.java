package org.cotato.homepage.api.homepageimage.controller;

import org.cotato.homepage.domain.homepageimage.model.HomepageImageException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** 사진 API에 한정한 오류 응답이다. 기존 인증·출석·점수 예외 처리에는 관여하지 않는다. */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackageClasses = HomepageImageController.class)
public class HomepageImageExceptionHandler {
	@ExceptionHandler(HomepageImageException.class)
	public ResponseEntity<ErrorResponse> expected(HomepageImageException exception) {
		return ResponseEntity.status(exception.status()).body(new ErrorResponse(exception.getMessage()));
	}

	@ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
	public ResponseEntity<ErrorResponse> invalid(Exception exception) {
		return ResponseEntity.badRequest().body(new ErrorResponse("입력한 사진 설정을 확인해 주세요."));
	}

	/** 클라이언트에서 표시할 설명만 포함한다. 요청 본문과 내부 원문 예외는 노출하지 않는다. */
	public record ErrorResponse(String message) {
	}
}
