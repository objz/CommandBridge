package dev.objz.commandbridge.velocity.ui;

import net.kyori.adventure.text.Component;

public interface UIComponent {
    /**
     * Renders the component for the Minecraft Chat
     */
    Component renderChat(RenderContext ctx);

    /**
     * Renders the component for the Console/Terminal
     */
    String renderConsole(RenderContext ctx);
}
