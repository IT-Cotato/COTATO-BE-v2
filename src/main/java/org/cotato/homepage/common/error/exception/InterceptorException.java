package org.cotato.homepage.common.error.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class InterceptorException extends RuntimeException {

	private String errorMessage;
}
