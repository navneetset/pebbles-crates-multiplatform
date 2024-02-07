package tech.sethi.pebbles.crates.util

import com.mojang.brigadier.ParseResults
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.minecraft.entity.effect.StatusEffect
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtHelper
import net.minecraft.nbt.NbtList
import net.minecraft.nbt.NbtString
import net.minecraft.registry.Registries
import net.minecraft.server.MinecraftServer
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.sound.SoundEvent
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import tech.sethi.pebbles.crates.config.ConfigHandler

object PM {
    private fun parseMessageWithStyles(
        text: String, placeholder: String, style: Boolean = true, forceMM: Boolean = false
    ): Component {
        val mm = if (style) {
            MiniMessage.miniMessage()
        } else {
            MiniMessage.builder().tags(TagResolver.empty()).build()
        }

        if (ConfigHandler.config.textType == ConfigHandler.TextType.LEGACY && !forceMM) {
            val legacySerializer = LegacyComponentSerializer.legacyAmpersand()
            val formattedText = text.replace("{placeholder}", placeholder)
            return legacySerializer.deserialize(formattedText)
        }

        return mm.deserialize(text.replace("{placeholder}", placeholder)).decoration(TextDecoration.ITALIC, false)
    }

    fun returnStyledText(text: String, style: Boolean = true): MutableText {
//        val parsedText = text.replace("%", "%%")
        val component = parseMessageWithStyles(text, "placeholder", style)
        val gson = GsonComponentSerializer.gson()
        val json = gson.serialize(component)
        return Text.Serializer.fromJson(json) as MutableText
    }

    fun returnStyledMMText(text: String): MutableText {
        val component = parseMessageWithStyles(text, "placeholder", true, true)
        val gson = GsonComponentSerializer.gson()
        val json = gson.serialize(component)
        return Text.Serializer.fromJson(json) as MutableText
    }

    fun returnStyledJson(text: String): String {
        val component = parseMessageWithStyles(text, "placeholder")
        val gson = GsonComponentSerializer.gson()
        val json = gson.serialize(component)
        return json
    }

    fun setLore(itemStack: ItemStack, lore: List<String>) {
        val itemNbt = itemStack.getOrCreateSubNbt("display")
        val loreNbt = NbtList()

        for (line in lore) {
            loreNbt.add(NbtString.of(returnStyledJson(line)))
        }

        itemNbt.put("Lore", loreNbt)
    }

    fun sendText(player: PlayerEntity, text: String) {
        val component = returnStyledText(text)
        player.sendMessage(component, false)
    }

    fun parseCommand(
        command: String, context: String, server: MinecraftServer, player: PlayerEntity?
    ): ParseResults<ServerCommandSource>? {
        val cmdManager = server.commandManager

        when (context) {
            "console" -> {
                return cmdManager?.dispatcher?.parse(command, server.commandSource)
            }

            "player" -> {
                return cmdManager?.dispatcher?.parse(command, player?.commandSource)
            }
        }

        return null
    }

    fun createItemStack(
        item: Item, count: Int, name: String? = null, lore: List<String>? = null, nbtString: String? = null
    ): ItemStack {
        val itemStack = ItemStack(item, count)

        if (nbtString != null) {
            itemStack.nbt = NbtHelper.fromNbtProviderString(nbtString)
        }

        if (name != null) {
            itemStack.setCustomName(returnStyledText(name))
        }

        if (lore != null) {
            setLore(itemStack, lore)
        }

        return itemStack
    }

    fun getItem(itemId: String): Item {
        return Registries.ITEM.get(Identifier.tryParse(itemId))
    }

    fun getSounds(soundId: String): SoundEvent? {
        return Registries.SOUND_EVENT.get(Identifier.tryParse(soundId)) ?: null
    }

    fun getStatusEffect(statusEffectId: String): StatusEffect {
        return Registries.STATUS_EFFECT.get(Identifier.tryParse(statusEffectId))
            ?: throw Exception("Status effect $statusEffectId not found")
    }
}