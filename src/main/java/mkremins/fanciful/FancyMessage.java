package mkremins.fanciful;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public final class FancyMessage {

    private final List<Segment> segments = new ArrayList<>();

    private Segment currentSegment;

    public FancyMessage(String text) {
        currentSegment = new Segment(text);
        segments.add(currentSegment);
    }

    public FancyMessage then(String text) {
        currentSegment = new Segment(text);
        segments.add(currentSegment);
        return this;
    }

    public FancyMessage color(ChatColor chatColor) {
        if (chatColor == null) {
            return this;
        }

        if (chatColor == ChatColor.RESET) {
            currentSegment.clearFormatting();
            return this;
        }

        TextColor textColor = toTextColor(chatColor);
        if (textColor != null) {
            currentSegment.color = textColor;
            return this;
        }

        TextDecoration decoration = toDecoration(chatColor);
        if (decoration != null) {
            currentSegment.decorations.add(decoration);
        }
        return this;
    }

    public FancyMessage style(ChatColor... chatColors) {
        if (chatColors == null) {
            return this;
        }

        for (ChatColor chatColor : chatColors) {
            if (chatColor == null) {
                continue;
            }

            if (chatColor == ChatColor.RESET) {
                currentSegment.clearFormatting();
                continue;
            }

            TextDecoration decoration = toDecoration(chatColor);
            if (decoration != null) {
                currentSegment.decorations.add(decoration);
                continue;
            }

            TextColor textColor = toTextColor(chatColor);
            if (textColor != null) {
                currentSegment.color = textColor;
            }
        }
        return this;
    }

    public FancyMessage command(String command) {
        currentSegment.clickEvent = ClickEvent.runCommand(command);
        return this;
    }

    public FancyMessage formattedTooltip(FancyMessage... messages) {
        currentSegment.hoverEvent = HoverEvent.showText(joinMessages(messages));
        return this;
    }

    public void send(CommandSender sender) {
        sender.sendMessage(asComponent());
    }

    private Component asComponent() {
        TextComponent.Builder builder = Component.text();
        for (Segment segment : segments) {
            builder.append(segment.asComponent());
        }
        return builder.build();
    }

    private static Component joinMessages(FancyMessage... messages) {
        TextComponent.Builder builder = Component.text();
        if (messages == null) {
            return builder.build();
        }

        for (int i = 0; i < messages.length; i++) {
            if (i > 0) {
                builder.append(Component.newline());
            }
            builder.append(messages[i].asComponent());
        }
        return builder.build();
    }

    private static TextColor toTextColor(ChatColor chatColor) {
        return switch (chatColor) {
            case BLACK -> NamedTextColor.BLACK;
            case DARK_BLUE -> NamedTextColor.DARK_BLUE;
            case DARK_GREEN -> NamedTextColor.DARK_GREEN;
            case DARK_AQUA -> NamedTextColor.DARK_AQUA;
            case DARK_RED -> NamedTextColor.DARK_RED;
            case DARK_PURPLE -> NamedTextColor.DARK_PURPLE;
            case GOLD -> NamedTextColor.GOLD;
            case GRAY -> NamedTextColor.GRAY;
            case DARK_GRAY -> NamedTextColor.DARK_GRAY;
            case BLUE -> NamedTextColor.BLUE;
            case GREEN -> NamedTextColor.GREEN;
            case AQUA -> NamedTextColor.AQUA;
            case RED -> NamedTextColor.RED;
            case LIGHT_PURPLE -> NamedTextColor.LIGHT_PURPLE;
            case YELLOW -> NamedTextColor.YELLOW;
            case WHITE -> NamedTextColor.WHITE;
            default -> null;
        };
    }

    private static TextDecoration toDecoration(ChatColor chatColor) {
        return switch (chatColor) {
            case MAGIC -> TextDecoration.OBFUSCATED;
            case BOLD -> TextDecoration.BOLD;
            case STRIKETHROUGH -> TextDecoration.STRIKETHROUGH;
            case UNDERLINE -> TextDecoration.UNDERLINED;
            case ITALIC -> TextDecoration.ITALIC;
            default -> null;
        };
    }

    private static final class Segment {

        private final String text;

        private final EnumSet<TextDecoration> decorations = EnumSet.noneOf(TextDecoration.class);

        private TextColor color;

        private ClickEvent clickEvent;

        private HoverEvent<Component> hoverEvent;

        private Segment(String text) {
            this.text = text;
        }

        private void clearFormatting() {
            color = null;
            decorations.clear();
            clickEvent = null;
            hoverEvent = null;
        }

        private Component asComponent() {
            TextComponent.Builder builder = Component.text().content(text);
            if (color != null) {
                builder.color(color);
            }
            for (TextDecoration decoration : decorations) {
                builder.decoration(decoration, true);
            }
            if (clickEvent != null) {
                builder.clickEvent(clickEvent);
            }
            if (hoverEvent != null) {
                builder.hoverEvent(hoverEvent);
            }
            return builder.build();
        }
    }
}
