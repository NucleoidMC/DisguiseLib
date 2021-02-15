package org.samo_lego.disguiselib;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import org.samo_lego.disguiselib.command.DisguiseCommand;

public class DisguiseLib implements ModInitializer {
	@Override
	public void onInitialize() {
		CommandRegistrationCallback.EVENT.register(DisguiseCommand::register);
	}
}
