package com.jobtracker.application.repository;

import com.jobtracker.application.entity.ApplicationStatus;

public interface StatusCountProjection {
    ApplicationStatus getStatus();
    long getCount();
}
