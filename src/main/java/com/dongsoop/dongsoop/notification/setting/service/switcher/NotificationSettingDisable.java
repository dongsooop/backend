package com.dongsoop.dongsoop.notification.setting.service.switcher;

import com.dongsoop.dongsoop.member.service.MemberService;
import com.dongsoop.dongsoop.memberdevice.repository.MemberDeviceRepository;
import com.dongsoop.dongsoop.notification.setting.repository.NotificationSettingRepository;
import org.springframework.stereotype.Component;

@Component
public class NotificationSettingDisable extends NotificationSettingSwitcher {

    public NotificationSettingDisable(MemberService memberService,
                                      MemberDeviceRepository memberDeviceRepository,
                                      NotificationSettingRepository notificationSettingRepository) {
        super(memberService, memberDeviceRepository, notificationSettingRepository);
    }

    @Override
    protected boolean shouldEnable() {
        return false;
    }
}
