package io.github.thefrontier.tab

import com.google.inject.Inject
import ninja.leaping.configurate.commented.CommentedConfigurationNode
import ninja.leaping.configurate.loader.ConfigurationLoader
import org.slf4j.Logger
import org.spongepowered.api.command.CommandResult
import org.spongepowered.api.command.CommandSource
import org.spongepowered.api.command.args.CommandElement
import org.spongepowered.api.command.args.GenericArguments
import org.spongepowered.api.command.spec.CommandSpec
import org.spongepowered.api.config.DefaultConfig
import org.spongepowered.api.entity.living.player.User
import org.spongepowered.api.event.Listener
import org.spongepowered.api.event.game.state.GameInitializationEvent
import org.spongepowered.api.plugin.Dependency
import org.spongepowered.api.plugin.Plugin
import org.spongepowered.api.scheduler.Task
import org.spongepowered.api.service.permission.PermissionService
import org.spongepowered.api.service.permission.Subject
import org.spongepowered.api.text.Text
import pw.dotdash.solace.guava.typeToken
import pw.dotdash.solace.java.unwrapped
import pw.dotdash.solace.sponge.asset.asset
import pw.dotdash.solace.sponge.command.registerCommand
import pw.dotdash.solace.sponge.event.registerListeners
import pw.dotdash.solace.sponge.text.darkGreen
import pw.dotdash.solace.sponge.text.fromAmpersand
import pw.dotdash.solace.sponge.text.green
import pw.dotdash.solace.sponge.text.text
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit

@Plugin(id = "frontier-tab", name = "FrontierTab", version = "1.1.0",
        description = "[Frontier] A tab menu customization plugin with placeholder support.",
        url = "https://github.com/TheFrontier/FrontierTab",
        authors = ["DotDash"],
        dependencies = [
            Dependency(id = "solace", version = "[2.0.0,)"),
            Dependency(id = "placeholderapi", version = "4.4")
        ])
