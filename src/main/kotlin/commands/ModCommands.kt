package com.yourname.modid.commands

import com.yourname.modid.pathfinding.PathFinder
import com.yourname.modid.pathfinding.PathExecutor
import com.yourname.modid.utils.PathUtils
import net.minecraft.client.Minecraft
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumChatFormatting
import net.minecraftforge.client.event.ClientChatReceivedEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object ModCommands {
    private lateinit var pathFinder: PathFinder
    private lateinit var pathExecutor: PathExecutor
    private val mc = Minecraft.getMinecraft()

    fun initialize(pathFinder: PathFinder, pathExecutor: PathExecutor) {
        this.pathFinder = pathFinder
        this.pathExecutor = pathExecutor
        println("ModCommands initialized")
    }

    // Simple method to process commands from anywhere
    fun handleRawCommand(text: String) {
        // First check if the command starts with *
        if (!text.startsWith("*")) return

        // Remove the * and split the command
        val commandText = text.substring(1).trim()
        val parts = commandText.split(" ")

        if (parts.isEmpty()) return

        val command = parts[0].toLowerCase()
        val args = parts.drop(1)

        when (command) {
            "goto" -> {
                if (args.size == 3) {
                    try {
                        val x = args[0].toInt()
                        val y = args[1].toInt()
                        val z = args[2].toInt()
                        navigateToCoordinates(x, y, z)
                    } catch (e: NumberFormatException) {
                        PathUtils.sendMessage("Invalid coordinates! Use numbers.", EnumChatFormatting.RED)
                    }
                } else {
                    PathUtils.sendMessage("Usage: *goto <x> <y> <z>", EnumChatFormatting.RED)
                }
            }
            "stop" -> {
                if (pathExecutor.isExecutingPath()) {
                    pathExecutor.stopPath()
                    PathUtils.sendMessage("Navigation stopped", EnumChatFormatting.YELLOW)
                } else {
                    PathUtils.sendMessage("No navigation in progress", EnumChatFormatting.YELLOW)
                }
            }
            "help" -> {
                PathUtils.sendMessage("=== Navigation Commands ===", EnumChatFormatting.GREEN)
                PathUtils.sendMessage("*goto <x> <y> <z> - Navigate to coordinates", EnumChatFormatting.WHITE)
                PathUtils.sendMessage("*stop - Stop navigation", EnumChatFormatting.WHITE)
                PathUtils.sendMessage("*help - Show this help", EnumChatFormatting.WHITE)
            }
            else -> {
                PathUtils.sendMessage("Unknown command. Try *help", EnumChatFormatting.RED)
            }
        }
    }

    private fun navigateToCoordinates(x: Int, y: Int, z: Int) {
        val targetPos = BlockPos(x, y, z)
        val startPos = PathUtils.getPlayerPos()

        PathUtils.sendMessage("Finding path to ($x, $y, $z)...", EnumChatFormatting.GREEN)

        // Find path
        val path = pathFinder.findPath(startPos, targetPos)

        if (path.isEmpty()) {
            PathUtils.sendMessage("No path found to destination!", EnumChatFormatting.RED)
        } else {
            PathUtils.sendMessage("Found path with ${path.size} nodes", EnumChatFormatting.GREEN)
            pathExecutor.startPath(path)
        }
    }
}