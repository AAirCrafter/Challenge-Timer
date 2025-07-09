package air.dev;

import com.mojang.serialization.DataResult;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
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


    public static Text generateGradientText(String text, String color1, String color2) {
        int[] startRGB = hexToRGB(color1);
        int[] endRGB = hexToRGB(color2);

        MutableText result = Text.literal("");
        int length = Math.max(1,text.length());

        for (int i = 0; i < text.length(); i++) {
            float ratio = (float) i / (length - 1);
            int r = interpolate(startRGB[0], endRGB[0],ratio);
            int g = interpolate(startRGB[1], endRGB[1],ratio);
            int b = interpolate(startRGB[2], endRGB[2],ratio);

            int color = (r << 16) | (g << 8) | b;
            TextColor textColor = TextColor.fromRgb(color);
            Text letter = Text.literal(String.valueOf(text.charAt(i)))
                    .setStyle(Style.EMPTY.withColor(textColor));

            result.append(letter);
        }
        return result;
    }
    private static int animationStep = 0;
    public static Text genereateAnimatedGradientText(String text, String color1, String color2) {
        int[] startRGB = hexToRGB(color1);
        int[] endRGB = hexToRGB(color2);
        int rawSpeed = TimerStorage.getSettings().animationTime;
        int animtime = 101 - Math.abs(rawSpeed);
        float direction = rawSpeed >= 0 ? 1 : -1;

        MutableText result = Text.literal("");

        for (int i = 0; i < text.length(); i++) {
            float phase = (float)(i+direction*animationStep) / animtime;
            float ratio = (float)(Math.sin(phase)*0.5+0.5);
            int r = interpolate(startRGB[0], endRGB[0],ratio);
            int g = interpolate(startRGB[1], endRGB[1],ratio);
            int b = interpolate(startRGB[2], endRGB[2],ratio);

            int color = (r << 16) | (g << 8) | b;
            TextColor textColor = TextColor.fromRgb(color);
            Style styled = Style.EMPTY.withColor(textColor);

            result.append(Text.literal(String.valueOf(text.charAt(i))).setStyle(styled));

        }
        return result;
    }

    private static int[] hexToRGB(String hex) {
        hex = hex.startsWith("#") ? hex.substring(1) : hex;
        int r = Integer.valueOf(hex.substring(0, 2), 16);
        int g = Integer.valueOf(hex.substring(2, 4), 16);
        int b = Integer.valueOf(hex.substring(4, 6), 16);
        return new int[]{r, g, b};
    }

    private static int interpolate(int start, int end, float ratio) {
        return Math.round(start + (end - start) * ratio);
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
            if (activeTimer != null) {
                TimerSettings settings = TimerStorage.getSettings();
                String prefix = settings.defaultPrefix != null ? settings.defaultPrefix : "";
                String suffix = settings.defaultSuffix != null ? settings.defaultSuffix : "";

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
                if (activeTimer.color.matches("^#[0-9a-fA-F]{6}\\|\\|#[0-9a-fA-F]{6}$")) {
                    String[] parts = activeTimer.color.split("\\|\\|");
                    timerText = generateGradientText(CalTimer.toTime(activeTimer.time),parts[0],parts[1]);
                } else if (activeTimer.color.matches("^#[0-9a-fA-F]{6}//#[0-9a-fA-F]{6}$")) {
                    String[] parts = activeTimer.color.split("//");
                    timerText = genereateAnimatedGradientText(CalTimer.toTime(activeTimer.time),parts[0],parts[1]);
                }

                animationStep++;

                Text display = Text.empty()
                        .append(prefixText)
                        .append(timerText)
                        .append(suffixText);

                player.networkHandler.sendPacket(new GameMessageS2CPacket(display, true));
            }



        }
    }
}