package dev.objz.commandbridge.net.proto;

public enum MessageType {
	AUTH_REQUEST, AUTH_OK, AUTH_FAIL, 
	REGISTER_COMMANDS,
	REGISTER_COMMANDS_RESULT,
	INVOKED_COMMAND,
	EXECUTE_COMMAND,
	PING, PONG
}
