package com.dongsoop.dongsoop.notice.notification.query;

import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.memberdevice.dto.DeviceSubscription;
import com.dongsoop.dongsoop.memberdevice.entity.MemberDevice;
import java.util.Collection;
import java.util.List;

public interface NoticeNotificationTargetQuery {
    List<MemberDevice> searchDevicesByDepartments(Collection<DepartmentType> departmentTypes);
    List<DeviceSubscription> findSubscriptionsByDeviceIds(Collection<Long> deviceIds);
}
