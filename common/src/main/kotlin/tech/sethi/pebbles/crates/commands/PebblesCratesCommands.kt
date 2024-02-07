package tech.sethi.pebbles.crates.commands

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import net.minecraft.command.CommandSource
import net.minecraft.command.argument.EntityArgumentType
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.server.command.ServerCommandSource
import tech.sethi.pebbles.crates.config.ConfigHandler
import tech.sethi.pebbles.crates.config.CrateLoader
import tech.sethi.pebbles.crates.util.PM
import java.util.concurrent.CompletableFuture

object PebblesCratesCommands {

    fun register(dispatcher: CommandDispatcher<ServerCommandSource>) {
        val padminCommand = literal("padmin").requires { it.hasPermissionLevel(2) }



        val giveCreatorCommand =
            literal("givecreator").then(CommandManager.argument("player", EntityArgumentType.players())
                .then(CommandManager.argument("crate", StringArgumentType.greedyString())
                    .suggests { context, builder -> getCrateNameSuggestions(builder) }.executes { context ->
                        val player = EntityArgumentType.getPlayer(context, "player")
                        val crateName = StringArgumentType.getString(context, "crate")
                        val crate = CrateLoader.crateConfigs[crateName] ?: return@executes 0
                        val stack = ItemStack(Items.PAPER)
                        stack.setCustomName(PM.returnStyledText("Crate Creator: ${crate.crateName}"))
                        stack.orCreateNbt.putString("CrateName", crateName)
                        stack.orCreateNbt.putBoolean("CrateCreator", true)
                        player.inventory.offerOrDrop(stack)

                        player.sendMessage(
                            PM.returnStyledMMText("<gold>You have been given a crate creator for ${crate.crateName}"),
                            false
                        )

                        context.source.sendFeedback(
                            { PM.returnStyledMMText("<gold>Gave crate creator to ${player.name.string} for ${crate.crateName}") },
                            true
                        )

                        1
                    })
            )

        val crateCommand = literal("crate")

        val reloadCommand = literal("reload").executes { context ->
            ConfigHandler.reload()
            context.source.sendFeedback({ PM.returnStyledMMText("<gold>Reloaded crates") }, true)
            1
        }

        crateCommand.then(giveCreatorCommand)
        crateCommand.then(reloadCommand)

        padminCommand.then(giveCreatorCommand)
        padminCommand.then(reloadCommand)
        padminCommand.then(crateCommand)
        dispatcher.register(padminCommand)
    }

    private fun getCrateNameSuggestions(
        builder: SuggestionsBuilder
    ): CompletableFuture<Suggestions> {
        val crateNames = CrateLoader.crateConfigs.map { it.key }
        return CommandSource.suggestMatching(crateNames, builder)
    }
}