package dev.objz.commandbridge.net.payloads.util;

import java.util.UUID;

public record SchedulePayload(
        UUID player,
        UUID scheduleId) {

}
