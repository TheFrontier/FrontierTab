package io.github.thefrontier.tab

import org.spongepowered.api.command.CommandSource
import org.spongepowered.api.command.args.CommandArgs
import org.spongepowered.api.command.args.CommandContext
import org.spongepowered.api.command.args.CommandElement
import org.spongepowered.api.text.Text
import pw.dotdash.solace.sponge.service.permission.get

class SubjectCommandElement(key: Text, private val collection: String) : CommandElement(key) {
    override fun parseValue(source: CommandSource, args: CommandArgs): Any? {
        val subjectColl = permissionService[collection]

        val subject = args.next()

        if (subjectColl != null) {
            return subjectColl[subject]
        }
        return null
    }

    override fun complete(src: CommandSource, args: CommandArgs, context: CommandContext): List<String> {
        val subjectColl = permissionService[collection]

        return subjectColl?.loadedSubjects?.map { it.identifier } ?: listOf()
    }
}