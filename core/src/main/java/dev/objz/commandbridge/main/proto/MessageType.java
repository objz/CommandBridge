package dev.objz.commandbridge.main.proto;

public enum MessageType {
	AUTH, AUTH_OK, AUTH_FAIL, ERROR, PING, PONG,
	REGISTER_COMMANDS, // Velocity → Backend
	COMMAND_INVOKED, // Backend → Velocity
	EXECUTE_COMMAND, // Velocity → Backend
	FEEDBACK, 
}
