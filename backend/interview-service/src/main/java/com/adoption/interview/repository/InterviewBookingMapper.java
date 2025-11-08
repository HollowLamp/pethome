package com.adoption.interview.repository;

import com.adoption.interview.model.InterviewBooking;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface InterviewBookingMapper {

    @Select("SELECT id, app_id AS appId, slot_id AS slotId, status, " +
            "created_at AS createdAt, updated_at AS updatedAt " +
            "FROM interview_booking WHERE app_id = #{appId}")
    InterviewBooking findByAppId(@Param("appId") Long appId);

    @Select("SELECT ib.id, ib.app_id AS appId, ib.slot_id AS slotId, ib.status, " +
            "ib.created_at AS createdAt, ib.updated_at AS updatedAt " +
            "FROM interview_booking ib " +
            "INNER JOIN schedule_slot ss ON ib.slot_id = ss.id " +
            "WHERE ss.org_id = #{orgId} ORDER BY ib.created_at DESC")
    List<InterviewBooking> findByOrgId(@Param("orgId") Long orgId);

    @Select("SELECT id, app_id AS appId, slot_id AS slotId, status, " +
            "created_at AS createdAt, updated_at AS updatedAt " +
            "FROM interview_booking WHERE id = #{id}")
    InterviewBooking findById(@Param("id") Long id);

    @Insert("INSERT INTO interview_booking (app_id, slot_id, status, created_at, updated_at) " +
            "VALUES (#{appId}, #{slotId}, #{status}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(InterviewBooking booking);

    @Update("UPDATE interview_booking SET status = #{status}, updated_at = NOW() WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    @Update("UPDATE interview_booking SET status = #{status}, updated_at = NOW() WHERE app_id = #{appId}")
    int updateStatusByAppId(@Param("appId") Long appId, @Param("status") String status);
}

