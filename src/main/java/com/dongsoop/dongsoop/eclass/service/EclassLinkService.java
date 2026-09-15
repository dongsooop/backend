package com.dongsoop.dongsoop.eclass.service;

import com.dongsoop.dongsoop.eclass.dto.EclassLinkResponse;

public interface EclassLinkService {

    EclassLinkResponse link(String fid, String deviceToken, String moodleToken);

    EclassLinkResponse getStatus(String fid, String deviceToken);

    void unlink(String fid, String deviceToken);

    void syncNow(String fid, String deviceToken);
}
