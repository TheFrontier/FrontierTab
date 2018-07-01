package io.github.thefrontier.tab

import ninja.leaping.configurate.objectmapping.Setting
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable

@ConfigSerializable
data class Config(
        @Setting val refreshEverySecs: Long = 60
)