package com.adoption.org.event;

/**
 * 事件类型
 * 后续用于 MQ Topic 分发
 */
public enum OrgEvent {
    ORG_APPLIED,
    ORG_APPROVED,
    ORG_REJECTED
}
