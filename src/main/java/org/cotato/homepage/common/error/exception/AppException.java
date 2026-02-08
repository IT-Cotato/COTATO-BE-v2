package org.cotato.homepage.common.error.exception;

import org.cotato.homepage.common.error.ErrorCode;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class AppException extends RuntimeException {

	private ErrorCode errorCode;
}
