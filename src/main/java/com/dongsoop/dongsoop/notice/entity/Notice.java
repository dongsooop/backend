package com.dongsoop.dongsoop.notice.entity;

import com.dongsoop.dongsoop.department.Department;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Entity
@AllArgsConstructor
@NoArgsConstructor
public class Notice {

    @Id
    private DepartmentNoticeKey id;

    public Notice(Department department, NoticeDetails noticeDetails) {
        this.id = new DepartmentNoticeKey(department, noticeDetails);
    }

    @Embeddable
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DepartmentNoticeKey {
        @ManyToOne(fetch = FetchType.EAGER)
        @JoinColumn()
        private Department department;

        @ManyToOne(fetch = FetchType.EAGER)
        @JoinColumn
        private NoticeDetails noticeDetails;

        // JPA 엔티티 비교 및 캐싱 시 사용된다
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            DepartmentNoticeKey that = (DepartmentNoticeKey) o;
            return department == that.department && Objects.equals(noticeDetails, that.noticeDetails);
        }

        @Override
        public int hashCode() {
            return Objects.hash(department, noticeDetails);
        }
    }
}
