package xyz.nucleoid.disguiselib;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import xyz.nucleoid.disguiselib.command.DisguiseCommand;

@Mod("disguiselib")
public class DisguiseLibForge {

	public DisguiseLibForge() {
		DisguiseLib.init();
		MinecraftForge.EVENT_BUS.register(this);
	}

	@SubscribeEvent
	public void registerCommands(RegisterCommandsEvent event) {
		CommandDispatcher<ServerCommandSource> dispatcher = event.getDispatcher();

		DisguiseCommand.register(dispatcher, false);
	}
}
