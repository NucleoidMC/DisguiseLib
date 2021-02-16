package xyz.nucleoid.disguiselib;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import xyz.nucleoid.disguiselib.command.DisguiseCommand;

public class DisguiseLib implements ModInitializer {

	public static final String MODID = "disguiselib";

	@Override
	public void onInitialize() {
		CommandRegistrationCallback.EVENT.register(DisguiseCommand::register);
	}
}