class FrontierTab @Inject constructor(
        private val logger: Logger,
        @DefaultConfig(sharedRoot = true) private val configPath: Path,
        @DefaultConfig(sharedRoot = true) private val configLoader: ConfigurationLoader<CommentedConfigurationNode>
) {

    private lateinit var config: Config

    private lateinit var refreshTask: Task

    @Listener
    fun onInit(event: GameInitializationEvent) {
        if (!Files.exists(configPath)) {
            this.asset("frontier-tab.conf")?.copyToFile(configPath)
        }
        config = configLoader.load().getValue(Config::class.typeToken)

        if (config.refreshEverySecs > 0) {
            refreshTask = Task.builder()
                    .execute { _ -> updateTab() }
                    .interval(config.refreshEverySecs, TimeUnit.SECONDS)
                    .name("FrontierTab - AutoRefresher")
                    .submit(this)
        }

        this.registerListeners(TabListener(this))
        this.registerCommands()

        logger.info("FrontierTab successfully initialized.")
    }

    fun reload() {
        config = configLoader.load().getValue(Config::class.typeToken)

        refreshTask.cancel()
        if (config.refreshEverySecs > 0) {
            refreshTask = Task.builder()
                    .execute { _ -> updateTab() }
                    .interval(config.refreshEverySecs, TimeUnit.SECONDS)
                    .name("FrontierTab - AutoRefresher")
                    .submit(this)
        }

        updateTab()
    }

    private fun registerCommands() {
        val reload = CommandSpec.builder()
                .executor { src, _ ->
                    reload()
                    src.sendMessage("Successfully reloaded the tab menu.".green)
                    return@executor CommandResult.success()
                }
                .permission("frontier.tab.reload.base")
                .build()

        val userFormat = CommandSpec.builder()
                .executor { src, args ->
                    val user = args.getOne<User>("user").get()
                    val format = args.getOne<String>("format").unwrapped
                    checkOrSetOption(src, user, "tab-format", format)
                    return@executor CommandResult.success()
                }
                .permission("frontier.tab.user.format")
                .arguments(GenericArguments.user("user".text),
                        !GenericArguments.remainingJoinedStrings("format".text))
                .build()

        val userHeader = CommandSpec.builder()
                .executor { src, args ->
                    val user = args.getOne<User>("user").get()
                    val format = args.getOne<String>("header").unwrapped
                    checkOrSetOption(src, user, "tab-header", format)
                    return@executor CommandResult.success()
                }
                .permission("frontier.tab.user.header")
                .arguments(GenericArguments.user("user".text),
                        !GenericArguments.remainingJoinedStrings("header".text))
                .build()

        val userFooter = CommandSpec.builder()
                .executor { src, args ->
                    val user = args.getOne<User>("user").get()
                    val format = args.getOne<String>("footer").unwrapped
                    checkOrSetOption(src, user, "tab-footer", format)
                    return@executor CommandResult.success()
                }
                .permission("frontier.tab.user.footer")
                .arguments(GenericArguments.user("user".text),
                        !GenericArguments.remainingJoinedStrings("footer".text))
                .build()

        val user = CommandSpec.builder()
                .permission("frontier.tab.user.base")
                .child(userFormat, "format")
                .child(userHeader, "header")
                .child(userFooter, "footer")
                .build()

        val groupFormat = CommandSpec.builder()
                .executor { src, args ->
                    val subject = args.getOne<Subject>("group").get()
                    val format = args.getOne<String>("format").unwrapped
                    checkOrSetOption(src, subject, "tab-format", format)
                    return@executor CommandResult.success()
                }
                .permission("frontier.tab.group.format")
                .arguments(SubjectCommandElement("group".text, PermissionService.SUBJECTS_GROUP),
                        !GenericArguments.remainingJoinedStrings("format".text))
                .build()

        val groupHeader = CommandSpec.builder()
                .executor { src, args ->
                    val subject = args.getOne<Subject>("group").get()
                    val format = args.getOne<String>("header").unwrapped
                    checkOrSetOption(src, subject, "tab-header", format)
                    return@executor CommandResult.success()
                }
                .permission("frontier.tab.group.header")
                .arguments(SubjectCommandElement("group".text, PermissionService.SUBJECTS_GROUP),
                        !GenericArguments.remainingJoinedStrings("header".text))
                .build()

        val groupFooter = CommandSpec.builder()
                .executor { src, args ->
                    val subject = args.getOne<Subject>("group").get()
                    val format = args.getOne<String>("footer").unwrapped
                    checkOrSetOption(src, subject, "tab-footer", format)
                    return@executor CommandResult.success()
                }
                .permission("frontier.tab.group.footer")
                .arguments(SubjectCommandElement("group".text, PermissionService.SUBJECTS_GROUP),
                        !GenericArguments.remainingJoinedStrings("footer".text))
                .build()

        val group = CommandSpec.builder()
                .permission("frontier.tab.group.base")
                .child(groupFormat, "format")
                .child(groupHeader, "header")
                .child(groupFooter, "footer")
                .build()

        val parent = CommandSpec.builder()
                .description("The base command for FrontierTab.".text)
                .permission("frontier.tab.command.base")
                .child(reload, "reload")
                .child(user, "user")
                .child(group, "group")
                .build()

        this.registerCommand(parent, "tab")
    }

    private fun checkOrSetOption(src: CommandSource, user: User, option: String, replacement: String?) {
        if (replacement != null) {
            if (replacement == "clear") {
                user.subjectData.setOption(setOf(), option, null)
                updateTab()
                src.sendMessage("Removed $option for ".green + user.name.darkGreen + ".".green)
                return
            }

            user.subjectData.setOption(setOf(), option, replacement)
            updateTab()
            src.sendMessage("Set ".green + user.name.darkGreen + "'s $option to '".green + replacement.fromAmpersand() + "'".green)
        } else {
            val current = user.getOption(option).unwrapped
            if (current != null) {
                src.sendMessage(user.name.darkGreen + "'s current $option is '".green + current.fromAmpersand() + "'".green)
            } else {
                src.sendMessage(user.name.darkGreen + " does not currently have a $option set.".green)
            }
        }
    }

    private fun checkOrSetOption(src: CommandSource, subject: Subject, option: String, replacement: String?) {
        if (replacement != null) {
            if (replacement == "clear") {
                subject.subjectData.setOption(setOf(), option, null)
                updateTab()
                src.sendMessage("Removed $option for ".green + subject.identifier.darkGreen + ".".green)
                return
            }

            subject.subjectData.setOption(setOf(), option, replacement)
            updateTab()
            src.sendMessage("Set ".green + subject.identifier.darkGreen + "'s $option to '".green + replacement.fromAmpersand() + "'".green)
        } else {
            val current = subject.getOption(option).unwrapped
            if (current != null) {
                src.sendMessage(subject.identifier.darkGreen + "'s current $option is '".green + current.fromAmpersand() + "'".green)
            } else {
                src.sendMessage(subject.identifier.darkGreen + " does not currently have a $option set.".green)
            }
        }
    }
}

operator fun Text.plus(other: Text): Text = Text.builder().append(this).append(other).build()
operator fun Text.plus(other: String): Text = Text.builder().append(this).append(other.text).build()
inline operator fun CommandElement.not(): CommandElement = GenericArguments.optional(this)