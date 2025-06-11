package air.dev;

import com.mojang.serialization.DataResult;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Types {

    public static Map<String, String> getColorMap() {
        Map<String, String> colors = new HashMap<>();
        colors.put("white", "§f");
        colors.put("black", "§0");
        colors.put("dark_gray", "§8");
        colors.put("gray", "§7");
        colors.put("dark_green", "§2");
        colors.put("dark_blue", "§1");
        colors.put("dark_aqua", "§3");
        colors.put("dark_purple", "§5");
        colors.put("aqua", "§b");
        colors.put("light_purple", "§d");
        colors.put("yellow", "§e");
        colors.put("blue", "§9");
        colors.put("green", "§a");
        colors.put("red", "§c");
        colors.put("orange", "§6");
        return colors;
    }

    public static Map<String, String> getFormattingMap() {
        Map<String, String> formattings = new HashMap<>();
        formattings.put("bold","§l");
        formattings.put("italic","§o");
        formattings.put("underlined","§n");
        formattings.put("strikedthrough","§m");
        formattings.put("obfuscated","§k");
        return formattings;
    }

    public static ArrayList<String> getColorNames() {
        return new ArrayList<>(getColorMap().keySet());
    }

    public static ArrayList<String> getFormattingNames() {
        return new ArrayList<>(getFormattingMap().keySet());
    }


    public static void sendActionBar(ServerPlayerEntity player) {
        List<TimerData> timers = TimerStorage.getTimers();
        if (!timers.isEmpty()) {
            TimerData activeTimer = null;
            for (TimerData timer : timers) {
                if (timer.active) {
                    activeTimer = timer;
                    break;
                }
            }
            if (activeTimer == null) {
                activeTimer = timers.getLast();
            }

            TimerSettings settings = TimerStorage.getSettings();
            String prefix = settings.prefix != null ? settings.prefix : "";
            String suffix = settings.suffix != null ? settings.suffix : "";

            String defaultColorStr = settings.defaultColor != null ? settings.defaultColor : "§f";

            DataResult<TextColor> defaultColorParse = TextColor.parse(defaultColorStr);
            TextColor defaultColor = defaultColorParse.result().orElse(TextColor.fromFormatting(Formatting.WHITE));

            DataResult<TextColor> timerColorParse = TextColor.parse(activeTimer.color);
            TextColor timerColor = timerColorParse.result().orElse(TextColor.fromFormatting(Formatting.WHITE));

            Text prefixText = Text.literal(prefix).setStyle(Style.EMPTY.withColor(defaultColor));
            Text suffixText = Text.literal(suffix).setStyle(Style.EMPTY.withColor(defaultColor));

            StringBuilder timerTextString;
            if (activeTimer.time >= 0) {
                timerTextString = new StringBuilder(CalTimer.toTime(activeTimer.time));
            } else {
                timerTextString = new StringBuilder(settings.finishedText != null ? settings.finishedText : "{name} over");
                timerTextString = new StringBuilder(timerTextString.toString().replace("{name}", activeTimer.name));
            }




            Map<String, Boolean> formattingOptions = Map.of(
                    "bold", activeTimer.bold,
                    "italic", activeTimer.italic,
                    "obfuscated", activeTimer.obfuscated,
                    "underlined", activeTimer.underlined,
                    "strikedthrough", activeTimer.strikedthrough
            );

            for (Map.Entry<String, Boolean> entry : formattingOptions.entrySet()) {
                if (entry.getValue()) {
                    timerTextString.insert(0, getFormattingMap().get(entry.getKey()));
                }
            }
            Text timerText = Text.literal(timerTextString.toString()).setStyle(Style.EMPTY.withColor(timerColor));

            Text display = Text.empty()
                    .append(prefixText)
                    .append(timerText)
                    .append(suffixText);

            player.networkHandler.sendPacket(new GameMessageS2CPacket(display, true));
        }
    }
}