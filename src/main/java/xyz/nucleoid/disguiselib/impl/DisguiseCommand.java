package xyz.nucleoid.disguiselib.impl;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.world.entity.EntitySpawnRequest;
import xyz.nucleoid.disguiselib.api.EntityDisguise;

import java.util.Collection;
import net.minecraft.util.Util;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.CompoundTagArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;

import static com.mojang.brigadier.arguments.StringArgumentType.word;
import static net.minecraft.commands.arguments.EntityArgument.entities;
import static net.minecraft.commands.synchronization.SuggestionProviders.SUMMONABLE_ENTITIES;
import static net.minecraft.world.entity.EntityTypes.PLAYER;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class DisguiseCommand {

    private static final Component NO_PERMISSION_ERROR = Component.translatable("commands.help.failed");

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext commandRegistryAccess, Commands.CommandSelection registrationEnvironment) {
        dispatcher.register(literal("disguise")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(argument("target", entities())
                        .then(literal("as")
                            .then(argument("disguise", new ResourceArgument<>(commandRegistryAccess, Registries.ENTITY_TYPE))
                                .suggests(SuggestionProviders.cast(SUMMONABLE_ENTITIES))
                                .executes(DisguiseCommand::setDisguise)
                                    .then(argument("nbt", CompoundTagArgument.compoundTag())
                                        .executes(DisguiseCommand::setDisguise)
                                    )
                            )
                            .then(literal("minecraft:player")
                                    .then(argument("playername", word())
                                            .executes(DisguiseCommand::disguiseAsPlayer)
                                    )
                                    .executes(DisguiseCommand::disguiseAsPlayer)
                            )
                            .then(literal("player")
                                    .then(argument("playername", word())
                                            .executes(DisguiseCommand::disguiseAsPlayer)
                                    )
                                    .executes(DisguiseCommand::disguiseAsPlayer)
                            )
                        )
                        .then(literal("clear").executes(DisguiseCommand::clearDisguise))
                )
        );
    }

    private static int disguiseAsPlayer(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Collection<? extends Entity> entities = EntityArgument.getEntities(ctx, "target");
        CommandSourceStack src = ctx.getSource();
        GameProfile profile;
        ServerPlayer player = src.getPlayerOrException();
        String playername;
        try {
            playername = StringArgumentType.getString(ctx, "playername");
        } catch(IllegalArgumentException ignored) {
            playername = player.getGameProfile().name();
        }

        profile = new GameProfile(Util.NIL_UUID, playername);  //fixme profile doesn't contain skin data; migrate to fabrictailor
        /*SkullBlockEntity.loadProperties(profile, gameProfile -> {
            // Minecraft doesn't allow "summoning" players, that's why we make an exception
            GameProfile finalProfile = gameProfile == null ? player.getGameProfile() : gameProfile;
            entities.forEach(entity -> {
                if(entity == src.getEntity()) {
                    if(src.hasPermissionLevel(2)) {
                        ((EntityDisguise) entity).disguiseAs(PLAYER);
                        if(finalProfile != null) {
                            ((EntityDisguise) entity).setGameProfile(finalProfile);
                        }
                    }
                    else
                        src.sendError(NO_PERMISSION_ERROR);
                } else {
                    if(src.hasPermissionLevel(2)) {
                        ((EntityDisguise) entity).disguiseAs(PLAYER);
                        if(finalProfile != null) {
                            ((EntityDisguise) entity).setGameProfile(finalProfile);
                        }
                    }
                    else
                        src.sendError(NO_PERMISSION_ERROR);
                }
            });
        });*/
        return 0;
    }

    private static int clearDisguise(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Collection<? extends Entity> entities = EntityArgument.getEntities(ctx, "target");
        CommandSourceStack src = ctx.getSource();
        // Minecraft doesn't allow "summoning" players, that's why we make an exception
        entities.forEach(entity -> {
            if(entity == src.getEntity()) {
                if(Commands.LEVEL_GAMEMASTERS.check(src.permissions()))
                    ((EntityDisguise) entity).removeDisguise();
                else
                    src.sendFailure(NO_PERMISSION_ERROR);
            } else {
                if(Commands.LEVEL_GAMEMASTERS.check(src.permissions())) {
                    ((EntityDisguise) entity).removeDisguise();
                } else
                    src.sendFailure(NO_PERMISSION_ERROR);
            }
        });
        return 0;
    }

    private static int setDisguise(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Collection<? extends Entity> entities = EntityArgument.getEntities(ctx, "target");
        CommandSourceStack src = ctx.getSource();
        var type = ResourceArgument.getResource(ctx, "disguise", Registries.ENTITY_TYPE);
        var disguise = BuiltInRegistries.ENTITY_TYPE.getKey(type.value());

        CompoundTag nbt;
        try {
            nbt = CompoundTagArgument.getCompoundTag(ctx, "nbt").copy();
        } catch(IllegalArgumentException ignored) {
            nbt = new CompoundTag();
        }
        nbt.putString("id", disguise.toString());

        CompoundTag finalNbt = nbt;
        entities.forEach(entity -> EntityType.loadEntityRecursive(finalNbt, ctx.getSource().getLevel(), new EntitySpawnRequest(EntitySpawnReason.LOAD, true), (entityx) -> {
            if(entity == src.getEntity()) {
                if(Commands.LEVEL_GAMEMASTERS.check(src.permissions()))
                    ((EntityDisguise) entity).disguiseAs(entityx);
                else
                    src.sendFailure(NO_PERMISSION_ERROR);
            } else {
                if(Commands.LEVEL_GAMEMASTERS.check(src.permissions())) {
                    ((EntityDisguise) entity).disguiseAs(entityx);
                } else
                    src.sendFailure(NO_PERMISSION_ERROR);
            }
            return entityx;
        }));
        return 0;
    }
}