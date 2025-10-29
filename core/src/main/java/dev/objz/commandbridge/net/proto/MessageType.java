package dev.objz.commandbridge.net.proto;

public enum MessageType {
	AUTH_REQUEST, AUTH_OK, AUTH_FAIL, 
	REGISTER_COMMANDS,
	INVOKED_COMMAND,
	FEEDBACK, 
}
