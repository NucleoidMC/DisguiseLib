package xyz.nucleoid.disguiselib.impl;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.block.entity.SkullBlockEntity;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.NbtCompoundArgumentType;
import net.minecraft.command.argument.RegistryEntryArgumentType;
import net.minecraft.command.argument.RegistryEntryReferenceArgumentType;
import net.minecraft.command.permission.Permission;
import net.minecraft.command.permission.PermissionLevel;
import net.minecraft.command.suggestion.SuggestionProviders;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import xyz.nucleoid.disguiselib.api.EntityDisguise;

import java.util.Collection;

import static com.mojang.brigadier.arguments.StringArgumentType.word;
import static net.minecraft.command.argument.EntityArgumentType.entities;
import static net.minecraft.command.suggestion.SuggestionProviders.SUMMONABLE_ENTITIES;
import static net.minecraft.entity.EntityType.PLAYER;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class DisguiseCommand {

    private static final Text NO_PERMISSION_ERROR = Text.translatable("commands.help.failed");

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess commandRegistryAccess, CommandManager.RegistrationEnvironment registrationEnvironment) {
        dispatcher.register(literal("disguise")
                .requires(src -> src.getPermissions().hasPermission(new Permission.Level(PermissionLevel.GAMEMASTERS)))
                .then(argument("target", entities())
                        .then(literal("as")
                            .then(argument("disguise", new RegistryEntryReferenceArgumentType<>(commandRegistryAccess, RegistryKeys.ENTITY_TYPE))
                                .suggests(SuggestionProviders.cast(SUMMONABLE_ENTITIES))
                                .executes(DisguiseCommand::setDisguise)
                                    .then(argument("nbt", NbtCompoundArgumentType.nbtCompound())
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

    private static int disguiseAsPlayer(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        Collection<? extends Entity> entities = EntityArgumentType.getEntities(ctx, "target");
        ServerCommandSource src = ctx.getSource();
        GameProfile profile;
        ServerPlayerEntity player = src.getPlayerOrThrow();
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

    private static int clearDisguise(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        Collection<? extends Entity> entities = EntityArgumentType.getEntities(ctx, "target");
        ServerCommandSource src = ctx.getSource();
        // Minecraft doesn't allow "summoning" players, that's why we make an exception
        entities.forEach(entity -> {
            if(entity == src.getEntity()) {
                if(src.getPermissions().hasPermission(new Permission.Level(PermissionLevel.GAMEMASTERS)))
                    ((EntityDisguise) entity).removeDisguise();
                else
                    src.sendError(NO_PERMISSION_ERROR);
            } else {
                if(src.getPermissions().hasPermission(new Permission.Level(PermissionLevel.GAMEMASTERS))) {
                    ((EntityDisguise) entity).removeDisguise();
                } else
                    src.sendError(NO_PERMISSION_ERROR);
            }
        });
        return 0;
    }

    private static int setDisguise(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        Collection<? extends Entity> entities = EntityArgumentType.getEntities(ctx, "target");
        ServerCommandSource src = ctx.getSource();
        var type = RegistryEntryReferenceArgumentType.getRegistryEntry(ctx, "disguise", RegistryKeys.ENTITY_TYPE);
        var disguise = Registries.ENTITY_TYPE.getId(type.value());

        NbtCompound nbt;
        try {
            nbt = NbtCompoundArgumentType.getNbtCompound(ctx, "nbt").copy();
        } catch(IllegalArgumentException ignored) {
            nbt = new NbtCompound();
        }
        nbt.putString("id", disguise.toString());

        NbtCompound finalNbt = nbt;
        entities.forEach(entity -> EntityType.loadEntityWithPassengers(finalNbt, ctx.getSource().getWorld(), SpawnReason.LOAD, (entityx) -> {
            if(entity == src.getEntity()) {
                if(src.getPermissions().hasPermission(new Permission.Level(PermissionLevel.GAMEMASTERS)))
                    ((EntityDisguise) entity).disguiseAs(entityx);
                else
                    src.sendError(NO_PERMISSION_ERROR);
            } else {
                if(src.getPermissions().hasPermission(new Permission.Level(PermissionLevel.GAMEMASTERS))) {
                    ((EntityDisguise) entity).disguiseAs(entityx);
                } else
                    src.sendError(NO_PERMISSION_ERROR);
            }
            return entityx;
        }));
        return 0;
    }
}