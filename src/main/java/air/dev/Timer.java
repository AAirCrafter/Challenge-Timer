package air.dev;

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
		});

		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			TimerStorage.save(server);
			System.out.println("Timer's saved!");
		});


		ServerTickEvents.START_SERVER_TICK.register(server -> {
			List<ServerPlayerEntity> player = server.getPlayerManager().getPlayerList();
			if (!player.isEmpty()) {
				if (Commands.toggleEndisableTimer) {
					for (ServerPlayerEntity players : server.getPlayerManager().getPlayerList()) {
						Types.sendActionBar(players);
					}
					if (Commands.toggleTimerPause) {tickcounter++;}

					if (tickcounter >= 20) {
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
						tickcounter = 0;
					}
				}
			}
		});
	}
}