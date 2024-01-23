package org.teamvoided.civilization.commands

import com.mojang.authlib.GameProfile
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.context.CommandContext
import net.minecraft.command.argument.GameProfileArgumentType
import net.minecraft.command.argument.MessageArgumentType
import net.minecraft.server.command.CommandManager.argument
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.ClickEvent
import net.minecraft.text.HoverEvent
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import org.teamvoided.civilization.commands.argument.SettlementArgumentType
import org.teamvoided.civilization.commands.argument.SettlementArgumentType.settlementArg
import org.teamvoided.civilization.config.CivilizationConfig
import org.teamvoided.civilization.data.PlayerDataManager
import org.teamvoided.civilization.data.PlayerDataManager.getRole
import org.teamvoided.civilization.data.PlayerDataManager.getSettlements
import org.teamvoided.civilization.data.Settlement
import org.teamvoided.civilization.data.SettlementManager
import org.teamvoided.civilization.util.ResultType
import org.teamvoided.civilization.util.Util.lTxt
import org.teamvoided.civilization.util.Util.tTxt

object SettlementCommand {
    fun init(dispatcher: CommandDispatcher<ServerCommandSource>) {
        val settlementNode = literal("settlement").build()
        dispatcher.root.addChild(settlementNode)


        val createNode = literal("create").build()
        settlementNode.addChild(createNode)
        val createNodeNameArg = argument("name", MessageArgumentType.message())
            .executes { createSettlement(it, MessageArgumentType.getMessage(it, "name")) }.build()
        createNode.addChild(createNodeNameArg)


        val deleteNode = literal("delete").build()
        settlementNode.addChild(deleteNode)
        val deleteNodeNameArg = settlementArg("name")
            .executes { deleteSettlement(it, SettlementArgumentType.getSettlement(it, "name"), false) }.build()
        deleteNode.addChild(deleteNodeNameArg)
        val deleteNodeNameArgConfirmArg = literal("confirm")
            .executes { deleteSettlement(it, SettlementArgumentType.getSettlement(it, "name"), true) }.build()
        deleteNodeNameArg.addChild(deleteNodeNameArgConfirmArg)


        val listNode = literal("list").executes(::list).build()
        settlementNode.addChild(listNode)


        val infoNode = literal("info").build()
        settlementNode.addChild(infoNode)
        val infoNodeNameArg = settlementArg("name")
            .executes { info(it, SettlementArgumentType.getSettlement(it, "name")) }.build()
        infoNode.addChild(infoNodeNameArg)


        val claimNode = literal("claim").executes(::claim).build()
        settlementNode.addChild(claimNode)
        val claimNodeNameArg = settlementArg("name")
            .executes { claim(it, SettlementArgumentType.getSettlement(it, "name")) }.build()
        claimNode.addChild(claimNodeNameArg)


        val desertNode = literal("desert").build()
        settlementNode.addChild(desertNode)
        val desertNodeNameArg = settlementArg("name")
            .executes { desert(it, SettlementArgumentType.getSettlement(it, "name")) }.build()
        desertNode.addChild(desertNodeNameArg)


        val inviteNode = literal("invite").build()
        settlementNode.addChild(inviteNode)
        val inviteNodePlayerArg = argument("name", GameProfileArgumentType.gameProfile())
            .executes { invite(it, GameProfileArgumentType.getProfileArgument(it, "name")) }.build()
        inviteNode.addChild(inviteNodePlayerArg)
        val inviteAcceptNode = literal("accept").executes(::acceptInvite).build()
        inviteNode.addChild(inviteAcceptNode)

        val joinNode = literal("join").build()
        settlementNode.addChild(joinNode)
        val joinNodeSetlArg = settlementArg("name")
            .executes { join(it, SettlementArgumentType.getSettlement(it, "name")) }.build()
        joinNode.addChild(joinNodeSetlArg)


        val kickNode = literal("kick").build()
        settlementNode.addChild(kickNode)
        val kickNodePlayerArg = argument("name", GameProfileArgumentType.gameProfile())
            .executes { kick(it, GameProfileArgumentType.getProfileArgument(it, "name")) }.build()
        kickNode.addChild(kickNodePlayerArg)

        val leaveNode = literal("leave").executes { leave(it, null) }.build()
        settlementNode.addChild(leaveNode)
        val leaveNodeSetlArg = settlementArg("name")
            .executes { leave(it, SettlementArgumentType.getSettlement(it, "name")) }.build()
        leaveNode.addChild(leaveNodeSetlArg)


        val menuNode = literal("menu").executes(::menu).build()
        settlementNode.addChild(menuNode)


        if (CivilizationConfig.config().haveSetlAlias) {
            val setlNode = literal("setl").redirect(settlementNode).build()
            dispatcher.root.addChild(setlNode)
        }
    }

