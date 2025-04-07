package com.dongsoop.dongsoop.notice.entity;

import com.dongsoop.dongsoop.department.Department;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Entity
@AllArgsConstructor
@NoArgsConstructor
public class Notice {

    @EmbeddedId
    private NoticeKey id;

    public Notice(Department department, NoticeDetails noticeDetails) {
        this.id = new NoticeKey(department, noticeDetails);
    }

    @Embeddable
    @AllArgsConstructor
    @NoArgsConstructor
    public static class NoticeKey {
        @ManyToOne(fetch = FetchType.EAGER)
        @JoinColumn(name = "department_id")
        private Department department;

        @ManyToOne(fetch = FetchType.EAGER)
        @JoinColumn(name = "notice_details_id")
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
            NoticeKey that = (NoticeKey) o;
            return Objects.equals(department, that.department) && Objects.equals(noticeDetails, that.noticeDetails);
        }

        @Override
        public int hashCode() {
            return Objects.hash(department, noticeDetails);
        }
    }
}
