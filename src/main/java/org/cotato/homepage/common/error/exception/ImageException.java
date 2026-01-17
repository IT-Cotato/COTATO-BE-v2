package org.cotato.homepage.common.error.exception;

import java.io.IOException;

import org.cotato.homepage.common.error.ErrorCode;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class ImageException extends IOException {

	private ErrorCode errorCode;
}
