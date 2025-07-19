package com.dongsoop.dongsoop.recruitment.repository;

import com.dongsoop.dongsoop.mypage.dto.MyRecruitmentOverview;
import com.dongsoop.dongsoop.recruitment.board.tutoring.entity.TutoringBoard;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RecruitmentRepository extends JpaRepository<TutoringBoard, Long> {

    /**
     * 특정 회원이 신청한 전체 타입의 모집 게시판 목록을 페이지 단위로 조회합니다.
     *
     * @param memberId 회원 ID
     * @param pageable 페이지 정보
     * @return 신청한 모집 게시판 목록
     */
    @Query(value = """
            SELECT
                id, volunteer, startAt, endAt, title, content, tags, departmentTypeList, boardType, createdAt, status
            FROM (
                (SELECT
                    p.id AS id,
                    (SELECT COUNT(DISTINCT subpa.member_id)::BIGINT
                        FROM project_apply subpa
                        WHERE subpa.project_board_id = p.id) AS volunteer,
                    p.start_at AS startAt,
                    p.end_at AS endAt,
                    p.title AS title,
                    p.content AS content,
                    p.tags AS tags,
                    STRING_AGG(d.name, ',') AS departmentTypeList,
                    'PROJECT' AS boardType,
                    p.created_At AS createdAt,
                    CASE
                        WHEN p.end_at > NOW() AND p.start_at <= NOW() THEN 'RECRUITING'
                        WHEN p.start_at > NOW() THEN 'WAITING'
                        ELSE 'ENDED'
                    END AS status,
                    pa.apply_time AS applyTime
                FROM project_board p
                LEFT JOIN project_apply pa ON p.id = pa.project_board_id
                LEFT JOIN project_board_department pd ON p.id = pd.project_board_id
                LEFT JOIN department d ON pd.department_id = d.id
                WHERE pa.member_id = :memberId
                GROUP BY p.id, pa.apply_time)
            
                UNION ALL
            
                (SELECT
                    s.id AS id,
                    (SELECT COUNT(DISTINCT subsa.member_id)::BIGINT
                        FROM study_apply subsa
                        WHERE subsa.study_board_id = s.id) AS volunteer,
                    s.start_at AS startAt,
                    s.end_at AS endAt,
                    s.title AS title,
                    s.content AS content,
                    s.tags AS tags,
                    STRING_AGG(d.name, ',') AS departmentTypeList,
                    'STUDY' AS boardType,
                    s.created_at AS createdAt,
                    CASE
                        WHEN s.end_at > NOW() AND s.start_at <= NOW() THEN 'RECRUITING'
                        WHEN s.start_at > NOW() THEN 'WAITING'
                        ELSE 'ENDED'
                    END AS status,
                    sa.apply_time AS applyTime
                FROM study_board s
                LEFT JOIN study_apply sa ON s.id = sa.study_board_id
                LEFT JOIN study_board_department sd ON s.id = sd.study_board_id
                LEFT JOIN department d ON sd.department_id = d.id
                WHERE sa.member_id = :memberId
                GROUP BY s.id, sa.apply_time)
            
                UNION ALL
            
                (SELECT
                    t.id AS id,
                    (SELECT COUNT(DISTINCT subta.member_id)::BIGINT
                        FROM tutoring_apply subta
                        WHERE subta.tutoring_board_id = t.id) AS volunteer,
                    t.start_at AS startAt,
                    t.end_at AS endAt,
                    t.title AS title,
                    t.content AS content,
                    t.tags AS tags,
                    d.name AS departmentTypeList,
                    'TUTORING' AS boardType,
                    t.created_at AS createdAt,
                    CASE
                        WHEN t.end_at > NOW() AND t.start_at <= NOW() THEN 'RECRUITING'
                        WHEN t.start_at > NOW() THEN 'WAITING'
                        ELSE 'ENDED'
                    END AS status,
                    ta.apply_time AS applyTime
                FROM tutoring_board t
                LEFT JOIN tutoring_apply ta ON t.id = ta.tutoring_board_id
                LEFT JOIN department d ON t.department_id = d.id
                WHERE ta.member_id = :memberId
                GROUP BY t.id, d.name, ta.apply_time)
            ) AS combined_results
            ORDER BY combined_results.applyTime DESC
            LIMIT :#{#pageable.pageSize}
            OFFSET :#{#pageable.offset}
            """,
            nativeQuery = true
    )
    List<MyRecruitmentOverview> findApplyRecruitmentsByMemberId(@Param("memberId") Long memberId,
                                                                @Param("pageable") Pageable pageable);

    /**
     * 특정 회원이 개설한 전체 타입의 모집 게시판 목록을 페이지 단위로 조회합니다.
     *
     * @param memberId 회원 ID
     * @param pageable 페이지 정보
     * @return 개설한 모집 게시판 목록
     */
    @Query(value = """
            SELECT
                id, volunteer, startAt, endAt, title, content, tags, departmentTypeList, boardType, createdAt, status
            FROM (
                (SELECT
                    p.id AS id,
                    (SELECT COUNT(DISTINCT subpa.member_id)::BIGINT
                        FROM project_apply subpa
                        WHERE subpa.project_board_id = p.id) AS volunteer,
                    p.start_at AS startAt,
                    p.end_at AS endAt,
                    p.title AS title,
                    p.content AS content,
                    p.tags AS tags,
                    STRING_AGG(d.name, ',') AS departmentTypeList,
                    'PROJECT' AS boardType,
                    p.created_At AS createdAt,
                    CASE
                        WHEN p.end_at > NOW() AND p.start_at <= NOW() THEN 'RECRUITING'
                        WHEN p.start_at > NOW() THEN 'WAITING'
                        ELSE 'ENDED'
                    END AS status
                FROM project_board p
                LEFT JOIN project_board_department pd ON p.id = pd.project_board_id
                LEFT JOIN department d ON pd.department_id = d.id
                WHERE p.author = :memberId
                GROUP BY p.id)
            
                UNION ALL
            
                (SELECT
                    s.id AS id,
                    (SELECT COUNT(DISTINCT subsa.member_id)::BIGINT
                        FROM study_apply subsa
                        WHERE subsa.study_board_id = s.id) AS volunteer,
                    s.start_at AS startAt,
                    s.end_at AS endAt,
                    s.title AS title,
                    s.content AS content,
                    s.tags AS tags,
                    STRING_AGG(d.name, ',') AS departmentTypeList,
                    'STUDY' AS boardType,
                    s.created_at AS createdAt,
                    CASE
                        WHEN s.end_at > NOW() AND s.start_at <= NOW() THEN 'RECRUITING'
                        WHEN s.start_at > NOW() THEN 'WAITING'
                        ELSE 'ENDED'
                    END AS status
                FROM study_board s
                LEFT JOIN study_board_department sd ON s.id = sd.study_board_id
                LEFT JOIN department d ON sd.department_id = d.id
                WHERE s.author = :memberId
                GROUP BY s.id)
            
                UNION ALL
            
                (SELECT
                    t.id AS id,
                    (SELECT COUNT(DISTINCT subta.member_id)::BIGINT
                        FROM tutoring_apply subta
                        WHERE subta.tutoring_board_id = t.id) AS volunteer,
                    t.start_at AS startAt,
                    t.end_at AS endAt,
                    t.title AS title,
                    t.content AS content,
                    t.tags AS tags,
                    d.name AS departmentTypeList,
                    'TUTORING' AS boardType,
                    t.created_at AS createdAt,
                    CASE
                        WHEN t.end_at > NOW() AND t.start_at <= NOW() THEN 'RECRUITING'
                        WHEN t.start_at > NOW() THEN 'WAITING'
                        ELSE 'ENDED'
                    END AS status
                FROM tutoring_board t
                LEFT JOIN department d ON t.department_id = d.id
                WHERE t.author = :memberId
                GROUP BY t.id, d.name)
            ) AS combined_results
            ORDER BY combined_results.createdAt DESC
            LIMIT :#{#pageable.pageSize}
            OFFSET :#{#pageable.offset}
            """,
            nativeQuery = true
    )
    List<MyRecruitmentOverview> findOpenedRecruitmentsByMemberId(@Param("memberId") Long memberId,
                                                                 @Param("pageable") Pageable pageable);
}
