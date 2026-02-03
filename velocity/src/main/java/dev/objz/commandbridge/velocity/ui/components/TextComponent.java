package dev.objz.commandbridge.velocity.ui.components;

import dev.objz.commandbridge.util.MM;
import dev.objz.commandbridge.velocity.ui.RenderContext;
import dev.objz.commandbridge.velocity.ui.UIComponent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class TextComponent implements UIComponent {

    private final String mmContent;
    private final String ansiContent;

    public TextComponent(String mmContent) {
        this.mmContent = mmContent;
        this.ansiContent = null;
    }
    
    public TextComponent(String mmContent, String ansiContent) {
        this.mmContent = mmContent;
        this.ansiContent = ansiContent;
    }

    @Override
    public Component renderChat(RenderContext ctx) {
        return MM.parse(mmContent);
    }

    @Override
    public String renderConsole(RenderContext ctx) {
        if (ansiContent != null) return ansiContent;
        return MiniMessage.miniMessage().stripTags(mmContent);
    }
}
