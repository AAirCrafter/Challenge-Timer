package air.timer;

import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;

import java.util.*;

public class Renderer {
    public static Map<String, Formatting> getColorMap() {
        Map<String, Formatting> colors = new HashMap<>();
        colors.put("white", Formatting.WHITE);
        colors.put("black", Formatting.BLACK);
        colors.put("dark_gray", Formatting.DARK_GRAY);
        colors.put("gray", Formatting.GRAY);
        colors.put("dark_green", Formatting.DARK_GREEN);
        colors.put("dark_blue", Formatting.DARK_BLUE);
        colors.put("dark_aqua", Formatting.DARK_AQUA);
        colors.put("dark_purple", Formatting.DARK_PURPLE);
        colors.put("aqua", Formatting.AQUA);
        colors.put("light_purple", Formatting.LIGHT_PURPLE);
        colors.put("yellow", Formatting.YELLOW);
        colors.put("blue", Formatting.BLUE);
        colors.put("green", Formatting.GREEN);
        colors.put("gold", Formatting.GOLD);
        colors.put("red", Formatting.RED);
        return colors;
    }

    public static Map<String, Formatting> getFormattingMap() {
        Map<String, Formatting> formattings = new HashMap<>();
        formattings.put("bold", Formatting.BOLD);
        formattings.put("italic", Formatting.ITALIC);
        formattings.put("underlined", Formatting.UNDERLINE);
        formattings.put("strikedthrough", Formatting.STRIKETHROUGH);
        formattings.put("obfuscated", Formatting.OBFUSCATED);
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
        int length = Math.max(1, text.length());

        for (int i = 0; i < text.length(); i++) {
            float ratio = (float) i / (length - 1);
            int r = interpolate(startRGB[0], endRGB[0], ratio);
            int g = interpolate(startRGB[1], endRGB[1], ratio);
            int b = interpolate(startRGB[2], endRGB[2], ratio);

            int color = (r << 16) | (g << 8) | b;
            TextColor textColor = TextColor.fromRgb(color);
            Text letter = Text.literal(String.valueOf(text.charAt(i))).setStyle(Style.EMPTY.withColor(textColor));

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
            float phase = (i + direction * animationStep) / animtime;
            float ratio = (float) (Math.sin(phase) * 0.5 + 0.5);
            int r = interpolate(startRGB[0], endRGB[0], ratio);
            int g = interpolate(startRGB[1], endRGB[1], ratio);
            int b = interpolate(startRGB[2], endRGB[2], ratio);

            int color = (r << 16) | (g << 8) | b;
            TextColor textColor = TextColor.fromRgb(color);
            Style styled = Style.EMPTY.withColor(textColor);

            result.append(Text.literal(String.valueOf(text.charAt(i))).setStyle(styled));
        }
        return result;
    }

    public static int[] hexToRGB(String hex) {
        hex = hex.startsWith("#") ? hex.substring(1) : hex;
        int r = Integer.parseInt(hex.substring(0, 2), 16);
        int g = Integer.parseInt(hex.substring(2, 4), 16);
        int b = Integer.parseInt(hex.substring(4, 6), 16);
        return new int[]{r, g, b};
    }

    private static int interpolate(int start, int end, float ratio) {
        return Math.round(start + (end - start) * ratio);
    }

    private static Text buildTimerText(TimerData timer, String content) {
        if (timer.color.matches("^#[0-9a-fA-F]{6}\\|\\|#[0-9a-fA-F]{6}$")) {
            String[] parts = timer.color.split("\\|\\|");
            return generateGradientText(content, parts[0], parts[1]);
        } else if (timer.color.matches("^#[0-9a-fA-F]{6}//#[0-9a-fA-F]{6}$")) {
            String[] parts = timer.color.split("//");
            return genereateAnimatedGradientText(content, parts[0], parts[1]);
        } else if (timer.color.matches("^#[0-9a-fA-F]{6}$")) {
            int[] rgb = hexToRGB(timer.color);
            int color = (rgb[0] << 16) | (rgb[1] << 8) | rgb[2];
            return Text.literal(content).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(color)));
        } else {
            Formatting formatting = getColorMap().getOrDefault(timer.color.toLowerCase(), Formatting.WHITE);
            return Text.literal(content).formatted(formatting);
        }
    }

    public static void sendActionBar(ServerPlayerEntity player) {
        List<TimerData> timers = TimerStorage.getTimers();
        if (timers.isEmpty()) return;

        TimerData activeTimer = null;
        for (TimerData timer : timers) {
            if (timer.active) {
                activeTimer = timer;
                break;
            }
        }
        
        if (activeTimer == null) return;

        TimerSettings settings = TimerStorage.getSettings();
        String prefix = settings.defaultPrefix != null ? settings.defaultPrefix : "";
        String suffix = settings.defaultSuffix != null ? settings.defaultSuffix : "";
        String defaultColorStr = settings.defaultColor != null ? settings.defaultColor : "white";

        Formatting defaultFormatting = getColorMap().getOrDefault(defaultColorStr.toLowerCase(), Formatting.WHITE);
        MutableText prefixText = Text.literal(prefix).formatted(defaultFormatting);
        Text suffixText = Text.literal(suffix).formatted(defaultFormatting);

        Map<String, Boolean> formattingOptions = Map.of(
                "bold", activeTimer.bold,
                "italic", activeTimer.italic,
                "obfuscated", activeTimer.obfuscated,
                "underlined", activeTimer.underlined,
                "strikedthrough", activeTimer.strikedthrough
        );

        String time = CalTimer.toTime(activeTimer.time);
        String timerContent;

        if (activeTimer.time < 0) {
            timerContent = Optional.ofNullable(settings.finishedText)
                    .orElse("{name} over")
                    .replace("{name}", activeTimer.name);
        } else if (TimerStorage.areTimersPaused()) {
            timerContent = Optional.ofNullable(settings.pausedText)
                    .orElse("{name} paused - {time}")
                    .replace("{name}", activeTimer.name)
                    .replace("{time}", time);
        } else {
            timerContent = time;
        }

        boolean timerFormattingsUsed = TimerStorage.isTimerFormattingsUsed();

        Text timerText = timerFormattingsUsed
                ? buildTimerText(activeTimer, prefix + timerContent + suffix)
                : prefixText.append(buildTimerText(activeTimer, timerContent)).append(suffixText);

        for (Text text : (timerFormattingsUsed ? timerText.getSiblings() : ((MutableText) timerText).getSiblings())) {
            if (!timerFormattingsUsed && !(text.getString().contains(timerContent))) continue; // Nur Timerteil
            if (text instanceof MutableText m) {
                Style style = m.getStyle();
                for (var e : formattingOptions.entrySet()) {
                    if (e.getValue()) style = style.withFormatting(getFormattingMap().get(e.getKey()));
                }
                m.setStyle(style);
            }
        }

        animationStep++;

        player.networkHandler.sendPacket(new GameMessageS2CPacket(timerText, true));
    }
}
