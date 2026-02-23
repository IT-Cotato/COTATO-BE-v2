package org.cotato.homepage.common.schedule;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

import org.cotato.homepage.common.sse.SseSender;
import org.cotato.homepage.common.util.TimeUtil;
import org.cotato.homepage.domain.attendance.entity.Attendance;
import org.cotato.homepage.domain.attendance.enums.AttendanceOpenStatus;
import org.cotato.homepage.domain.attendance.repository.AttendanceRepository;
import org.cotato.homepage.domain.generation.entity.AttendanceNotification;
import org.cotato.homepage.domain.generation.entity.Generation;
import org.cotato.homepage.domain.generation.entity.GenerationMember;
import org.cotato.homepage.domain.generation.entity.Session;
import org.cotato.homepage.domain.generation.repository.AttendanceNotificationRepository;
import org.cotato.homepage.domain.generation.repository.GenerationMemberRepository;
import org.cotato.homepage.domain.generation.repository.GenerationRepository;
import org.cotato.homepage.domain.generation.service.component.SessionReader;
import org.cotato.homepage.domain.member.entity.Member;
import org.cotato.homepage.domain.member.entity.RefusedMember;
import org.cotato.homepage.domain.member.enums.MemberStatus;
import org.cotato.homepage.domain.member.repository.MemberRepository;
import org.cotato.homepage.domain.member.repository.RefusedMemberRepository;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SchedulerService {

	private final AttendanceNotificationRepository attendanceNotificationRepository;
	private final AttendanceRepository attendanceRepository;
	private final RefusedMemberRepository refusedMemberRepository;
	private final MemberRepository memberRepository;
	private final GenerationRepository generationRepository;
	private final GenerationMemberRepository generationMemberRepository;
	private final SessionReader sessionReader;
	private final SseSender sseSender;
	private final TaskScheduler taskScheduler;
	private final Map<Long, ScheduledFuture<?>> notificationByAttendanceId = new ConcurrentHashMap<>();

	@PostConstruct
	protected void restoreScheduledTasksFromDB() {
		LocalDateTime now = LocalDateTime.now();

		List<AttendanceNotification> attendanceNotifications = attendanceNotificationRepository.findAllByDoneFalse();
		attendanceNotifications.forEach(
			attendanceNotification -> {
				Session session = sessionReader.findById(attendanceNotification.getAttendance().getSessionId());
				if (session.getSessionDateTime().isBefore(now)) {
					return;
				}

				ScheduledFuture<?> schedule = taskScheduler.schedule(
					() -> {
						log.info("schedule attendance notification: session id <{}>, time <{}>",
							session.getId(), session.getSessionDateTime());
						sseSender.sendAttendanceStartNotification(attendanceNotification);
						notificationByAttendanceId.remove(attendanceNotification.getAttendance().getId());
					},
					TimeUtil.getSeoulZoneTime(session.getSessionDateTime()).toInstant()
				);
				notificationByAttendanceId.put(attendanceNotification.getAttendance().getId(), schedule);
				log.info("restored attendance notification: attendance id <{}>",
					attendanceNotification.getAttendance().getId());
			});

		List<Attendance> activeAttendances = attendanceRepository.findAllByLateDeadLineAfter(now);
		activeAttendances.forEach(attendance -> {
			if (attendance.getAttendanceDeadLine().isAfter(now)) {
				taskScheduler.schedule(
					() -> sseSender.sendAttendanceStatusNotification(attendance.getId(), AttendanceOpenStatus.LATE),
					TimeUtil.getSeoulZoneTime(attendance.getAttendanceDeadLine()).toInstant()
				);
				log.info("restored LATE notification: attendance id <{}>", attendance.getId());
			}

			taskScheduler.schedule(
				() -> sseSender.sendAttendanceStatusNotification(attendance.getId(), AttendanceOpenStatus.CLOSED),
				TimeUtil.getSeoulZoneTime(attendance.getLateDeadLine()).toInstant()
			);
			log.info("restored CLOSED notification: attendance id <{}>", attendance.getId());
		});
	}

	@Transactional
	@Scheduled(cron = "0 0 0 * * *")
	public void updateRefusedMember() {
		log.info("updateRefusedMember 시작 {}", LocalDateTime.now());
		LocalDateTime now = LocalDateTime.now();
		List<RefusedMember> deleteRefusedMembers = refusedMemberRepository.findAllByCreatedAtBefore(now.minusDays(30));

		List<Member> refusedMembers = new ArrayList<>();
		deleteRefusedMembers.forEach(refusedMember -> {
			if (refusedMember.getMember().isRejectedMember()) {
				refusedMembers.add(refusedMember.getMember());
			}
		});

		memberRepository.deleteAll(refusedMembers);
		refusedMemberRepository.deleteAll(deleteRefusedMembers);
	}

	@Transactional
	@Scheduled(cron = "0 0 0,8,16 * * *")
	public void updateExpiredGenerationMembers() {
		log.info("updateExpiredGenerationMembers 시작 {}", LocalDateTime.now());
		LocalDate yesterday = LocalDate.now().minusDays(1);

		// 어제 종료된 기수 조회
		Optional<Generation> expiredGeneration = generationRepository.findByPeriod_EndDate(yesterday);
		if (expiredGeneration.isEmpty()) {
			log.info("어제 종료된 기수가 없습니다.");
			return;
		}

		// 종료된 기수의 APPROVED 회원들을 RETIRED로 변경
		List<GenerationMember> expiredMembers = generationMemberRepository
			.findAllByGeneration(expiredGeneration.get());

		List<Member> approvedMembers = expiredMembers.stream()
			.map(GenerationMember::getMember)
			.filter(member -> member.getStatus() == MemberStatus.APPROVED)
			.toList();

		approvedMembers.forEach(member -> member.updateStatus(MemberStatus.RETIRED));
		memberRepository.saveAll(approvedMembers);

		log.info("{}명의 회원이 수료로 변경되었습니다.", approvedMembers.size());
	}

	public void scheduleAttendanceNotification(final AttendanceNotification attendanceNotification) {
		Attendance attendance = attendanceNotification.getAttendance();
		Session session = sessionReader.findById(attendance.getSessionId());

		ScheduledFuture<?> schedule = taskScheduler.schedule(() -> {
			log.info("schedule attendance notification: session id <{}>, time <{}>", attendance.getSessionId(),
				session.getSessionDateTime());
			sseSender.sendAttendanceStartNotification(attendanceNotification);
			notificationByAttendanceId.remove(attendance.getId());
		}, TimeUtil.getSeoulZoneTime(session.getSessionDateTime()).toInstant());
		notificationByAttendanceId.put(attendance.getId(), schedule);

		taskScheduler.schedule(
			() -> sseSender.sendAttendanceStatusNotification(attendance.getId(), AttendanceOpenStatus.LATE),
			TimeUtil.getSeoulZoneTime(attendance.getAttendanceDeadLine()).toInstant()
		);

		taskScheduler.schedule(
			() -> sseSender.sendAttendanceStatusNotification(attendance.getId(), AttendanceOpenStatus.CLOSED),
			TimeUtil.getSeoulZoneTime(attendance.getLateDeadLine()).toInstant()
		);
	}
}
