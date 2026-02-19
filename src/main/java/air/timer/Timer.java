package air.timer;

import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;

public class Timer implements ModInitializer {

    int tickcounter = 0;

    @Override
    public void onInitialize() {
        Commands.registerCommands();

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            System.out.println("[TimerStorage] Loading Timer at ServerStart ...");
            TimerStorage.load(server);

            if (TimerStorage.getTimers().isEmpty()) {
                TimerStorage.addTimer(server,"1-Hour","Countdown",3600);
                TimerStorage.addTimer(server,"2-Hours","Countdown",7200);
            }
        });

        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            TimerStorage.save(server);
            System.out.println("[TimerStorage] Timer's saved!");
        });

        ServerTickEvents.START_SERVER_TICK.register(server -> {
            List<ServerPlayerEntity> player = server.getPlayerManager().getPlayerList();

            if (!player.isEmpty() && TimerStorage.areTimersEnabled()) {
                for (ServerPlayerEntity players : server.getPlayerManager().getPlayerList()) {
                    Renderer.sendActionBar(players);
                }
                if (!TimerStorage.areTimersPaused()) tickcounter++;

                if (tickcounter % 20 == 0) {
                    for (var timer : TimerStorage.getTimers()) {
                        if (Boolean.TRUE.equals(timer.active) && timer.time >= 0) {
                            if (timer.type.equals("Stopwatch")) {
                                timer.time += 1;
                            } else {
                                timer.time -= 1;
                            }
                            break;
                        }
                    }

                    if (tickcounter == 200) {
                        TimerStorage.save(server);
                        tickcounter = 0;
                    }
                }
            }
        });
    }
}