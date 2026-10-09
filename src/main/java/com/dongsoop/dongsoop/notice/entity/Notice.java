package com.dongsoop.dongsoop.notice.entity;

import com.dongsoop.dongsoop.department.entity.Department;
import com.dongsoop.dongsoop.department.entity.DepartmentType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Entity
@IdClass(Notice.NoticeKey.class)
@AllArgsConstructor
@NoArgsConstructor
public class Notice {

    @Id
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "department_id")
    private Department department;

    @Id
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "notice_details_id")
    private NoticeDetails noticeDetails;

    private LocalDateTime deletedAt;

    public Notice(Department department, NoticeDetails noticeDetails) {
        this.department = department;
        this.noticeDetails = noticeDetails;
    }

    public Department getDepartment() {
        return department;
    }

    public NoticeDetails getNoticeDetails() {
        return noticeDetails;
    }

    public boolean markDeleted(LocalDateTime deletedAt) {
        if (this.deletedAt != null) {
            return false;
        }

        this.deletedAt = deletedAt;
        return true;
    }

    public boolean restore() {
        if (deletedAt == null) {
            return false;
        }

        deletedAt = null;
        return true;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    public static class NoticeKey implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        @Enumerated(EnumType.STRING)
        private DepartmentType department;

        private Long noticeDetails;

        // JPA 엔티티 비교 및 캐싱 시 사용된다
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            NoticeKey that = (NoticeKey) o;
            return Objects.equals(department, that.department) && Objects.equals(noticeDetails, that.noticeDetails);
        }

        @Override
        public int hashCode() {
            return Objects.hash(department, noticeDetails);
        }
    }
}
