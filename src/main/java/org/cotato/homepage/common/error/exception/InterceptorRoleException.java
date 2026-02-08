package org.cotato.homepage.common.error.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class InterceptorRoleException extends RuntimeException {
	private String errorMessage;
}
