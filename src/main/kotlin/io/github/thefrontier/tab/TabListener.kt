package io.github.thefrontier.tab

import org.spongepowered.api.entity.living.player.Player
import org.spongepowered.api.event.Listener
import org.spongepowered.api.event.filter.Getter
import org.spongepowered.api.event.game.GameReloadEvent
import org.spongepowered.api.event.network.ClientConnectionEvent

class TabListener(private val plugin: FrontierTab) {

    @Listener
    fun onReload(event: GameReloadEvent) {
        plugin.reload()
    }

    @Listener
    fun onJoin(event: ClientConnectionEvent.Join, @Getter("getTargetEntity") player: Player) {
        updateTab()
    }

    @Listener
    fun onDisconnect(event: ClientConnectionEvent.Disconnect, @Getter("getTargetEntity") player: Player) {
        updateTab()
    }
}