package io.github.thefrontier.tab

import me.rojo8399.placeholderapi.PlaceholderService
import org.spongepowered.api.data.key.Keys
import org.spongepowered.api.entity.living.player.Player
import org.spongepowered.api.service.permission.PermissionService
import org.spongepowered.api.text.Text
import solace.java.util.unwrapped
import solace.sponge.Server
import solace.sponge.service.uncheckedService
import solace.sponge.text.fromAmpersand
import solace.sponge.text.text

val placeholderService: PlaceholderService by lazy { PlaceholderService::class.uncheckedService }
val permissionService: PermissionService by lazy { PermissionService::class.uncheckedService }

fun Player.tabFormat(target: Player): Text? {
    val format = this.getOption("tab-format").unwrapped?.fromAmpersand()
    if (format != null) {
        return placeholderService.replaceSourcePlaceholders(format, target)
    }
    return null
}

val Player.tabHeader: Text?
    get() {
        val header = this.getOption("tab-header").unwrapped?.split("\\n")?.map { it.fromAmpersand() }
        if (header != null) {
            return placeholderService.replaceSourcePlaceholders(Text.joinWith(Text.NEW_LINE, header), this)
        }
        return null
    }

val Player.tabFooter: Text?
    get() {
        val footer = this.getOption("tab-footer").unwrapped?.split("\\n")?.map { it.fromAmpersand() }
        if (footer != null) {
            return placeholderService.replaceSourcePlaceholders(Text.joinWith(Text.NEW_LINE, footer), this)
        }
        return null
    }

fun Player.updateHeaderAndFooter() {
    this.tabList.setHeaderAndFooter(this.tabHeader, this.tabFooter)
}

fun Player.updateTabListEntry() {
    for (player in Server.onlinePlayers) {
        player.tabList.getEntry(this.uniqueId).unwrapped?.setDisplayName(this.tabFormat(player) ?: this.name.text)
    }
}

fun updateHeadersAndFooters() {
    for (player in Server.onlinePlayers) {
        player.updateHeaderAndFooter()
    }
}

fun updateTabListEntries() {
    for (player in Server.onlinePlayers) {
        for (target in Server.onlinePlayers) {
            if (!target.isVanished) {
                player.tabList.getEntry(target.uniqueId).unwrapped?.setDisplayName(player.tabFormat(target)
                        ?: target.name.text)
            }
        }
    }
}

fun updateTab() {
    for (player in Server.onlinePlayers) {
        player.updateHeaderAndFooter()
        for (target in Server.onlinePlayers) {
            if (!target.isVanished) {
                player.tabList.getEntry(target.uniqueId).unwrapped?.setDisplayName(player.tabFormat(target)
                        ?: target.name.text)
            }
        }
    }
}

inline val Player.isVanished: Boolean get() = this[Keys.VANISH].orElse(false)