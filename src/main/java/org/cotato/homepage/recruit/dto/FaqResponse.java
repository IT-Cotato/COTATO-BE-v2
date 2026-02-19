package org.cotato.homepage.recruit.dto;

import org.cotato.homepage.recruit.entity.Faq;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({"id", "question", "answer"})
public record FaqResponse(Long id, String question, String answer) {
	public static FaqResponse from(Faq faq) {
		return new FaqResponse(faq.getId(), faq.getQuestion(), faq.getAnswer());
	}
}
