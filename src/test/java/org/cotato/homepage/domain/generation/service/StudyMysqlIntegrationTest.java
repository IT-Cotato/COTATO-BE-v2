package org.cotato.homepage.domain.generation.service;

import static org.junit.jupiter.api.Assertions.*;

import org.cotato.homepage.domain.generation.entity.Project;
import org.cotato.homepage.domain.generation.enums.ProjectType;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/** 새 로컬 MySQL에서 기존 프로젝트 행을 유지한 채 스터디 타입의 저장·수정·삭제만 확인한다. 운영 설정은 읽지 않는다. */
@EnabledIfEnvironmentVariable(named = "HOMEPAGE_IMAGE_MYSQL_JDBC_URL",
	matches = "jdbc:mysql://127\\.0\\.0\\.1:[0-9]+/homepage_image_test(?:\\?.*)?")
class StudyMysqlIntegrationTest {
	@Test
	void studyCrudKeepsExistingProjectRow() {
		Configuration configuration = new Configuration().addAnnotatedClass(Project.class)
			.setProperty("hibernate.connection.url", System.getenv("HOMEPAGE_IMAGE_MYSQL_JDBC_URL"))
			.setProperty("hibernate.connection.username", "photo_test")
			.setProperty("hibernate.connection.password", System.getenv("HOMEPAGE_IMAGE_MYSQL_TEST_PASSWORD"))
			.setProperty("hibernate.hbm2ddl.auto", "create-drop")
			.setProperty("hibernate.show_sql", "false");
		// 1. URL 조건이 정확히 로컬 전용 시험 DB일 때만 프로젝트 시험 테이블을 만든다.
		try (SessionFactory factory = configuration.buildSessionFactory()) {
			Long projectId;
			Long studyId;
			try (var session = factory.openSession()) {
				var transaction = session.beginTransaction();
				var project = Project.builder().name("기존 합성 프로젝트").generationId(14L)
					.projectType(ProjectType.DEMODAY).build();
				var study = Project.builder().name("신규 합성 스터디").generationId(14L)
					.projectType(ProjectType.STUDY).build();
				session.persist(project);
				session.persist(study);
				transaction.commit();
				projectId = project.getId();
				studyId = study.getId();
			}
			// 2. 새 세션에서 다시 읽고 스터디만 변경한 뒤 삭제한다.
			try (var session = factory.openSession()) {
				var transaction = session.beginTransaction();
				var study = session.find(Project.class, studyId);
				assertEquals(ProjectType.STUDY, study.getProjectType());
				study.update("수정한 합성 스터디", "소개", "설명", ProjectType.STUDY,
					"https://example.invalid/study", null, null, 14L);
				transaction.commit();
			}
			try (var session = factory.openSession()) {
				var transaction = session.beginTransaction();
				assertEquals("수정한 합성 스터디", session.find(Project.class, studyId).getName());
				session.remove(session.find(Project.class, studyId));
				transaction.commit();
			}
			// 3. 기존 행의 값과 개수는 그대로이며 새 스터디만 삭제됐는지 재조회한다.
			try (var session = factory.openSession()) {
				assertNull(session.find(Project.class, studyId));
				assertEquals("기존 합성 프로젝트", session.find(Project.class, projectId).getName());
				assertEquals(ProjectType.DEMODAY, session.find(Project.class, projectId).getProjectType());
				assertEquals(1L, session.createQuery("select count(p) from Project p", Long.class).getSingleResult());
			}
		}
	}
}
