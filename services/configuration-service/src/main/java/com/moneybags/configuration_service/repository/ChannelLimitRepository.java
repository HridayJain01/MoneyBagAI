package com.moneybags.configuration_service.repository;

import com.moneybags.configuration_service.entity.ChannelLimit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChannelLimitRepository extends JpaRepository<ChannelLimit, Long> {
    List<ChannelLimit> findAll();
    List<ChannelLimit> findByChannelAndLimitType(String channel, String limitType);
}