    private fun createSettlement(c: CommandContext<ServerCommandSource>, name: Text): Int {
        val src = c.source
        val world = src.world
        val player = src.player ?: return src.playerOnly()
        val results = SettlementManager.addSettlement(
            name.string, player, world.getChunk(player.blockPos).pos, player.blockPos, world.registryKey.value
        )
        if (results.first.didFail()) return src.endError(results.second)

        src.sendSystemMessage(results.second)

        return 1
    }

    private fun deleteSettlement(
        c: CommandContext<ServerCommandSource>, settlement: Settlement, confirm: Boolean
    ): Int {
        val src = c.source
        val player = src.player ?: return src.playerOnly()
        val results = SettlementManager.removeSettlement(settlement, player, confirm)

        return when (results.first) {
            ResultType.FAIL -> src.endError(results.second)
            ResultType.LOGIC -> {
                src.sendSystemMessage(results.second)
                src.endMsg(tTxt("To delete write /settlement delete confirm")
                    .styled {
                        it.withFormatting(Formatting.GRAY, Formatting.ITALIC)
                            .withClickEvent(
                                ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/settlement delete confirm")
                            )
                            .withHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT, tTxt("Click to run!")))
                    }
                )
            }

            ResultType.SUCCESS -> src.endMsg(results.second)
        }
    }

    private fun list(c: CommandContext<ServerCommandSource>): Int {
        val src = c.source
        val settlements = SettlementManager.getAllSettlement()

        if (settlements.isEmpty()) return src.endMsg("No settlements exists!")

        c.source.sendSystemMessage(tTxt("Settlements:"))
        for (setl in settlements) src.sendSystemMessage(lTxt(" - ${setl.name}"))

        return 1
    }

    private fun info(c: CommandContext<ServerCommandSource>, settlement: Settlement): Int {
        c.source.sendSystemMessage(tTxt("TEST:"))
        c.source.sendSystemMessage(tTxt(settlement.toString()))

        return 1
    }

    private fun claim(c: CommandContext<ServerCommandSource>): Int = claim(c, null)
    private fun claim(c: CommandContext<ServerCommandSource>, settlement: Settlement?): Int {
        val src = c.source
        val world = src.world
        val player = src.player ?: return src.playerOnly()

        val validSettlement = settlement ?: player.getSettlements()?.first()
        if (validSettlement == null) return src.endMsg("You are not the leader of a Settlement!")


        val results = SettlementManager.addChunk(validSettlement, world.getChunk(player.blockPos).pos)

        if (results.first.didFail())
            return src.endError(results.second)

        src.sendSystemMessage(results.second)

        return 1
    }

    private fun desert(c: CommandContext<ServerCommandSource>, settlement: Settlement): Int {
        val src = c.source
        val world = src.world
        val player = src.player ?: return src.playerOnly()

        val results = SettlementManager.removeChunk(settlement, world.getChunk(player.blockPos).pos)

        if (results.first.didFail())
            return src.endError(results.second)

        src.sendSystemMessage(results.second)

        return 1
    }

    private fun invite(c: CommandContext<ServerCommandSource>, gameProfiles: Collection<GameProfile>): Int {
        val src = c.source
        val player = src.player ?: return src.playerOnly()
        var count = 0

        val settlement = player.getSettlements()?.first() ?: return src.notInSettlement()

        for (it in gameProfiles) {
            if (it.id == player.uuid) continue
            SettlementManager.addInvites(it.id, settlement)
            src.server.playerManager.getPlayer(it.id)?.sendSystemMessage(
                tTxt("You have been invited to join %s settlement by %s", settlement.name, player.name)
            )
            count++
        }
        src.sendSystemMessage(
            if (gameProfiles.size > 1) tTxt("An invite has been sent to %s players!", count)
            else tTxt("An invite has been sent to %s!", gameProfiles.first().name)
        )

        return 1
    }

    private fun acceptInvite(c: CommandContext<ServerCommandSource>): Int {
        val src = c.source
        val player = src.player ?: return src.playerOnly()

        val invites = SettlementManager.getInvites(player.uuid) ?: return src.endMsg("No pending invites!")

        val setl = invites.first()
        SettlementManager.addCitizen(player, setl)
        SettlementManager.clearInvites(player.uuid)
        src.sendSystemMessage(tTxt("You have joined %s settlement!", setl.name))

        return 1
    }

    private fun kick(c: CommandContext<ServerCommandSource>, gameProfiles: Collection<GameProfile>): Int {
        val src = c.source
        val player = src.player ?: return src.playerOnly()
        var count = 0

        val settlement = player.getSettlements()?.first() ?: return src.notInSettlement()

        for (it in gameProfiles) {
            val kickedPlayer = src.server.playerManager.getPlayer(it.id) ?: continue
            SettlementManager.removeCitizen(kickedPlayer, settlement)
            count++
        }
        src.sendSystemMessage(
            if (gameProfiles.size > 1) tTxt("%s players have been kicked from the Settlement!", count)
            else tTxt("%s has been kicked from the Settlement", gameProfiles.first().name)
        )

        return 1
    }


    private fun leave(c: CommandContext<ServerCommandSource>, settlement: Settlement?): Int {
        val src = c.source
        val player = src.player ?: return src.playerOnly()

        val setl = settlement ?: player.getSettlements()?.first()
        if (setl == null) return src.notInSettlement()

        val role = player.getRole(setl)
            ?: return src.endMsg("You are not in a part of %s settlement!", setl.nameId)

        if (role == PlayerDataManager.Role.LEADER)
            return src.endMsg("You are the leader of the settlement! You cant leave! Run /settlement delete if you want to delete your settlement.")

        SettlementManager.removeCitizen(player, setl)
        src.sendSystemMessage(tTxt("You have left the %s settlement!", setl.name))

        return 1
    }


    private fun join(c: CommandContext<ServerCommandSource>, settlement: Settlement): Int {
        val src = c.source
        val player = src.player ?: return src.playerOnly()

        return when (settlement.joinPolicy) {
            Settlement.JoinPolicy.INVITE -> {
                val setl = SettlementManager.getInvite(player.uuid, settlement)
                    ?: return src.endMsg("You need to be invited to join this settlement!")

                SettlementManager.addCitizen(player, setl)
                SettlementManager.clearInvites(player.uuid)
                src.endMsg(tTxt("You have joined %s settlement!", setl.name))
            }

            Settlement.JoinPolicy.OPEN -> {
                SettlementManager.addCitizen(player, settlement)
                SettlementManager.clearInvites(player.uuid)
                src.endMsg(tTxt("You have joined %s settlement!", settlement.name))
            }

            Settlement.JoinPolicy.CLOSED -> src.endError("This settlement doesnt allow ppl to join right now!")
        }
    }

    private fun menu(c: CommandContext<ServerCommandSource>): Int {
        val src = c.source
        val world = src.world
        val player = src.player ?: return src.playerOnly()
        src.sendSystemMessage(tTxt("gui"))

        return 1
    }

    private fun ServerCommandSource.endMsg(text: String, vararg args: Any): Int {
        this.sendSystemMessage(tTxt(text, *args))
        return 0
    }

    private fun ServerCommandSource.endMsg(text: Text): Int {
        this.sendSystemMessage(text)
        return 0
    }

    private fun ServerCommandSource.endError(text: String, vararg args: Any): Int {
        this.sendError(tTxt(text, *args))
        return 0
    }

    private fun ServerCommandSource.endError(text: Text): Int {
        this.sendError(text)
        return 0
    }

    private fun ServerCommandSource.notInSettlement(): Int = this.endError("You are not in a settlement!")


    private fun ServerCommandSource.playerOnly(): Int = this.endError("This command can only be run by a player!")

}