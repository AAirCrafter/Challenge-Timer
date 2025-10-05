package air.dev;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.serialization.DataResult;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class Commands {

    public static Boolean toggleEndisableTimer = true;

    public static void registerCommands() {
        CommandRegistrationCallback.EVENT.register(((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("timer")
                    //.requires(source -> source.hasPermissionLevel(1))
                    .executes(Commands::timer)
                    .then(CommandManager.literal("add")
                            .then(CommandManager.argument("name", StringArgumentType.string())
                                    .then(CommandManager.literal("Countup")
                                            .executes(Commands::addStopWatch))
                                    .then(CommandManager.literal("Countdown")
                                            .then(CommandManager.argument("time", StringArgumentType.string())
                                                    .executes(Commands::addTimer)
                                            )
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
                                if (TimerStorage.toggleTimersEnabled(context.getSource().getServer())) {
                                    context.getSource().sendFeedback(() -> Text.literal("Timer's enabled!").formatted(Formatting.GOLD), true);
                                } else {
                                    context.getSource().sendFeedback(() -> Text.literal("Timer's disabled!").formatted(Formatting.GOLD), true);
                                }
                                return 1;
                            }))
                    .then(CommandManager.literal("settings")
                            .then(CommandManager.literal("prefix")
                                    .then(CommandManager.argument("value", StringArgumentType.string())
                                            .executes(context -> {
                                                String prefix = StringArgumentType.getString(context, "value");
                                                TimerStorage.setDefaultPrefix(context.getSource().getServer(), prefix);
                                                context.getSource().sendFeedback(() -> Text.literal("Prefix set to: " + prefix).formatted(Formatting.GREEN), false);
                                                return 1;
                                            })
                                    )
                            )
                            .then(CommandManager.literal("suffix")
                                    .then(CommandManager.argument("value", StringArgumentType.string())
                                            .executes(context -> {
                                                String suffix = StringArgumentType.getString(context, "value");
                                                TimerStorage.setDefaultSuffix(context.getSource().getServer(), suffix);
                                                context.getSource().sendFeedback(() -> Text.literal("Suffix set to: " + suffix).formatted(Formatting.GREEN), false);
                                                return 1;
                                            })
                                    )
                            )
                            .then(CommandManager.literal("defaultColor")
                                    .then(CommandManager.argument("color", StringArgumentType.string())
                                            .suggests((context, builder) -> {
                                                for (String color : Renderer.getColorNames()) {
                                                    builder.suggest(color);
                                                }
                                                return builder.buildFuture();
                                            })
                                            .executes(Commands::setDefaultColor)
                                    )
                                    .then(CommandManager.literal("useTimerFormattings")
                                            .executes(context -> {
                                                context.getSource().sendFeedback(() ->
                                                        Text.literal((TimerStorage.toggleUseTimerFormattings(context.getSource().getServer()) ?
                                                                "Pre/Suffix now uses Timer Formattings" : "Pre/Suffix no longer uses Timer Formattings"))
                                                                .formatted(Formatting.GREEN),true);
                                                return 1;
                                            })
                                    )
                            )
                            .then(CommandManager.literal("finishedText")
                                    .then(CommandManager.argument("value", StringArgumentType.string())
                                            .suggests((context,builder) -> {
                                                builder.suggest("\"{name} over\"");
                                                return builder.buildFuture();
                                            })
                                            .executes(Commands::setFinishedText)
                                    )
                            )
                            .then(CommandManager.literal("pausedText")
                                    .then(CommandManager.argument("value", StringArgumentType.string())
                                            .suggests((context, builder) -> {
                                                builder.suggest("\"{name} paused - {time}\"");
                                                return builder.buildFuture();
                                            })
                                            .executes(Commands::setPausedText)
                                    )
                            )
                            .then(CommandManager.literal("animationSpeed")
                                    .then(CommandManager.argument("animspeed", IntegerArgumentType.integer())
                                            .executes(Commands::setAnimationTime)))
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
                                                        for (String color : Renderer.getColorNames()) {
                                                            builder.suggest(color);
                                                        }
                                                        return builder.buildFuture();
                                                    })
                                                    .executes(Commands::setColor)
                                            )
                                    )
                                    .then(CommandManager.literal("gradient")
                                            .then(CommandManager.argument("color1",StringArgumentType.string())
                                                    .then(CommandManager.argument("color2",StringArgumentType.string())
                                                            .then(CommandManager.literal("static")
                                                                    .executes(Commands::setStaticColorGradient))
                                                            .then(CommandManager.literal("animated")
                                                                    .executes(Commands::setAnimatedGradient))
                                                    )
                                            )
                                    )
                                    .then(CommandManager.literal("formattings")
                                            .then(CommandManager.argument("formatting", StringArgumentType.string())
                                                    .suggests((context, builder) -> {
                                                        for (String formatting : Renderer.getFormattingNames()) {
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
                    .then(CommandManager.literal("pause")
                            .executes(context -> {
                                //toggleTimerPause = !toggleTimerPause;
                                if (TimerStorage.toggleTimersPaused(context.getSource().getServer())) {
                                    context.getSource().sendFeedback(() -> Text.literal("Timer's paused!").formatted(Formatting.GOLD), true);
                                } else {
                                    context.getSource().sendFeedback(() -> Text.literal("Timer's unpaused!").formatted(Formatting.GOLD), true);
                                }
                                return 1;
                            }))
                    .then(CommandManager.literal("reset")
                            .then(CommandManager.argument("name",StringArgumentType.string())
                                    .suggests((context,builder) -> {

                                        for (TimerData timer : TimerStorage.getTimers()) {
                                            builder.suggest(timer.name);
                                        }
                                        return builder.buildFuture();
                                    })
                                    .executes(Commands::resetTimer)))
                    .then(CommandManager.literal("start")
                            .then(CommandManager.argument("name",StringArgumentType.string())
                                    .suggests((conext, builder) -> {
                                        for (TimerData timer : TimerStorage.getTimers()) {
                                            builder.suggest(timer.name);
                                        }
                                        return builder.buildFuture();
                                    })
                                    .executes(Commands::startTimer)))
                    .then(CommandManager.literal("help")
                            .executes(Commands::help)
                    )
            );
        }));
    }

    private static int setAnimationTime(CommandContext<ServerCommandSource> context) {
        int time = IntegerArgumentType.getInteger(context,"animspeed");
        MinecraftServer server = context.getSource().getServer();

        if (time >= -100 && time <= 100 && time != 0) {
            TimerStorage.setAnimationTime(server,time);
            context.getSource().sendFeedback(() -> Text.literal("Set AnimationTime to "+time).formatted(Formatting.GREEN),false);
        } else if (time == 0) {
            context.getSource().sendFeedback(() -> Text.literal("Please choose a static animation instad of typing 0").formatted(Formatting.RED),false);
        } else {
            context.getSource().sendFeedback(() -> Text.literal("Please enter a valid number between -100 and 100").formatted(Formatting.RED),false);
        }

        return 1;
    }


    private static int setAnimatedGradient(CommandContext<ServerCommandSource> context) {
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


    private static int setStaticColorGradient(CommandContext<ServerCommandSource> context) {
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


    private static int help(CommandContext<ServerCommandSource> context) {
        Text header = Text.literal("Quick Tutorial\n").formatted(Formatting.BOLD, Formatting.UNDERLINE).styled(style -> style.withColor(0x58C7FF));

        Text tutorial = Text.empty()
                .append(Text.literal("\nHow to add a (fe. 3 hour) Timer:\n").formatted(Formatting.WHITE))
                .append(Text.literal("▶ /timer add exampletimer Countdown 3h\n").formatted(Formatting.GRAY))
                .append(Text.literal("\nHow to start the Timer:\n").formatted(Formatting.WHITE))
                .append(Text.literal("▶ /timer start exampletimer\n").formatted(Formatting.GRAY))
                .append(Text.literal("\nHow to change the shown timer:\n").formatted(Formatting.WHITE))
                .append(Text.literal("▶ /timer set othertimer\n").formatted(Formatting.GRAY))
                .append(Text.literal("\nHow to change the timer's Prefix:\n").formatted(Formatting.WHITE))
                .append(Text.literal("▶ /timer settings prefix \">> \"\n").formatted(Formatting.GRAY))
                .append(Text.literal("▶▶ the defaultPrefix&defaultSuffix color is the default color\n").formatted(Formatting.GRAY))
                .append(Text.literal("\nHow to merge prefix formattings with timer formattings :\n").formatted(Formatting.WHITE))
                .append(Text.literal("▶ /timer settings defaultColor useTimerFormattings\">> \"\n").formatted(Formatting.GRAY))
                .append(Text.literal("\nHow to set the color of a timer:\n").formatted(Formatting.WHITE))
                .append(Text.literal("▶ /timer set exampletimer color aqua\n").formatted(Formatting.GRAY))
                .append(Text.literal("▶▶ you can use hex codes by typing the code in quotation marks fe: '#FFFFFF'\n").formatted(Formatting.GRAY))
                .append(Text.literal("\nHow to set the time of a timer:\n").formatted(Formatting.WHITE))
                .append(Text.literal("▶ /timer set exampletimer time 1h30m\n").formatted(Formatting.GRAY))
                .append(Text.literal("▶▶ time is given like '1d6h20m12s'. timekeywords like h = hours, etc.\n").formatted(Formatting.GRAY))
                .append(Text.literal("\nHow to modify the text shown when the timer is over:\n").formatted(Formatting.WHITE))
                .append(Text.literal("▶ /timer settings finishedText \"{name} is over stop doing sthẞ\"\n").formatted(Formatting.GRAY))
                .append(Text.literal("▶▶ use {name} for the timer's name in the finished_text\n").formatted(Formatting.GRAY))
                .append(Text.literal("\nHow to modify the text shown when the timer is paused:\n").formatted(Formatting.WHITE))
                .append(Text.literal("▶ /timer settings pausedText \"{name} is paused - {time}\"\n").formatted(Formatting.GRAY))
                .append(Text.literal("▶▶ {name} is the timer's name, {time} is the timer's time\n").formatted(Formatting.GRAY))
                ;

        Text link = Text.literal("\n▶ Click here for more Information")
                .formatted(Formatting.GOLD)
                .styled(style -> style.withClickEvent(new ClickEvent.OpenUrl(URI.create("https://modrinth.com/mod/challengetimer"))));

        context.getSource().sendMessage(Text.empty().append(header).append(tutorial).append(link));
        return 1;
    }


    private static int startTimer(CommandContext<ServerCommandSource> context) {
        String name = StringArgumentType.getString(context, "name");
        MinecraftServer server = context.getSource().getServer();
        TimerStorage.startTimer(server,name);
        context.getSource().sendFeedback(() -> Text.literal("Started "+name),false);
        return 1;
    }


    private static int setColor(CommandContext<ServerCommandSource> context) {
        String color = StringArgumentType.getString(context, "color").toLowerCase();
        String name = StringArgumentType.getString(context, "name");
        Map<String, Formatting> colorMap = Renderer.getColorMap();
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
            Text fullText = Text.literal("Recolored ").append(nameText);
            context.getSource().sendFeedback(() -> fullText, false);
        }

        return 1;
    }


    private static int resetTimer(CommandContext<ServerCommandSource> context) {
        String name = StringArgumentType.getString(context,"name");
        MinecraftServer server = context.getSource().getServer();
        boolean exists = TimerStorage.getTimers().stream()
                .anyMatch(timer -> timer.name.equals(name));

        if (!exists) {
            context.getSource().sendFeedback(() -> Text.literal("Timer not found!").formatted(Formatting.RED),false);
        } else {
            TimerStorage.resetTimer(server,name);
            context.getSource().sendFeedback(() -> Text.literal("Reset "+name).formatted(Formatting.GREEN),false);
        }
        return 1;
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
            if (Renderer.getFormattingNames().contains(formatting)) {
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
        TimerStorage.setFinishedText(server,text);
        context.getSource().sendFeedback(() -> Text.literal("Finished Text set to: "+text).formatted(Formatting.GREEN),false);
        return 1;
    }

    private static int setPausedText(CommandContext<ServerCommandSource> context) {
        String text = StringArgumentType.getString(context,"value");
        MinecraftServer server = context.getSource().getServer();
        TimerStorage.setPausedText(server,text);
        context.getSource().sendFeedback(() -> Text.literal("Paused Text set to: "+text).formatted(Formatting.GREEN),false);
        return 1;
    }


    private static int setActiveTimer(CommandContext<ServerCommandSource> context) {
        String name = StringArgumentType.getString(context,"name");
        MinecraftServer server = context.getSource().getServer();

        boolean exists = TimerStorage.getTimers().stream()
                .anyMatch(timer -> timer.name.equals(name));

        TimerData timernow = null;
        for (TimerData timer : TimerStorage.getTimers()) {
            if (timer.active) {
                timernow = timer;
            }
        }


        if (!exists) {
            context.getSource().sendFeedback(() -> Text.literal("Timer not found!").formatted(Formatting.RED),false);
        } else {
            if (name.equals(timernow.name)) {
                timernow.active = false;
            }
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


    private static int setDefaultColor(CommandContext<ServerCommandSource> context) {
        String color = StringArgumentType.getString(context, "color").toLowerCase();
        Map<String, Formatting> colorMap = Renderer.getColorMap();
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
            Text fullText = Text.literal("Set default color to ").append(nameText);
            context.getSource().sendFeedback(() -> fullText, true);
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


    private static int timer(CommandContext<ServerCommandSource> context) {
        List<TimerData> timers = TimerStorage.getTimers();

        if (timers.isEmpty()) {
            context.getSource().sendFeedback(() -> Text.literal("No Timers available!").formatted(Formatting.RED), false);
            return 1;
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
        return 1;
    }


}