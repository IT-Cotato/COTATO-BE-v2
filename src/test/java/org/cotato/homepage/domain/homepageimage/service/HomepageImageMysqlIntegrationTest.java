package org.cotato.homepage.domain.homepageimage.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.cotato.homepage.domain.homepageimage.entity.HomepageImage;
import org.cotato.homepage.domain.homepageimage.model.HomepageImageCrop;
import org.cotato.homepage.domain.homepageimage.model.HomepageImageException;
import org.cotato.homepage.domain.homepageimage.model.HomepageImageSettings;
import org.cotato.homepage.domain.homepageimage.model.HomepageImageSlot;
import org.cotato.homepage.domain.homepageimage.repository.HomepageImageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/** 별도 로컬 MySQL에서 실제 사진 DDL·JPA·낙관적 잠금만 검증한다. 운영 앱·DB·GCS는 로드하지 않는다. */
@DataJpaTest(properties = {"spring.config.location=optional:classpath:/homepage-image-mysql-test-only.properties",
	"spring.jpa.hibernate.ddl-auto=none", "spring.jpa.show-sql=false",
	"spring.datasource.hikari.maximum-pool-size=12", "spring.datasource.hikari.connection-timeout=5000",
	"logging.level.root=WARN", "logging.level.org.hibernate.SQL=OFF"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = HomepageImageMysqlIntegrationTest.DatabaseConfig.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@EnabledIfEnvironmentVariable(named = "HOMEPAGE_IMAGE_MYSQL_JDBC_URL",
	matches = "jdbc:mysql://127\\.0\\.0\\.1:[0-9]+/homepage_image_test(?:\\?.*)?")
class HomepageImageMysqlIntegrationTest {
	private static final HomepageImageSlot SLOT = HomepageImageSlot.SCHEDULE_OT;
	private static final HomepageImageCrop DESKTOP = new HomepageImageCrop(31.5, 72.25, 1.75);
	private static final HomepageImageCrop MOBILE = new HomepageImageCrop(65.5, 40.25, 2.15);

	/** 사진 엔티티·실제 저장 어댑터만 등록한다. 다른 도메인 저장소와 운영 설정은 스캔하지 않는다. */
	@Configuration(proxyBeanMethods = false)
	@EntityScan(basePackageClasses = HomepageImage.class)
	@EnableJpaRepositories(basePackageClasses = HomepageImageRepository.class)
	@Import(JpaHomepageImageStore.class)
	static class DatabaseConfig {
	}

	@DynamicPropertySource
	static void database(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", () -> System.getenv("HOMEPAGE_IMAGE_MYSQL_JDBC_URL"));
		registry.add("spring.datasource.username", () -> "photo_test");
		registry.add("spring.datasource.password", () -> System.getenv("HOMEPAGE_IMAGE_MYSQL_TEST_PASSWORD"));
		registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
	}

	@Autowired
	private JpaHomepageImageStore store;
	@Autowired
	private HomepageImageRepository repository;
	@Autowired
	private JdbcTemplate jdbc;
	@Autowired
	private PlatformTransactionManager transactions;

	@BeforeEach
	void prepareIsolatedSchema() throws Exception {
		// 1. 대상 DB·주소를 확인하기 전에는 어떤 DDL도 실행하지 않는다.
		assertEquals("homepage_image_test", jdbc.queryForObject("SELECT DATABASE()", String.class));
		try (var connection = jdbc.getDataSource().getConnection()) {
			assertTrue(connection.getMetaData().getURL().matches(
				"jdbc:mysql://127\\.0\\.0\\.1:[0-9]+/homepage_image_test(?:\\?.*)?"));
		}
		assertTrue(jdbc.queryForObject("SELECT @@sql_mode", String.class).contains("STRICT"));
		// 2. 시험 DB의 사진 테이블에만 실제 배포용 DDL을 적용한다. Hibernate 자동 생성은 사용하지 않는다.
		jdbc.execute("DROP TABLE IF EXISTS homepage_image");
		applyPhotoDdl();
	}

	private void applyPhotoDdl() throws Exception {
		try (var connection = jdbc.getDataSource().getConnection()) {
			ScriptUtils.executeSqlScript(connection,
				new ClassPathResource("db/manual/20261125_add_homepage_image.sql"));
		}
	}

	private String key(HomepageImageSlot slot, int index) {
		return slot.prefix() + "/00000000-0000-4000-8000-" + String.format("%012d", index) + ".webp";
	}

	private HomepageImageSettings find(HomepageImageSlot slot) {
		return store.findAll().stream().filter(value -> value.slot() == slot).findFirst().orElseThrow();
	}

	private void assertConflict(Runnable work) {
		assertEquals(409, assertThrows(HomepageImageException.class, work::run).status());
	}

	@Test
	void actualManualDdlSupportsEmptyJpaRead() {
		assertTrue(store.findAll().isEmpty());
		assertEquals(10, jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.columns "
			+ "WHERE table_schema=DATABASE() AND table_name='homepage_image'", Integer.class).intValue());
		assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.table_constraints "
			+ "WHERE table_schema=DATABASE() AND table_name='homepage_image' AND constraint_type='CHECK'",
			Integer.class).intValue());
	}

	@Test
	void committedRowReloadsBothDeviceCropsFromMysql() {
		var saved = store.save(SLOT, key(SLOT, 1), DESKTOP, MOBILE, null);
		assertEquals(0, saved.revision());
		assertEquals(saved, find(SLOT));
		assertEquals(1, repository.count());
	}

	@Test
	void replacementCommitsFileAndBothCropsWithOneVersionIncrement() {
		var before = store.save(SLOT, key(SLOT, 1), DESKTOP, MOBILE, null);
		var after = store.save(SLOT, key(SLOT, 2), MOBILE, DESKTOP, before.revision());
		assertEquals(1, after.revision());
		assertEquals(key(SLOT, 2), after.objectKey());
		assertEquals(MOBILE, after.desktop());
		assertEquals(DESKTOP, after.mobile());
		assertEquals(after, find(SLOT));
		assertEquals(1, repository.count());
	}

	@Test
	void staleRevisionKeepsWinnerAndUnrelatedPhoto() {
		var first = store.save(SLOT, key(SLOT, 1), DESKTOP, MOBILE, null);
		var other = store.save(HomepageImageSlot.PART_PM, key(HomepageImageSlot.PART_PM, 1), MOBILE, DESKTOP, null);
		var winner = store.save(SLOT, key(SLOT, 2), MOBILE, DESKTOP, first.revision());
		assertConflict(() -> store.save(SLOT, key(SLOT, 3), DESKTOP, MOBILE, first.revision()));
		assertEquals(winner, find(SLOT));
		assertEquals(other, find(HomepageImageSlot.PART_PM));
		assertEquals(2, repository.count());
	}

	@Test
	void missingExpectedRevisionCannotOverwriteExistingPhoto() {
		var before = store.save(SLOT, key(SLOT, 1), DESKTOP, MOBILE, null);
		assertConflict(() -> store.save(SLOT, key(SLOT, 2), MOBILE, DESKTOP, null));
		assertEquals(before, find(SLOT));
	}

	@Test
	void expectedRevisionForMissingPhotoCannotCreateRow() {
		assertConflict(() -> store.save(SLOT, key(SLOT, 1), DESKTOP, MOBILE, 0L));
		assertEquals(0, repository.count());
	}

	@Test
	void revisionAboveBoxedNumberCacheStillComparesByValue() {
		store.save(SLOT, key(SLOT, 1), DESKTOP, MOBILE, null);
		jdbc.update("UPDATE homepage_image SET revision=200 WHERE slot=?", SLOT.name());
		var saved = store.save(SLOT, key(SLOT, 2), MOBILE, DESKTOP, 200L);
		assertEquals(201, saved.revision());
		assertEquals(saved, find(SLOT));
	}

	@ParameterizedTest
	@CsvSource({"desktop_x,-1", "mobile_y,101", "desktop_zoom,0.99", "mobile_zoom,3.01"})
	void mysqlCheckConstraintRejectsOutOfRangeDirectSql(String column, double value) {
		var before = store.save(SLOT, key(SLOT, 1), DESKTOP, MOBILE, null);
		assertTrue(List.of("desktop_x", "mobile_y", "desktop_zoom", "mobile_zoom").contains(column));
		DataAccessException failure = assertThrows(DataAccessException.class,
			() -> jdbc.update("UPDATE homepage_image SET " + column + "=? WHERE slot=?", value, SLOT.name()));
		assertTrue(failure.getMostSpecificCause().getMessage().contains("ck_homepage_image"));
		assertEquals(before, find(SLOT));
	}

	@Test
	void uniqueSlotConstraintRejectsDuplicateDirectSql() {
		var before = store.save(SLOT, key(SLOT, 1), DESKTOP, MOBILE, null);
		assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
			"INSERT INTO homepage_image (slot,object_key,desktop_x,desktop_y,desktop_zoom,"
				+ "mobile_x,mobile_y,mobile_zoom,revision) SELECT slot,object_key,desktop_x,desktop_y,desktop_zoom,"
				+ "mobile_x,mobile_y,mobile_zoom,revision FROM homepage_image WHERE slot=?", SLOT.name()));
		assertEquals(1, repository.count());
		assertEquals(before, find(SLOT));
	}

	@Test
	void databaseFailureRollsBackKeyCropsAndVersionTogether() {
		var before = store.save(SLOT, key(SLOT, 1), DESKTOP, MOBILE, null);
		assertConflict(() -> store.save(SLOT, "x".repeat(513), MOBILE, DESKTOP, before.revision()));
		assertEquals(before, find(SLOT));
		assertEquals(1, repository.count());
	}

	@Test
	void photoDdlLeavesUnrelatedSyntheticRowsUntouched() throws Exception {
		jdbc.execute("CREATE TABLE IF NOT EXISTS photo_test_guard (id INT PRIMARY KEY, marker VARCHAR(30))");
		try {
			jdbc.update("INSERT INTO photo_test_guard VALUES (1, 'synthetic-unchanged')");
			jdbc.execute("DROP TABLE homepage_image");
			applyPhotoDdl();
			assertEquals("synthetic-unchanged",
				jdbc.queryForObject("SELECT marker FROM photo_test_guard WHERE id=1", String.class));
		} finally {
			jdbc.execute("DROP TABLE photo_test_guard");
		}
	}

	@RepeatedTest(3)
	void concurrentInitialInsertsCreateExactlyOneRow() throws Exception {
		assertSingleWinner(concurrently(Collections.nCopies(10, SLOT), null), 0);
	}

	@RepeatedTest(3)
	void concurrentExistingVersionUpdatesHaveExactlyOneWinner() throws Exception {
		var before = store.save(SLOT, key(SLOT, 20), DESKTOP, MOBILE, null);
		assertSingleWinner(concurrently(Collections.nCopies(10, SLOT), before.revision()), 1);
	}

	@Test
	void independentPhotoSlotsAllCommitConcurrently() throws Exception {
		List<SaveResult> results = concurrently(Arrays.asList(HomepageImageSlot.values()), null);
		assertEquals(10, results.size());
		assertTrue(results.stream().allMatch(value -> value.status() == 200));
		assertEquals(10, repository.count());
		for (SaveResult result : results) {
			assertEquals(result.saved(), find(result.saved().slot()));
			assertEquals(0, result.saved().revision());
		}
		System.out.println("MYSQL_RACE distinct_slots=10 success=10 conflict=0 rows=10");
	}

	private void assertSingleWinner(List<SaveResult> results, long revision) {
		assertEquals(1, results.stream().filter(value -> value.status() == 200).count());
		assertEquals(9, results.stream().filter(value -> value.status() == 409).count());
		var winner = results.stream().filter(value -> value.status() == 200).findFirst().orElseThrow().saved();
		assertEquals(1, repository.count());
		assertEquals(revision, winner.revision());
		assertEquals(winner, find(SLOT));
		System.out.println("MYSQL_RACE workers=10 success=1 conflict=9 rows=1 revision=" + revision);
	}

	private List<SaveResult> concurrently(List<HomepageImageSlot> slots, Long expectedRevision) throws Exception {
		ExecutorService pool = Executors.newFixedThreadPool(slots.size());
		CountDownLatch ready = new CountDownLatch(slots.size());
		CountDownLatch start = new CountDownLatch(1);
		List<java.util.concurrent.Future<SaveResult>> work = new ArrayList<>();
		try {
			for (int i = 0; i < slots.size(); i++) {
				HomepageImageSlot slot = slots.get(i);
				int index = i;
				work.add(pool.submit(() -> {
					try {
						var saved = new TransactionTemplate(transactions).execute(status -> {
							// 3. 각 실제 트랜잭션이 같은 초기 스냅샷을 읽은 뒤 함께 저장하도록 겹침을 강제한다.
							assertEquals(expectedRevision == null, repository.findBySlot(slot).isEmpty());
							ready.countDown();
							try {
								assertTrue(start.await(15, TimeUnit.SECONDS));
							} catch (InterruptedException exception) {
								Thread.currentThread().interrupt();
								throw new IllegalStateException("MySQL race interrupted", exception);
							}
							return store.save(slot, key(slot, index), new HomepageImageCrop(index, 50, 1.5), MOBILE,
								expectedRevision);
						});
						return new SaveResult(200, saved);
					} catch (HomepageImageException exception) {
						return new SaveResult(exception.status(), null);
					}
				}));
			}
			assertTrue(ready.await(15, TimeUnit.SECONDS), "All MySQL transactions must read before any save");
			start.countDown();
			List<SaveResult> results = new ArrayList<>();
			for (var future : work) {
				results.add(future.get(20, TimeUnit.SECONDS));
			}
			return results;
		} finally {
			// 4. 실패한 시험도 대기 중인 트랜잭션과 작업 스레드를 남기지 않는다.
			start.countDown();
			pool.shutdownNow();
			assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
		}
	}

	/** 각 작업의 저장 상태와 실제 커밋 결과만 보관한다. 계정·파일 내용·저장소 자격 증명은 포함하지 않는다. */
	private record SaveResult(int status, HomepageImageSettings saved) {
	}
}
