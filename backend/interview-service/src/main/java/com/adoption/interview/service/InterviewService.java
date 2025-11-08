package com.adoption.interview.service;

import com.adoption.common.api.ApiResponse;
import com.adoption.interview.feign.AdoptionServiceClient;
import com.adoption.interview.model.InterviewBooking;
import com.adoption.interview.model.ScheduleSlot;
import com.adoption.interview.repository.InterviewBookingMapper;
import com.adoption.interview.repository.ScheduleSlotMapper;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class InterviewService {

    private final InterviewBookingMapper interviewBookingMapper;
    private final ScheduleSlotMapper scheduleSlotMapper;
    private final AdoptionServiceClient adoptionServiceClient;

    public InterviewService(InterviewBookingMapper interviewBookingMapper,
                           ScheduleSlotMapper scheduleSlotMapper,
                           AdoptionServiceClient adoptionServiceClient) {
        this.interviewBookingMapper = interviewBookingMapper;
        this.scheduleSlotMapper = scheduleSlotMapper;
        this.adoptionServiceClient = adoptionServiceClient;
    }

    /**
     * 用户提交面谈预约请求
     */
    public ApiResponse<InterviewBooking> requestInterview(Long appId, Long slotId) {
        // 检查是否已有预约
        InterviewBooking existing = interviewBookingMapper.findByAppId(appId);
        if (existing != null) {
            return ApiResponse.error(400, "该申请已有面谈预约");
        }

        // 检查时段是否存在且可预约
        ScheduleSlot slot = scheduleSlotMapper.findById(slotId);
        if (slot == null) {
            return ApiResponse.error(404, "面谈时段不存在");
        }
        if (!slot.getIsOpen()) {
            return ApiResponse.error(400, "该时段不可预约");
        }

        // 创建预约
        InterviewBooking booking = new InterviewBooking();
        booking.setAppId(appId);
        booking.setSlotId(slotId);
        booking.setStatus("REQUESTED");
        interviewBookingMapper.insert(booking);

        // 加载关联信息
        booking.setSlot(slot);

        // 发送 RabbitMQ 消息：通知机构管理员有新的面谈预约请求
        // C端用例：注册用户提交面谈预约 -> 机构管理员收到通知
        try {
            adoptionServiceClient.notifyInterviewRequested(appId);
        } catch (Exception e) {
            // 消息发送失败不影响主流程，只记录日志
            System.err.println("发送面谈预约请求通知失败: " + e.getMessage());
        }

        return ApiResponse.success(booking);
    }

    /**
     * 机构管理员查看预约请求（根据申请ID）
     */
    public ApiResponse<InterviewBooking> getInterviewByAppId(Long appId) {
        InterviewBooking booking = interviewBookingMapper.findByAppId(appId);
        if (booking == null) {
            return ApiResponse.error(404, "未找到面谈预约");
        }
        // 加载时段信息
        ScheduleSlot slot = scheduleSlotMapper.findById(booking.getSlotId());
        booking.setSlot(slot);
        return ApiResponse.success(booking);
    }

    /**
     * 机构管理员查看所有预约请求（根据机构ID）
     */
    public ApiResponse<List<InterviewBooking>> getInterviewRequests(Long orgId) {
        List<InterviewBooking> bookings = interviewBookingMapper.findByOrgId(orgId);
        // 加载时段信息
        for (InterviewBooking booking : bookings) {
            ScheduleSlot slot = scheduleSlotMapper.findById(booking.getSlotId());
            booking.setSlot(slot);
        }
        return ApiResponse.success(bookings);
    }

    /**
     * 机构管理员确认面谈
     */
    public ApiResponse<String> confirmInterview(Long appId) {
        InterviewBooking booking = interviewBookingMapper.findByAppId(appId);
        if (booking == null) {
            return ApiResponse.error(404, "未找到面谈预约");
        }
        if (!"REQUESTED".equals(booking.getStatus())) {
            return ApiResponse.error(400, "该预约状态不允许确认");
        }

        interviewBookingMapper.updateStatusByAppId(appId, "CONFIRMED");

        // 发送 RabbitMQ 消息：通知申请人面谈时间已确认
        // C端用例：注册用户收到系统通知 -> 面谈时间提醒
        try {
            // 加载时段信息
            ScheduleSlot slot = scheduleSlotMapper.findById(booking.getSlotId());
            if (slot != null) {
                // 格式化面谈时间
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                String interviewTime = slot.getStartAt().format(formatter) + " - " + slot.getEndAt().format(formatter);

                // 构建请求体
                Map<String, String> body = new HashMap<>();
                body.put("interviewTime", interviewTime);
                // 如果有机构地址，可以在这里添加
                // body.put("interviewLocation", "机构地址");

                // 调用 adoption-service 发送通知
                adoptionServiceClient.notifyInterviewConfirmed(appId, body);
            }
        } catch (Exception e) {
            // 消息发送失败不影响主流程，只记录日志
            System.err.println("发送面谈确认通知失败: " + e.getMessage());
        }

        return ApiResponse.success("面谈已确认");
    }

    /**
     * 机构管理员完成面谈
     */
    public ApiResponse<String> completeInterview(Long appId) {
        InterviewBooking booking = interviewBookingMapper.findByAppId(appId);
        if (booking == null) {
            return ApiResponse.error(404, "未找到面谈预约");
        }
        if (!"CONFIRMED".equals(booking.getStatus())) {
            return ApiResponse.error(400, "只有已确认的面谈才能完成");
        }

        interviewBookingMapper.updateStatusByAppId(appId, "DONE");
        return ApiResponse.success("面谈已完成");
    }

    /**
     * 机构管理员完成交接
     */
    public ApiResponse<String> completeHandover(Long appId) {
        InterviewBooking booking = interviewBookingMapper.findByAppId(appId);
        if (booking == null) {
            return ApiResponse.error(404, "未找到面谈预约");
        }
        if (!"DONE".equals(booking.getStatus())) {
            return ApiResponse.error(400, "只有已完成面谈的申请才能完成交接");
        }

        // 调用 adoption-service 将申请状态改为 COMPLETED
        try {
            ApiResponse<String> result = adoptionServiceClient.completeHandover(appId);
            if (result != null && result.getCode() == 200) {
                return ApiResponse.success("交接已完成");
            } else {
                return ApiResponse.error(500, result != null ? result.getMessage() : "交接完成失败");
            }
        } catch (Exception e) {
            return ApiResponse.error(500, "调用领养服务失败: " + e.getMessage());
        }
    }

    /**
     * 机构管理员 - 获取机构的时段列表
     */
    public ApiResponse<List<ScheduleSlot>> getScheduleSlots(Long orgId) {
        List<ScheduleSlot> slots = scheduleSlotMapper.findAllByOrgId(orgId);
        return ApiResponse.success(slots);
    }

    /**
     * 机构管理员 - 创建时段
     */
    public ApiResponse<ScheduleSlot> createScheduleSlot(Long orgId, ScheduleSlot slot) {
        slot.setOrgId(orgId);
        if (slot.getIsOpen() == null) {
            slot.setIsOpen(true);
        }
        scheduleSlotMapper.insert(slot);
        return ApiResponse.success(slot);
    }

    /**
     * 机构管理员 - 更新时段
     */
    public ApiResponse<String> updateScheduleSlot(Long orgId, Long slotId, ScheduleSlot slot) {
        ScheduleSlot existing = scheduleSlotMapper.findById(slotId);
        if (existing == null) {
            return ApiResponse.error(404, "时段不存在");
        }
        if (!existing.getOrgId().equals(orgId)) {
            return ApiResponse.error(403, "无权操作此时段");
        }
        slot.setId(slotId);
        slot.setOrgId(orgId);
        scheduleSlotMapper.update(slot);
        return ApiResponse.success("时段已更新");
    }

    /**
     * 机构管理员 - 删除时段
     */
    public ApiResponse<String> deleteScheduleSlot(Long orgId, Long slotId) {
        ScheduleSlot existing = scheduleSlotMapper.findById(slotId);
        if (existing == null) {
            return ApiResponse.error(404, "时段不存在");
        }
        if (existing.getOrgId() == null || !existing.getOrgId().equals(orgId)) {
            return ApiResponse.error(403, "无权操作此时段");
        }
        scheduleSlotMapper.deleteByIdAndOrgId(slotId, orgId);
        return ApiResponse.success("时段已删除");
    }

    /**
     * 用户 - 根据申请ID获取可用时段（需要先通过申请获取机构ID）
     */
    public ApiResponse<List<ScheduleSlot>> getAvailableSlotsByAppId(Long appId) {
        // 通过 adoption-service 获取申请的机构ID
        try {
            ApiResponse<Object> appResponse = adoptionServiceClient.getApplicationDetail(appId);
            if (appResponse == null || appResponse.getCode() != 200) {
                return ApiResponse.error(404, "申请不存在");
            }
            // 从响应中提取 orgId
            Object appData = appResponse.getData();
            if (appData instanceof Map) {
                Map<?, ?> appMap = (Map<?, ?>) appData;
                Object orgIdObj = appMap.get("orgId");
                if (orgIdObj == null) {
                    return ApiResponse.error(400, "申请信息不完整");
                }
                Long orgId = Long.valueOf(orgIdObj.toString());
                List<ScheduleSlot> slots = scheduleSlotMapper.findByOrgId(orgId);
                return ApiResponse.success(slots);
            }
            return ApiResponse.error(400, "无法解析申请信息");
        } catch (Exception e) {
            return ApiResponse.error(500, "获取可用时段失败: " + e.getMessage());
        }
    }
}

