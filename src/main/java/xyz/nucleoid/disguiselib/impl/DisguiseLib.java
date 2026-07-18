package xyz.nucleoid.disguiselib.impl;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team;

import static org.apache.logging.log4j.LogManager.getLogger;

public class DisguiseLib {

	/**
	 * Disables collisions with disguised entities.
	 * (Client predictions are horrible sometimes ... )
	 */
	public static final PlayerTeam DISGUISE_TEAM = new PlayerTeam(new Scoreboard(), "");

	public static void init() {
		DISGUISE_TEAM.setCollisionRule(Team.CollisionRule.PUSH_OTHER_TEAMS);
		getLogger("DisguiseLib").info("DisguiseLib loaded.");

		CommandRegistrationCallback.EVENT.register(DisguiseCommand::register);
	}

	public static void setPlayerClientVisibility(boolean clientVisibility) {
		DISGUISE_TEAM.setSeeFriendlyInvisibles(clientVisibility);
	}
}
