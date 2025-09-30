package dev.objz.commandbridge.main.scripting.v3.model.raw;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

import java.time.Duration;

@ConfigSerializable
public final class TargetServerRaw {
    @Setting("target-required") public Boolean targetRequired;
    @Setting("schedule-online") public Boolean scheduleOnline;
    @Setting("timeout")         public Duration timeout;
    @Setting("frequency")       public Duration frequency;
}
