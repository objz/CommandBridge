package dev.objz.commandbridge.main.scripting;

public final class ScriptTypes {
	private ScriptTypes() {
	}

	public enum ScriptKind {
		VELOCITY_TO_BACKEND,
		BACKEND_TO_VELOCITY,
		BACKEND_TO_BACKEND;

		public enum Side {
			VELOCITY, BACKEND
		}

		public boolean registersOn(Side side) {
			return switch (this) {
				case VELOCITY_TO_BACKEND -> side == Side.VELOCITY;
				case BACKEND_TO_VELOCITY, BACKEND_TO_BACKEND -> side == Side.BACKEND;
			};
		}
	}

	public enum ExecutorMode {
		PLAYER, CONSOLE, OP_PLAYER
	}

	public enum TargetMode {
		PLAYERS_SERVER, FIXED, BY_ARG
	}

	public enum ArgType {
		PLAYER, NUMBER, SERVER, WORD, TEXT, UUID
	}
}
