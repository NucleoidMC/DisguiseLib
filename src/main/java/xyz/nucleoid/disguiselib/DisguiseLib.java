package xyz.nucleoid.disguiselib;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.minecraft.scoreboard.AbstractTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import xyz.nucleoid.disguiselib.command.DisguiseCommand;

public class DisguiseLib implements ModInitializer {

	public static final String MODID = "disguiselib";

	/**
	 * Disables collisions with disguised entities.
	 * (Client predictions are horrible sometimes ... )
	 */
	public static final Team DISGUISE_TEAM = new Team(new Scoreboard(), "");

	@Override
	public void onInitialize() {
		CommandRegistrationCallback.EVENT.register(DisguiseCommand::register);
		DISGUISE_TEAM.setCollisionRule(AbstractTeam.CollisionRule.PUSH_OTHER_TEAMS);
	}
}
