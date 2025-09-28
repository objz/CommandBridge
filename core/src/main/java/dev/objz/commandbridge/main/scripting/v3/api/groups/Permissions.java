package dev.objz.commandbridge.main.scripting.v3.api.groups;

import dev.objz.commandbridge.main.scripting.v3.api.Meta;
import dev.objz.commandbridge.main.scripting.v3.api.constants.PermissionsConstants;

public record Permissions(
		Meta<Boolean> enabled,
		Meta<Boolean> silent) {
	public static Permissions withDefaults() {
		return new Permissions(
				Meta.isOptional(PermissionsConstants.ENABLED),
				Meta.isOptional(PermissionsConstants.SILENT));
	}

}
