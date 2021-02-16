package org.samo_lego.disguiselib.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.suggestion.SuggestionProviders;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.registry.Registry;
import org.samo_lego.disguiselib.EntityDisguise;

import java.util.Optional;

import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;
import static net.minecraft.command.argument.EntityArgumentType.entity;
import static net.minecraft.entity.EntityType.FISHING_BOBBER;
import static net.minecraft.entity.EntityType.ITEM;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class DisguiseCommand {

    private static final SuggestionProvider<ServerCommandSource> DISGUISES;

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, boolean dedicated) {
        dispatcher.register(literal("disguise")
                .requires(serverCommandSource -> serverCommandSource.hasPermissionLevel(4))
                .then(argument("target", entity())
                        .then(literal("as")
                            .then(argument("disguise", greedyString())
                                .suggests(DISGUISES)
                                .executes(DisguiseCommand::setDisguise)
                            )
                        )
                        .then(literal("clear").executes(DisguiseCommand::clearDisguise))
                )
        );
    }

    private static int clearDisguise(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        Entity entity = EntityArgumentType.getEntity(ctx, "target");
        ((EntityDisguise) entity).removeDisguise();
        return 0;
    }

    private static int setDisguise(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        Entity entity = EntityArgumentType.getEntity(ctx, "target");
        String disguise = StringArgumentType.getString(ctx, "disguise");
        Optional<EntityType<?>> optionalType = EntityType.get(disguise);
        if(optionalType.isPresent())
            ((EntityDisguise) entity).disguiseAs(optionalType.get());
        else
            ctx.getSource().sendError(new LiteralText("Invalid entity id!"));
        return 0;
    }

    static {
        DISGUISES = SuggestionProviders.register(
                new Identifier("taterzens", "entites"),
                (context, builder) ->
                        CommandSource.suggestFromIdentifier(Registry.ENTITY_TYPE.stream().filter(type -> type != FISHING_BOBBER || type != ITEM), builder, EntityType::getId,
                                (entityType) -> new TranslatableText(Util.createTranslationKey("entity", EntityType.getId(entityType)))
                        )
        );
    }
}
