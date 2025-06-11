package air.dev;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.serialization.DataResult;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;


public class Commands {

    public static Boolean toggleEndisableTimer = true;
    public static Boolean toggleTimerPause = true;

    public static void registerCommands() {
        CommandRegistrationCallback.EVENT.register(((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("timer")
                    .executes(context -> {
                        timer(context);
                        return 1;
                    })
                    .then(CommandManager.literal("add")
                            .then(CommandManager.argument("name", StringArgumentType.string())
                                    .then(CommandManager.argument("time", StringArgumentType.string())
                                            .executes(Commands::addTimer)
                                            .then(CommandManager.literal("Countup")
                                                    .executes(Commands::addStopWatch))
                                            .then(CommandManager.literal("Countdown")
                                                    .executes(Commands::addTimer))
                                    )
                            )
                    )
                    .then(CommandManager.literal("remove")
                            .then(CommandManager.argument("name", StringArgumentType.word())
                                            .suggests((context,builder) -> {

                                                for (TimerData timer : TimerStorage.getTimers()) {
                                                    builder.suggest(timer.name);
                                                }
                                                return builder.buildFuture();
                                            })
                                            .executes(Commands::removeTimer)
                            )
                    )
                    .then(CommandManager.literal("en/disable")
                            .executes(context -> {
                                toggleEndisableTimer = !toggleEndisableTimer;
                                if (toggleEndisableTimer) {
                                    context.getSource().sendFeedback(() -> Text.literal("Timer's enabled!").formatted(Formatting.GOLD), true);
                                } else {
                                    context.getSource().sendFeedback(() -> Text.literal("Timer's disabled!").formatted(Formatting.GOLD), true);
                                }
                                return 1;
                            }))
                    .then(CommandManager.literal("togglePause")
                            .executes(context -> {
                                toggleTimerPause = !toggleTimerPause;
                                if (toggleTimerPause) {
                                    context.getSource().sendFeedback(() -> Text.literal("Timer's unpaused!").formatted(Formatting.GOLD), true);
                                } else {
                                    context.getSource().sendFeedback(() -> Text.literal("Timer's paused!").formatted(Formatting.GOLD), true);
                                }
                                return 1;
                            }))
                    .then(CommandManager.literal("settings")
                            .then(CommandManager.literal("prefix")
                                    .then(CommandManager.argument("value", StringArgumentType.string())
                                            .executes(context -> {
                                                String prefix = StringArgumentType.getString(context, "value");
                                                TimerStorage.setPrefix(context.getSource().getServer(), prefix);
                                                context.getSource().sendFeedback(() -> Text.literal("Prefix set to: " + prefix).formatted(Formatting.GREEN), false);
                                                return 1;
                                            })
                                    )
                            )
                            .then(CommandManager.literal("suffix")
                                    .then(CommandManager.argument("value", StringArgumentType.string()) // nur mit Anführungszeichen!
                                            .executes(context -> {
                                                String suffix = StringArgumentType.getString(context, "value");
                                                TimerStorage.setSuffix(context.getSource().getServer(), suffix);
                                                context.getSource().sendFeedback(() -> Text.literal("Suffix set to: " + suffix).formatted(Formatting.GREEN), false);
                                                return 1;
                                            })
                                    )
                            )
                            .then(CommandManager.literal("default_color")
                                    .then(CommandManager.argument("color", StringArgumentType.string())
                                            .suggests((context, builder) -> {
                                                for (String color : Types.getColorNames()) {
                                                    builder.suggest(color);
                                                }
                                                return builder.buildFuture();
                                            })
                                            .executes(Commands::setDefaultColor)
                                    )
                            )
                            .then(CommandManager.literal("finished_text")
                                    .then(CommandManager.argument("value", StringArgumentType.string())
                                            .suggests((context,builder) -> {
                                                builder.suggest("{name} over");
                                                return builder.buildFuture();
                                            })
                                            .executes(Commands::setFinishedText))
                            )
                    )

                    .then(CommandManager.literal("set")
                            .then(CommandManager.argument("name",StringArgumentType.word())
                                    .executes(Commands::setActiveTimer)
                                    .suggests((context,builder) -> {

                                        for (TimerData timer : TimerStorage.getTimers()) {
                                            builder.suggest(timer.name);
                                        }
                                        return builder.buildFuture();
                                    })
                                    .then(CommandManager.literal("color")
                                        .then(CommandManager.argument("color",StringArgumentType.string())
                                                .suggests((context,builder) -> {
                                                   for (String color : Types.getColorNames()) {
                                                        builder.suggest(color);
                                                    }
                                                    return builder.buildFuture();
                                                })
                                                .executes(Commands::setColor))


                                    )
                                    .then(CommandManager.literal("formattings")
                                            .then(CommandManager.argument("formatting", StringArgumentType.string())
                                                    .suggests((context, builder) -> {
                                                        for (String formatting : Types.getFormattingNames()) {
                                                            builder.suggest(formatting);
                                                        }
                                                        return builder.buildFuture();
                                                    })
                                                    .executes(Commands::setFormattings)
                                            )
                                    )
                                    .then(CommandManager.literal("time")
                                            .then(CommandManager.argument("time",StringArgumentType.string())
                                                    .executes(Commands::setTime)
                                            )
                                    )
                                    .then(CommandManager.literal("type")
                                            .then(CommandManager.literal("Countup")
                                                    .executes(Commands::setupType))
                                            .then(CommandManager.literal("Countdown")
                                                    .executes(Commands::setdownType))
                                    )
                            )
                    )
            );
        }));
    }

    private static int setFormattings(CommandContext<ServerCommandSource> context) {
        String name = StringArgumentType.getString(context,"name");
        String formatting = StringArgumentType.getString(context,"formatting");
        MinecraftServer server = context.getSource().getServer();
        boolean exists = TimerStorage.getTimers().stream()
                .anyMatch(timer -> timer.name.equals(name));

        if (!exists) {
            context.getSource().sendFeedback(() -> Text.literal("Timer not found!").formatted(Formatting.RED),false);
        } else {
            if (Types.getFormattingNames().contains(formatting)) {
                boolean enabled =  TimerStorage.setFormattings(server,name,formatting);
                context.getSource().sendFeedback(() -> Text.literal((enabled? "En" : "Dis") + "abled formatting for "+name+": "+formatting).formatted(Formatting.GREEN),false);
            } else {
                context.getSource().sendFeedback(() -> Text.literal("Formatting not found!").formatted(Formatting.RED),false);
            }

        }
        return 1;
    }

    private static int setFinishedText(CommandContext<ServerCommandSource> context) {
        String text = StringArgumentType.getString(context,"value");
        MinecraftServer server = context.getSource().getServer();
        TimerStorage.setFinishedMessage(server,text);
        context.getSource().sendFeedback(() -> Text.literal("Finished Text set to: "+text).formatted(Formatting.GREEN),false);
        return 1;
    }

    private static int setActiveTimer(CommandContext<ServerCommandSource> context) {
        String name = StringArgumentType.getString(context,"name");
        MinecraftServer server = context.getSource().getServer();

        boolean exists = TimerStorage.getTimers().stream()
                .anyMatch(timer -> timer.name.equals(name));

        if (!exists) {
            context.getSource().sendFeedback(() -> Text.literal("Timer not found!").formatted(Formatting.RED),false);
        } else {
            TimerStorage.setActiveTimer(server,name);
            context.getSource().sendFeedback(() -> Text.literal("Set active Timer: "+name).formatted(Formatting.GREEN),false);
        }

        return 1;
    }

    private static int setupType(CommandContext<ServerCommandSource> context) {
        String name = StringArgumentType.getString(context,"name");
        MinecraftServer server = context.getSource().getServer();

        boolean exists = TimerStorage.getTimers().stream()
                .anyMatch(timer -> timer.name.equals(name));

        if (!exists) {
            context.getSource().sendFeedback(() -> Text.literal("Timer not found!").formatted(Formatting.RED),false);
        } else {
            boolean colorset = TimerStorage.setType(server,name,"Stopwatch");
            String text = name+" is now a Stopwatch";

            if (colorset) {
                context.getSource().sendFeedback(() -> Text.literal(text).formatted(Formatting.GREEN),false);
            }
        }
        return 1;
    }

    private static int setdownType(CommandContext<ServerCommandSource> context) {
        String name = StringArgumentType.getString(context,"name");
        MinecraftServer server = context.getSource().getServer();

        boolean exists = TimerStorage.getTimers().stream()
                .anyMatch(timer -> timer.name.equals(name));

        if (!exists) {
            context.getSource().sendFeedback(() -> Text.literal("Timer not found!").formatted(Formatting.RED),false);
        } else {
            boolean colorset = TimerStorage.setType(server,name,"Countdown");
            String text = name+" is now a Countdown";

            if (colorset) {
                context.getSource().sendFeedback(() -> Text.literal(text).formatted(Formatting.GREEN),false);
            }
        }
        return 1;
    }


    private static int setColor(CommandContext<ServerCommandSource> context) {
        String color = StringArgumentType.getString(context, "color").toLowerCase();
        String name = StringArgumentType.getString(context, "name");
        Map<String, String> colorMap = Types.getColorMap();
        MinecraftServer server = context.getSource().getServer();

        boolean exists = TimerStorage.getTimers().stream()
                .anyMatch(timer -> timer.name.equals(name));



        if (!exists) {
            context.getSource().sendFeedback(() -> Text.literal("Timer not found!").formatted(Formatting.RED), false);
            return 0;
        }

        boolean isColorValid = false;

        if (colorMap.containsKey(color)) {
            isColorValid = true;
        } else if (color.startsWith("#") && color.length() == 7) {
            try {
                Integer.parseInt(color.substring(1), 16);
                isColorValid = true;
            } catch (NumberFormatException e) {
                isColorValid = false;
            }
        }

        if (!isColorValid) {
            context.getSource().sendFeedback(() -> Text.literal("Color not found or invalid! Use a valid name or hex code (#RRGGBB).").formatted(Formatting.RED), false);
            return 0;
        }

        boolean colorset = TimerStorage.setColor(server, name, color);

        Text nameText;

        DataResult<TextColor> parsed = TextColor.parse(color);
        Optional<TextColor> optional = parsed.result();

        if (optional.isPresent()) {
            nameText = Text.literal(name).setStyle(Style.EMPTY.withColor(optional.get()));
        } else {
            context.getSource().sendFeedback(() ->
                    Text.literal("Invalid hex color: " + color).formatted(Formatting.RED), false);
            return 0;
        }

        if (colorset) {
            Text fullText = Text.literal("Recolored ").append(nameText).append(Text.literal("§r"));
            context.getSource().sendFeedback(() -> fullText, false);
        }

        return 1;
    }





    private static int setDefaultColor(CommandContext<ServerCommandSource> context) {
        String color = StringArgumentType.getString(context, "color").toLowerCase();
        Map<String, String> colorMap = Types.getColorMap();
        MinecraftServer server = context.getSource().getServer();

        boolean isColorValid = false;

        if (colorMap.containsKey(color)) {
            isColorValid = true;
        } else if (color.startsWith("#") && color.length() == 7) {
            try {
                Integer.parseInt(color.substring(1), 16);
                isColorValid = true;
            } catch (NumberFormatException e) {
                isColorValid = false;
            }
        }

        if (!isColorValid) {
            context.getSource().sendFeedback(() ->
                    Text.literal("Color not found or invalid! Use a valid name or hex code (#RRGGBB).").formatted(Formatting.RED), false);
            return 0;
        }

        boolean colorset = TimerStorage.setDefaultColor(server, color);

        Optional<TextColor> optional = TextColor.parse(color).result();

        if (optional.isEmpty()) {
            context.getSource().sendFeedback(() ->
                    Text.literal("Invalid hex color: " + color).formatted(Formatting.RED), false);
            return 0;
        }

        TextColor textColor = optional.get();
        Text nameText = Text.literal(color).setStyle(Style.EMPTY.withColor(textColor));

        if (colorset) {
            Text fullText = Text.literal("Recolored ").append(nameText).append(Text.literal("§r"));
            context.getSource().sendFeedback(() -> fullText, false);
        }

        return 1;
    }






    private static int setTime(CommandContext<ServerCommandSource> context) {
        String name = StringArgumentType.getString(context,"name");
        String time = StringArgumentType.getString(context,"time");
        MinecraftServer server = context.getSource().getServer();


        boolean exists = TimerStorage.getTimers().stream()
                .anyMatch(timer -> timer.name.equals(name));

        int finaltime = CalTimer.toSeconds(time);

        if (!exists) {
            context.getSource().sendFeedback(() -> Text.literal("Timer not found!").formatted(Formatting.RED),false);
        }  else if (finaltime == 0) {
            context.getSource().sendFeedback(() -> Text.literal("Please enter time like this: 2d11h37m9s").formatted(Formatting.RED), false);
        } else {
            TimerStorage.setTime(server, name, CalTimer.toSeconds(time));
            context.getSource().sendFeedback(() -> Text.literal("Retimed " + name + ": " + time).formatted(Formatting.GREEN), false);
        }
        return 1;
    }


    private static int addStopWatch(CommandContext<ServerCommandSource> context) {
        String name = StringArgumentType.getString(context, "name");
        String type = "Stopwatch";
        MinecraftServer server = context.getSource().getServer();
        boolean exists = TimerStorage.getTimers().stream()
                .anyMatch(timer -> timer.name.equals(name));

        if (exists) {
            context.getSource().sendFeedback(() -> Text.literal("Stopwatch " + name + " already exists!").formatted(Formatting.RED),false);
        } else {
            TimerStorage.addTimer(server, name, type, 0);
            context.getSource().sendFeedback(() -> Text.literal("Stopwatch added: " + name).formatted(Formatting.GREEN), true);
        }

        return 1;

    }

    private static int addTimer(CommandContext<ServerCommandSource> context) {
        String name = StringArgumentType.getString(context, "name");
        String time = StringArgumentType.getString(context, "time");
        String type = "Countdown";
        MinecraftServer server = context.getSource().getServer();

        boolean exists = TimerStorage.getTimers().stream()
                .anyMatch(timer -> timer.name.equals(name));

        int finaltime = CalTimer.toSeconds(time);


        if (exists) {
            context.getSource().sendFeedback(() -> Text.literal("Timer " + name + " already exists!").formatted(Formatting.RED),false);
        }  else if (finaltime == 0) {
            context.getSource().sendFeedback(() -> Text.literal("Please enter time like this: 2d11h37m9s").formatted(Formatting.RED), false);
        } else {
            TimerStorage.addTimer(server, name, type, CalTimer.toSeconds(time));
            context.getSource().sendFeedback(() -> Text.literal("Timer added: " + name + " || " + time).formatted(Formatting.GREEN), true);
        }

        return 1;
    }


    private static int removeTimer(CommandContext<ServerCommandSource> context) {
        String name = StringArgumentType.getString(context, "name");
        MinecraftServer server = context.getSource().getServer();

        boolean removed = TimerStorage.removeTimer(server, name);
        if (removed) {
            context.getSource().sendFeedback(() -> Text.literal("Timer removed: " + name).formatted(Formatting.GREEN), true);
        } else {
            context.getSource().sendFeedback(() -> Text.literal("Timer not found: " + name).formatted(Formatting.RED), false);
        }
        return 1;
    }


    private static void timer(CommandContext<ServerCommandSource> context) {
        List<TimerData> timers = TimerStorage.getTimers();

        if (timers.isEmpty()) {
            context.getSource().sendFeedback(() -> Text.literal("No Timers available!").formatted(Formatting.RED), false);
            return;
        }

        Text existingtimerstxt = Text.literal("Existing Timers:\n\n").formatted(Formatting.UNDERLINE).formatted(Formatting.GRAY).formatted(Formatting.BOLD);



        String timerlist = timers.stream()
                .map(timer -> (timer.active ? "§a>§r " : "> ") + timer.name + ": " + CalTimer.toTime(timer.time))
                .collect(Collectors.joining(",\n"));

        Text existingtimers = Text.literal(timerlist).formatted(Formatting.WHITE);

        Text combined = Text.empty()
                        .append(existingtimerstxt)
                                .append(existingtimers);




        context.getSource().sendFeedback(() ->combined, false);
    }
}