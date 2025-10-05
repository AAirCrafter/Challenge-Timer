package air.dev;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.WorldSavePath;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TimerStorage {
    private static final Gson gson = new Gson();
    private static final Type listType = new TypeToken<List<TimerData>>(){}.getType();
    private static final String fileName = "timers.json";

    private static List<TimerData> timers = new ArrayList<>();
    private static TimerSettings settings = new TimerSettings(); // NEU

    public static List<TimerData> getTimers() {
        return timers;
    }

    public static TimerSettings getSettings() {
        return settings;
    }

    public static void setAnimationTime(MinecraftServer server, int time) {
        settings.animationTime = time;
        save(server);
    }

    public static boolean toggleUseTimerFormattings(MinecraftServer server) {
        settings.useTimerFormattings = !settings.useTimerFormattings;
        save(server);
        return settings.useTimerFormattings;
    }

    public static boolean toggleTimersEnabled(MinecraftServer server) {
        settings.timersEnabled = !settings.timersEnabled;
        save(server);
        return settings.timersEnabled;
    }

    public static boolean areTimersEnabled() {
        return settings.timersEnabled;
    }

    public static boolean toggleTimersPaused(MinecraftServer server) {
        settings.timersPaused = !settings.timersPaused;
        save(server);
        return settings.timersPaused;
    }

    public static boolean areTimersPaused() {
        return settings.timersPaused;
    }

    public static boolean isTimerFormattingsUsed() {
        return settings.useTimerFormattings;
    }

    public static void setDefaultPrefix(MinecraftServer server, String prefix) {
        settings.defaultPrefix = prefix;
        save(server);
    }

    public static void setDefaultSuffix(MinecraftServer server, String suffix) {
        settings.defaultSuffix = suffix;
        save(server);
    }

    public static boolean setDefaultColor(MinecraftServer server, String color) {
        settings.defaultColor = color;
        save(server);
        return true;
    }

    public static void setFinishedText(MinecraftServer server, String text) {
        settings.finishedText = text;
        save(server);
    }

    public static void setPausedText(MinecraftServer server, String text) {
        settings.pausedText = text;
        save(server);
    }

    public static void addTimer(MinecraftServer server, String name, String type, int time) {
        timers.add(new TimerData(name, time, settings.defaultColor, type, false));
        save(server);
    }

    public static void startTimer(MinecraftServer server, String name) {
        for (TimerData timer : timers) {
            timer.active = timer.name.equals(name);
        }
    }

    public static void resetTimer(MinecraftServer server, String name) {
        for (TimerData timer : timers) {
            if (timer.name.equals(name)) {
                timer.time = timer.defaultTime;
            }
        }
    }

    public static void setActiveTimer(MinecraftServer server, String name) {
        for (TimerData timer : timers) timer.active = false;
        for (TimerData timer : timers) {
            if (timer.name.equals(name)) {
                timer.active = true;
                save(server);
                break;
            }
        }
    }

    public static void setTime(MinecraftServer server, String name, int time) {
        for (TimerData timer : timers) {
            if (timer.name.equals(name)) {
                timer.time = time;
                timer.defaultTime = time;
                save(server);
                return;
            }
        }
    }

    public static boolean setFormattings(MinecraftServer server, String name, String formatting) {
        if (Renderer.getFormattingMap().containsKey(formatting)) {
            for (TimerData timer : timers) {
                if (timer.name.equals(name)) {
                    boolean wasActive = switch (formatting) {
                        case "bold" -> timer.bold = !timer.bold;
                        case "italic" -> timer.italic = !timer.italic;
                        case "obfuscated" -> timer.obfuscated = !timer.obfuscated;
                        case "underlined" -> timer.underlined = !timer.underlined;
                        case "strikedthrough" -> timer.strikedthrough = !timer.strikedthrough;
                        default -> false;
                    };
                    save(server);
                    return wasActive;
                }
            }
        }
        return false;
    }

    public static boolean setType(MinecraftServer server, String name, String type) {
        for (TimerData timer : timers) {
            if (timer.name.equals(name)) {
                timer.type = type;
                save(server);
                return true;
            }
        }
        return false;
    }

    public static boolean setColor(MinecraftServer server, String name, String color) {
        for (TimerData timer : timers) {
            if (timer.name.equals(name)) {
                timer.color = color;
                save(server);
                return true;
            }
        }
        return false;
    }

    public static boolean removeTimer(MinecraftServer server, String name) {
        Iterator<TimerData> iterator = timers.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().name.equals(name)) {
                iterator.remove();
                save(server);
                return true;
            }
        }
        return false;
    }

    public static void load(MinecraftServer server) {
        timers.clear();
        try {
            File file = getFile(server);
            if (file.exists()) {

                FileReader reader = new FileReader(file);
                JsonObject obj = gson.fromJson(reader, JsonObject.class);

                timers = gson.fromJson(obj.get("timers"), listType);
                settings = gson.fromJson(obj.get("settings"), TimerSettings.class);

                reader.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static File getFile(MinecraftServer server) {
        File worldDir = server.getSavePath(WorldSavePath.ROOT).toFile();
        return new File(worldDir, fileName);
    }

    public static void save(MinecraftServer server) {
        try {
            File file = getFile(server);
            file.getParentFile().mkdirs();
            FileWriter writer = new FileWriter(file);

            JsonObject obj = new JsonObject();
            obj.add("timers", gson.toJsonTree(timers));
            obj.add("settings", gson.toJsonTree(settings));

            gson.toJson(obj, writer);
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
