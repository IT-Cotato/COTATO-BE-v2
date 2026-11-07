package org.cotato.homepage.domain.minuspoint.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.cotato.homepage.api.minuspoint.dto.CancelManualBonusPointRequest;
import org.cotato.homepage.api.minuspoint.dto.GrantManualBonusPointRequest;
import org.cotato.homepage.common.error.ErrorCode;
import org.cotato.homepage.common.error.exception.AppException;
import org.cotato.homepage.domain.generation.entity.Generation;
import org.cotato.homepage.domain.generation.repository.GenerationMemberRepository;
import org.cotato.homepage.domain.generation.service.component.GenerationReader;
import org.cotato.homepage.domain.member.entity.Member;
import org.cotato.homepage.domain.member.enums.MemberRole;
import org.cotato.homepage.domain.member.service.component.MemberReader;
import org.cotato.homepage.domain.minuspoint.entity.ManualBonusPoint;
import org.cotato.homepage.domain.minuspoint.repository.ManualBonusPointRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** 명시적으로 지정한 로컬 전용 MySQL에서 실제 DDL·트랜잭션·동시 요청만 검증한다. */
@DataJpaTest(properties = {"spring.jpa.hibernate.ddl-auto=none", "spring.jpa.show-sql=false"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = ManualBonusPointIntegrationTest.DatabaseConfig.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@EnabledIfEnvironmentVariable(named = "MANUAL_BONUS_JDBC_URL",
	matches = "jdbc:mysql://127\\.0\\.0\\.1:[0-9]+/manual_bonus_test.*")
class ManualBonusPointIntegrationTest {

	/** 운영 애플리케이션·Redis·메일을 띄우지 않고 점수 JPA 저장소만 구성한다. */
	@Configuration
	@EntityScan(basePackageClasses = ManualBonusPoint.class)
	@EnableJpaRepositories(basePackageClasses = ManualBonusPointRepository.class)
	@EnableJpaAuditing
	@Import(ManualBonusPointService.class)
	static class DatabaseConfig {
	}

	@DynamicPropertySource
	static void database(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", () -> System.getenv("MANUAL_BONUS_JDBC_URL"));
		registry.add("spring.datasource.username", () -> "bonus_test");
		registry.add("spring.datasource.password", () -> System.getenv("MANUAL_BONUS_TEST_PASSWORD"));
		registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
	}

	@Autowired
	private ManualBonusPointService service;
	@Autowired
	private ManualBonusPointRepository repository;
	@Autowired
	private JdbcTemplate jdbc;
	@MockBean
	private GenerationReader generationReader;
	@MockBean
	private MemberReader memberReader;
	@MockBean
	private GenerationMemberRepository membershipRepository;

	private final Member actor = BonusPointFixtures.member(3L, MemberRole.OPERATION);

	@BeforeEach
	void prepareSyntheticTable() throws Exception {
		// 매 실행마다 새 로컬 시험 테이블에 실제 배포용 DDL을 그대로 적용한다.
		assertEquals("manual_bonus_test", jdbc.queryForObject("SELECT DATABASE()", String.class));
		jdbc.execute("DROP TABLE IF EXISTS manual_bonus_point");
		try (var connection = jdbc.getDataSource().getConnection()) {
			ScriptUtils.executeSqlScript(connection,
				new ClassPathResource("db/manual/20261105_add_manual_bonus_point.sql"));
		}
		when(generationReader.findById(14L)).thenReturn(Generation.builder().id(14L).build());
		when(memberReader.findById(7L)).thenReturn(BonusPointFixtures.member(7L, MemberRole.MEMBER));
		when(membershipRepository.existsByGenerationAndMember(any(), any())).thenReturn(true);
	}

	@Test
	void repeatedRequestAndPayloadConflictKeepOneOriginalRow() {
		UUID requestId = UUID.randomUUID();
		LocalDateTime beforeGrant = LocalDateTime.now().minusSeconds(1);
		var first = service.grant(actor, request(1, requestId));
		assertFalse(first.createdAt().isBefore(beforeGrant));
		assertFalse(first.createdAt().isAfter(LocalDateTime.now().plusSeconds(1)));
		var retry = service.grant(actor, request(1, requestId));
		assertEquals(first.id(), retry.id());
		assertEquals(1, repository.count());
		assertEquals(ErrorCode.BONUS_REQUEST_CONFLICT,
			assertThrows(AppException.class, () -> service.grant(actor, request(2, requestId))).getErrorCode());
		assertEquals(1, repository.findById(first.id()).orElseThrow().getPoints());
	}

	@Test
	void grantAndCancellationPreserveAuditAndActiveSum() {
		service.grant(actor, request(1, UUID.randomUUID()));
		var second = service.grant(actor, request(2, UUID.randomUUID()));
		assertEquals(3, activeSum());
		service.cancel(actor, second.id(), new CancelManualBonusPointRequest("오입력"));
		service.cancel(BonusPointFixtures.member(4L, MemberRole.PR), second.id(),
			new CancelManualBonusPointRequest("다른 사유"));
		assertEquals(1, activeSum());
		assertEquals(2, repository.count());
		ManualBonusPoint stored = repository.findById(second.id()).orElseThrow();
		assertEquals(2, stored.getPoints());
		assertEquals("행사 도움", stored.getReason());
		assertEquals(3L, stored.getCancelledBy());
		assertEquals("오입력", stored.getCancellationReason());
	}

	@Test
	void sixteenConcurrentRetriesCreateExactlyOneGrant() throws Exception {
		UUID requestId = UUID.randomUUID();
		List<Long> ids = concurrently(16, () -> service.grant(actor, request(2, requestId)).id());
		assertEquals(1, ids.stream().distinct().count());
		assertEquals(1, repository.count());
		assertEquals(2, activeSum());
	}

	@Test
	void independentConcurrentGrantsDoNotLoseUpdates() throws Exception {
		concurrently(12, () -> service.grant(actor, request(2, UUID.randomUUID())).id());
		assertEquals(12, repository.count());
		assertEquals(24, activeSum());
	}

	@ParameterizedTest
	@ValueSource(ints = {3, 5, 10})
	void databasePreservesDirectlyEnteredPoints(int points) {
		var response = service.grant(actor, request(points, UUID.randomUUID()));
		assertEquals(points, response.points());
		assertEquals(points, activeSum());
		assertEquals(1, repository.count());
	}

	@ParameterizedTest
	@ValueSource(ints = {-1, 0, 11})
	void databaseRejectsUnsupportedPointEvenWhenServiceIsBypassed(int points) {
		var exception = assertThrows(org.springframework.dao.DataAccessException.class, () ->
			jdbc.update("INSERT INTO manual_bonus_point "
				+ "(generation_id,member_id,points,reason,request_id,created_by,created_by_name,"
				+ "created_at,modified_at) "
				+ "VALUES (14,7,?,'fixture',?,3,'fixture',NOW(6),NOW(6))", points, UUID.randomUUID().toString()));
		assertInstanceOf(java.sql.SQLException.class, exception.getMostSpecificCause());
		assertEquals(3819, ((java.sql.SQLException)exception.getMostSpecificCause()).getErrorCode());
		assertEquals(0, repository.count());
	}

	private int activeSum() {
		return repository.findAllByGenerationIdAndCancelledAtIsNull(14L).stream()
			.mapToInt(ManualBonusPoint::getPoints).sum();
	}

	private GrantManualBonusPointRequest request(int points, UUID requestId) {
		return new GrantManualBonusPointRequest(14L, 7L, points, "행사 도움", requestId);
	}

	private List<Long> concurrently(int count, Callable<Long> action) throws Exception {
		ExecutorService pool = Executors.newFixedThreadPool(count);
		CountDownLatch start = new CountDownLatch(1);
		try {
			List<java.util.concurrent.Future<Long>> futures = new ArrayList<>();
			for (int index = 0; index < count; index++) {
				futures.add(pool.submit(() -> {
					assertTrue(start.await(10, TimeUnit.SECONDS));
					return action.call();
				}));
			}
			start.countDown();
			List<Long> results = new ArrayList<>();
			for (var future : futures) {
				results.add(future.get(30, TimeUnit.SECONDS));
			}
			return results;
		} finally {
			pool.shutdownNow();
			assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
		}
	}
}
