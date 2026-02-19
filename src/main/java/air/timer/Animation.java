package air.timer;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.serialization.DataResult;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;

import java.util.Optional;

public class Animation {
    static int setAnimationTime(CommandContext<ServerCommandSource> context) {
        int time = IntegerArgumentType.getInteger(context,"animspeed");
        MinecraftServer server = context.getSource().getServer();

        if (time >= -100 && time <= 100 && time != 0) {
            TimerStorage.setAnimationTime(server,time);
            context.getSource().sendFeedback(() -> Text.literal("Set AnimationTime to "+time).formatted(Formatting.GREEN),false);
        } else if (time == 0) {
            context.getSource().sendFeedback(() -> Text.literal("Please choose a static Animation instad of typing 0").formatted(Formatting.RED),false);
        } else {
            context.getSource().sendFeedback(() -> Text.literal("Please enter a valid number between -100 and 100").formatted(Formatting.RED),false);
        }

        return 1;
    }

    static int setAnimatedGradient(CommandContext<ServerCommandSource> context) {
        String name = StringArgumentType.getString(context, "name");
        String color1 = StringArgumentType.getString(context, "color1").toLowerCase();
        String color2 = StringArgumentType.getString(context, "color2").toLowerCase();
        MinecraftServer server = context.getSource().getServer();

        boolean exists = TimerStorage.getTimers().stream()
                .anyMatch(timer -> timer.name.equals(name));

        if (!exists) {
            context.getSource().sendFeedback(() -> Text.literal("Timer not found!").formatted(Formatting.RED), false);
            return 0;
        }

        boolean isFirstColorValid = false;
        boolean isSecondColorValid = false;

        if (color1.startsWith("#") && color1.length() == 7) {
            try {
                Integer.parseInt(color1.substring(1), 16);
                isFirstColorValid = true;
            } catch (NumberFormatException ignored) {
            }
        }

        if (color2.startsWith("#") && color2.length() == 7) {
            try {
                Integer.parseInt(color2.substring(1), 16);
                isSecondColorValid = true;
            } catch (NumberFormatException ignored) {
            }
        }

        if (!isFirstColorValid || !isSecondColorValid) {
            context.getSource().sendFeedback(() -> Text.literal("Invalid Hex Code! Use a valid hex code like '#RRGGBB'.").formatted(Formatting.RED), false);
            return 0;
        }

        String color = color1 + "//" + color2;

        boolean colorset = TimerStorage.setColor(server, name, color);

        Text nameText;

        DataResult<TextColor> parsed = TextColor.parse(color1);
        DataResult<TextColor> parsed2 = TextColor.parse(color2);
        Optional<TextColor> optional = parsed.result();
        Optional<TextColor> optional2 = parsed2.result();

        if (optional.isPresent()) {
            nameText = Text.literal(name).setStyle(Style.EMPTY.withColor(optional.get()));
        } else {
            context.getSource().sendFeedback(() ->
                    Text.literal("Invalid hex color: " + color1).formatted(Formatting.RED), false);
            return 0;
        }

        Text nameTextGradient = Renderer.generateGradientText(name,color1,color2);

        if (optional2.isPresent()) {
            nameText = Text.literal(name).setStyle(Style.EMPTY.withColor(optional.get()));
        } else {
            context.getSource().sendFeedback(() ->
                    Text.literal("Invalid hex color: " + color2).formatted(Formatting.RED), false);
            return 0;
        }

        if (colorset) {
            Text fullText = Text.literal("Recolored ").append(nameTextGradient);
            context.getSource().sendFeedback(() -> fullText, false);
        }

        return 1;
    };

    static int setStaticColorGradient(CommandContext<ServerCommandSource> context) {
        String name = StringArgumentType.getString(context, "name");
        String color1 = StringArgumentType.getString(context, "color1").toLowerCase();
        String color2 = StringArgumentType.getString(context, "color2").toLowerCase();
        MinecraftServer server = context.getSource().getServer();

        boolean exists = TimerStorage.getTimers().stream()
                .anyMatch(timer -> timer.name.equals(name));

        if (!exists) {
            context.getSource().sendFeedback(() -> Text.literal("Timer not found!").formatted(Formatting.RED), false);
            return 0;
        }

        boolean isFirstColorValid = false;
        boolean isSecondColorValid = false;

        if (color1.startsWith("#") && color1.length() == 7) {
            try {
                Integer.parseInt(color1.substring(1), 16);
                isFirstColorValid = true;
            } catch (NumberFormatException ignored) {
            }
        }

        if (color2.startsWith("#") && color2.length() == 7) {
            try {
                Integer.parseInt(color2.substring(1), 16);
                isSecondColorValid = true;
            } catch (NumberFormatException ignored) {
            }
        }

        if (!isFirstColorValid || !isSecondColorValid) {
            context.getSource().sendFeedback(() -> Text.literal("Invalid Hex Code! Use a valid hex code like '#RRGGBB'.").formatted(Formatting.RED), false);
            return 0;
        }

        String color = color1 + "||" + color2;

        boolean colorset = TimerStorage.setColor(server, name, color);

        Text nameText;

        DataResult<TextColor> parsed = TextColor.parse(color1);
        DataResult<TextColor> parsed2 = TextColor.parse(color2);
        Optional<TextColor> optional = parsed.result();
        Optional<TextColor> optional2 = parsed2.result();

        if (optional.isPresent()) {
            nameText = Text.literal(name).setStyle(Style.EMPTY.withColor(optional.get()));
        } else {
            context.getSource().sendFeedback(() ->
                    Text.literal("Invalid hex color: " + color1).formatted(Formatting.RED), false);
            return 0;
        }

        Text nameTextGradient = Renderer.generateGradientText(name,color1,color2);

        if (optional2.isPresent()) {
            nameText = Text.literal(name).setStyle(Style.EMPTY.withColor(optional.get()));
        } else {
            context.getSource().sendFeedback(() ->
                    Text.literal("Invalid hex color: " + color2).formatted(Formatting.RED), false);
            return 0;
        }

        if (colorset) {
            Text fullText = Text.literal("Recolored ").append(nameTextGradient);
            context.getSource().sendFeedback(() -> fullText, false);
        }

        return 1;
    }
}
