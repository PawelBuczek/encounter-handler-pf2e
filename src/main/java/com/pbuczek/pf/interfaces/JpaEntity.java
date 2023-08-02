package com.pbuczek.pf.interfaces;

import java.time.LocalDateTime;

public interface JpaEntity {
    Integer getId();

    LocalDateTime getTimeCreated();
}
