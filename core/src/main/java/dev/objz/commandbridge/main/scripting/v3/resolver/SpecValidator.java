package dev.objz.commandbridge.main.scripting.v3.resolver;

import dev.objz.commandbridge.main.scripting.v3.enums.ArgType;
import dev.objz.commandbridge.main.scripting.v3.model.*;
import dev.objz.commandbridge.main.scripting.v3.model.args.*;

import java.util.*;

public final class SpecValidator {
	public static final class Errors extends RuntimeException {
		private final String script;
		private final List<String> errors;

		public Errors(String script, List<String> errors) {
			super("invalid script: " + script);
			this.script = script;
			this.errors = errors;
		}

		public List<String> list() {
			return errors;
		}

		public String script() {
			return script;
		}
	}

	public void validate(ScriptSpec s) {
		List<String> e = new ArrayList<>();
		if (s.version() == null)
			e.add("version is required");
		if (s.name() == null || s.name().isBlank())
			e.add("name is required");
		if (s.commands() == null || s.commands().isEmpty())
			e.add("commands is required");

		// args checks
		if (s.args() != null) {
			Set<String> seen = new HashSet<>();
			for (int i = 0; i < s.args().size(); i++) {
				ArgDef a = s.args().get(i);
				if (a.name() == null || a.name().isBlank())
					e.add("args[" + i + "] name is required");
				if (a.type() == null)
					e.add("args[" + i + "] type is required");
				if (a.name() != null && !seen.add(a.name()))
					e.add("duplicate arg name: " + a.name());

				if (a instanceof ChoiceArgDef c) {
					if (c.choices() == null || c.choices().isEmpty())
						e.add("args[" + i + "] CHOICE requires non-empty choices");
				}
				if (a instanceof SimpleArgDef s1) {
					if (s1.type() == ArgType.CHOICE || s1.type() == ArgType.RANGE)
						e.add("args[" + i + "]: use ChoiceArgDef/RangeArgDef for type "
								+ s1.type());
				}
			}
		}

		// steps
		if (s.commands() != null) {
			for (int i = 0; i < s.commands().size(); i++) {
				StepSpec st = s.commands().get(i);
				if (st.command() == null || st.command().isBlank())
					e.add("commands[" + i + "] command is required");
			}
		}

		if (!e.isEmpty())
			throw new Errors(s.name() == null ? "<unnamed>" : s.name(), e);
	}
}
