package xyz.nucleoid.disguiselib;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import xyz.nucleoid.disguiselib.command.DisguiseCommand;

public class DisguiseLibFabric implements ModInitializer {

	@Override
	public void onInitialize() {
		DisguiseLib.init();
		CommandRegistrationCallback.EVENT.register(DisguiseCommand::register);
	}
}
