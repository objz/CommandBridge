package dev.objz.commandbridge.velocity.cmd.bridge.packetevents;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.chat.Node;
import com.github.retrooper.packetevents.protocol.chat.Parsers;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.resources.ResourceLocation;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDeclareCommands;
import dev.objz.commandbridge.velocity.cmd.bridge.framework.CommandOverride;
import dev.objz.commandbridge.velocity.cmd.bridge.framework.CustomArgumentRegistry;
import dev.objz.commandbridge.velocity.cmd.bridge.framework.PacketArgumentSpec;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class DeclareCommandsPatchListener implements PacketListener {
	private final CustomArgumentRegistry argumentRegistry;

	public DeclareCommandsPatchListener(CustomArgumentRegistry argumentRegistry) {
		this.argumentRegistry = Objects.requireNonNull(argumentRegistry);
	}

	@Override
	public void onPacketSend(PacketSendEvent event) {
		if (event.getPacketType() != PacketType.Play.Server.DECLARE_COMMANDS) {
			return;
		}

		WrapperPlayServerDeclareCommands wrapper = new WrapperPlayServerDeclareCommands(event);
		List<Node> nodes = wrapper.getNodes();
		if (nodes == null || nodes.isEmpty()) {
			return;
		}

		int rootIndex = wrapper.getRootIndex();
		if (rootIndex < 0 || rootIndex >= nodes.size()) {
			return;
		}

		Node rootNode = nodes.get(rootIndex);
		List<Integer> rootChildren = rootNode.getChildren();
		if (rootChildren == null || rootChildren.isEmpty()) {
			return;
		}

		boolean changed = false;
		for (int childIndex : rootChildren) {
			if (childIndex < 0 || childIndex >= nodes.size()) {
				continue;
			}
			Node child = nodes.get(childIndex);
			if (!isLiteral(child)) {
				continue;
			}

			String commandName = child.getName().orElse(null);
			if (commandName == null) {
				continue;
			}

			Optional<CommandOverride> override = argumentRegistry.getOverride(commandName);
			if (override.isEmpty() || override.get().isEmpty()) {
				continue;
			}

			if (patchCommandTree(nodes, childIndex, override.get())) {
				changed = true;
			}
		}

		if (changed) {
			event.markForReEncode(true);
		}
	}

	private boolean patchCommandTree(List<Node> nodes, int startIndex, CommandOverride override) {
		Set<Integer> visited = new HashSet<>();
		Deque<Integer> stack = new ArrayDeque<>();
		stack.push(startIndex);
		boolean changed = false;

		while (!stack.isEmpty()) {
			int index = stack.pop();
			if (index < 0 || index >= nodes.size() || !visited.add(index)) {
				continue;
			}

			Node node = nodes.get(index);
			if (isArgument(node)) {
				String argumentName = node.getName().orElse(null);
				if (argumentName != null) {
					PacketArgumentSpec spec = override.argumentSpec(argumentName);
					if (spec != null) {
						applySpec(node, spec);
						changed = true;
					}
				}
			}

			List<Integer> children = node.getChildren();
			if (children != null) {
				for (int childIndex : children) {
					stack.push(childIndex);
				}
			}

			if ((node.getFlags() & Node.FLAG_REDIRECT) != 0) {
				stack.push(node.getRedirectNodeIndex());
			}
		}

		return changed;
	}

	private static void applySpec(Node node, PacketArgumentSpec spec) {
		Parsers.Parser parser = Parsers.getByName(spec.parserKey());
		if (parser == null) {
			return;
		}
		node.setParser(Optional.of(parser));
		node.setProperties(spec.properties());
		spec.suggestionsTypeKey().ifPresent(suggestionsType -> {
			node.setSuggestionsType(Optional.of(new ResourceLocation(suggestionsType)));
			node.setFlags((byte) (node.getFlags() | Node.FLAG_CUSTOM_SUGGESTIONS));
		});
	}

	private static boolean isLiteral(Node node) {
		return (node.getFlags() & Node.TYPE_MASK) == Node.TYPE_LITERAL;
	}

	private static boolean isArgument(Node node) {
		return (node.getFlags() & Node.TYPE_MASK) == Node.TYPE_ARGUMENT;
	}
}
