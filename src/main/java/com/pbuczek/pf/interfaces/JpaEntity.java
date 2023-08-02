package com.pbuczek.pf.interfaces;

import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDateTime;

@ResponseBody
public interface JpaEntity {
    Integer getId();

    LocalDateTime getTimeCreated();
}
