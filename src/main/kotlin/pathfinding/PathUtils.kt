package com.yourname.modid.utils

import net.minecraft.client.Minecraft
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import net.minecraft.util.ChatComponentText
import net.minecraft.util.EnumChatFormatting

/**
 * Utility functions for pathfinding and automation
 */
object PathUtils {
    private val mc = Minecraft.getMinecraft()

    /**
     * Convert Vec3 to BlockPos
     */
    fun vecToBlockPos(vec: Vec3): BlockPos {
        return BlockPos(vec.xCoord, vec.yCoord, vec.zCoord)
    }

    /**
     * Convert BlockPos to Vec3 (centered on block)
     */
    fun blockPosToVec(pos: BlockPos): Vec3 {
        return Vec3(pos.x + 0.5, pos.y.toDouble(), pos.z + 0.5)
    }

    /**
     * Get the player's current position as BlockPos
     */
    fun getPlayerPos(): BlockPos {
        val player = mc.thePlayer
        return BlockPos(player.posX, player.posY, player.posZ)
    }

    /**
     * Send a message to the player's chat
     */
    fun sendMessage(message: String, color: EnumChatFormatting = EnumChatFormatting.WHITE) {
        val chatComponent = ChatComponentText(message)
        chatComponent.chatStyle.color = color
        mc.thePlayer?.addChatMessage(chatComponent)
    }

    /**
     * Format a path as a string for debugging
     */
    fun formatPath(path: List<BlockPos>): String {
        if (path.isEmpty()) return "Empty path"

        val sb = StringBuilder("Path: ")
        path.forEachIndexed { index, pos ->
            sb.append("(${pos.x}, ${pos.y}, ${pos.z})")
            if (index < path.size - 1) sb.append(" → ")
        }
        return sb.toString()
    }

    /**
     * Check if two block positions are adjacent (including diagonals)
     */
    fun areBlocksAdjacent(pos1: BlockPos, pos2: BlockPos): Boolean {
        val dx = Math.abs(pos1.x - pos2.x)
        val dy = Math.abs(pos1.y - pos2.y)
        val dz = Math.abs(pos1.z - pos2.z)

        // Adjacent if the maximum difference in any direction is 1
        return dx <= 1 && dy <= 1 && dz <= 1 && (dx + dy + dz > 0)
    }
}