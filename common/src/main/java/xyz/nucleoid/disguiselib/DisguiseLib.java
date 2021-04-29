package xyz.nucleoid.disguiselib;

import net.minecraft.scoreboard.AbstractTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;

public class DisguiseLib {

	/**
	 * Disables collisions with disguised entities.
	 * (Client predictions are horrible sometimes ... )
	 */
	public static final Team DISGUISE_TEAM = new Team(new Scoreboard(), "");

	public static void init() {
		DISGUISE_TEAM.setCollisionRule(AbstractTeam.CollisionRule.PUSH_OTHER_TEAMS);
	}
}
