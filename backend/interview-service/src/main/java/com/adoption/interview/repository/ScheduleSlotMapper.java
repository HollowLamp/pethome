package com.adoption.interview.repository;

import com.adoption.interview.model.ScheduleSlot;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ScheduleSlotMapper {

    @Select("SELECT id, org_id AS orgId, start_at AS startAt, end_at AS endAt, is_open AS isOpen, " +
            "created_at AS createdAt, updated_at AS updatedAt " +
            "FROM schedule_slot WHERE org_id = #{orgId} AND is_open = TRUE ORDER BY start_at ASC")
    List<ScheduleSlot> findByOrgId(@Param("orgId") Long orgId);

    @Select("SELECT id, org_id AS orgId, start_at AS startAt, end_at AS endAt, is_open AS isOpen, " +
            "created_at AS createdAt, updated_at AS updatedAt " +
            "FROM schedule_slot WHERE org_id = #{orgId} ORDER BY start_at ASC")
    List<ScheduleSlot> findAllByOrgId(@Param("orgId") Long orgId);

    @Select("SELECT id, org_id AS orgId, start_at AS startAt, end_at AS endAt, is_open AS isOpen, " +
            "created_at AS createdAt, updated_at AS updatedAt " +
            "FROM schedule_slot WHERE id = #{id}")
    ScheduleSlot findById(@Param("id") Long id);

    @Insert("INSERT INTO schedule_slot (org_id, start_at, end_at, is_open, created_at, updated_at) " +
            "VALUES (#{orgId}, #{startAt}, #{endAt}, #{isOpen}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ScheduleSlot slot);

    @Update("UPDATE schedule_slot SET is_open = #{isOpen}, updated_at = NOW() WHERE id = #{id}")
    int updateOpenStatus(@Param("id") Long id, @Param("isOpen") Boolean isOpen);

    @Update("UPDATE schedule_slot SET start_at = #{startAt}, end_at = #{endAt}, is_open = #{isOpen}, updated_at = NOW() WHERE id = #{id}")
    int update(ScheduleSlot slot);

    @Delete("DELETE FROM schedule_slot WHERE id = #{id} AND org_id = #{orgId}")
    int deleteByIdAndOrgId(@Param("id") Long id, @Param("orgId") Long orgId);
}

