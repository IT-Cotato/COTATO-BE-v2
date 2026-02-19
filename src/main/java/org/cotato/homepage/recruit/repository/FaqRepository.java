package org.cotato.homepage.recruit.repository;

import java.util.List;

import org.cotato.homepage.recruit.entity.Faq;
import org.cotato.homepage.recruit.enums.FaqType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FaqRepository extends JpaRepository<Faq, Long> {

	List<Faq> findAllByFaqTypeOrderBySequenceAsc(FaqType faqType);
}
