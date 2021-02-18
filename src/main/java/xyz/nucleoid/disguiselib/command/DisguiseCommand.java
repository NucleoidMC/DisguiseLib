package xyz.nucleoid.disguiselib.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.EntitySummonArgumentType;
import net.minecraft.command.argument.NbtCompoundTagArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.Identifier;
import xyz.nucleoid.disguiselib.EntityDisguise;

import static net.minecraft.command.argument.EntityArgumentType.entity;
import static net.minecraft.command.suggestion.SuggestionProviders.SUMMONABLE_ENTITIES;
import static net.minecraft.entity.EntityType.PLAYER;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class DisguiseCommand {


    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, boolean dedicated) {
        dispatcher.register(literal("disguise")
                .requires(serverCommandSource -> serverCommandSource.hasPermissionLevel(4))
                .then(argument("target", entity())
                        .then(literal("as")
                            .then(argument("disguise", EntitySummonArgumentType.entitySummon())
                                .suggests(SUMMONABLE_ENTITIES)
                                .executes(DisguiseCommand::setDisguise)
                                    .then(argument("nbt", NbtCompoundTagArgumentType.nbtCompound())
                                        .executes(DisguiseCommand::setDisguise)
                                    )
                            )
                            .then(literal("minecraft:player").executes(DisguiseCommand::disguiseAsPlayer))
                        )
                        .then(literal("clear").executes(DisguiseCommand::clearDisguise))
                )
        );
    }

    private static int disguiseAsPlayer(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        Entity entity = EntityArgumentType.getEntity(ctx, "target");
        // Minecraft doesn't allow "summoning" players, that's why we make an exception
        ((EntityDisguise) entity).disguiseAs(PLAYER);
        return 0;
    }

    private static int clearDisguise(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        Entity entity = EntityArgumentType.getEntity(ctx, "target");
        ((EntityDisguise) entity).removeDisguise();
        return 0;
    }

    private static int setDisguise(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        Entity entity = EntityArgumentType.getEntity(ctx, "target");
        Identifier disguise = EntitySummonArgumentType.getEntitySummon(ctx, "disguise");


        CompoundTag nbt;
        try {
            nbt = NbtCompoundTagArgumentType.getCompoundTag(ctx, "nbt").copy();
        } catch(IllegalArgumentException ignored) {
            nbt = new CompoundTag();
        }
        nbt.putString("id", disguise.toString());

        EntityType.loadEntityWithPassengers(nbt, ctx.getSource().getWorld(), (entityx) -> {
            ((EntityDisguise) entity).disguiseAs(entityx);
            return entityx;
        });
        return 0;
    }
}
