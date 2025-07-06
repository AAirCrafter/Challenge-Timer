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
    private static final Gson GSON = new Gson();
    private static final Type LIST_TYPE = new TypeToken<List<TimerData>>(){}.getType();
    private static final String FILE_NAME = "timers.json";

    private static List<TimerData> timers = new ArrayList<>();
    private static TimerSettings settings = new TimerSettings(); // NEU

    public static List<TimerData> getTimers() {
        return timers;
    }

    public static TimerSettings getSettings() {
        return settings;
    }

    public static void setPrefix(MinecraftServer server, String prefix) {
        settings.prefix = prefix;
        save(server);
    }

    public static void setSuffix(MinecraftServer server, String suffix) {
        settings.suffix = suffix;
        save(server);
    }

    public static boolean setDefaultColor(MinecraftServer server, String color) {
        settings.defaultColor = color;
        save(server);
        return true;
    }

    public static void setFinishedMessage(MinecraftServer server, String message) {
        settings.finishedText = message;
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
        if (Types.getFormattingMap().containsKey(formatting)) {
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
        try {
            File file = getFile(server);
            if (file.exists()) {

                FileReader reader = new FileReader(file);
                JsonObject obj = GSON.fromJson(reader, JsonObject.class);

                timers = GSON.fromJson(obj.get("timers"), LIST_TYPE);
                settings = GSON.fromJson(obj.get("settings"), TimerSettings.class);

                reader.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static File getFile(MinecraftServer server) {
        File worldDir = server.getSavePath(WorldSavePath.ROOT).toFile();
        return new File(worldDir, FILE_NAME);
    }

    public static void save(MinecraftServer server) {
        try {
            File file = getFile(server);
            file.getParentFile().mkdirs();
            FileWriter writer = new FileWriter(file);

            JsonObject obj = new JsonObject();
            obj.add("timers", GSON.toJsonTree(timers));
            obj.add("settings", GSON.toJsonTree(settings));

            GSON.toJson(obj, writer);
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
