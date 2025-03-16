package com.yourname.modid.pathfinding

import net.minecraft.client.Minecraft
import net.minecraft.client.settings.KeyBinding
import net.minecraft.util.BlockPos
import net.minecraft.util.MathHelper
import net.minecraft.util.Vec3
import net.minecraftforge.fml.common.gameevent.TickEvent
import kotlin.math.atan2

class PathExecutor {
    private val mc = Minecraft.getMinecraft()
    private var path: List<BlockPos> = emptyList()
    private var currentIndex = 0
    private var isExecuting = false

    // Input controls
    private val forwardKey: KeyBinding = mc.gameSettings.keyBindForward
    private val backKey: KeyBinding = mc.gameSettings.keyBindBack
    private val leftKey: KeyBinding = mc.gameSettings.keyBindLeft
    private val rightKey: KeyBinding = mc.gameSettings.keyBindRight
    private val jumpKey: KeyBinding = mc.gameSettings.keyBindJump

    /**
     * Start following a path
     * @param newPath The path to follow
     */
    fun startPath(newPath: List<BlockPos>) {
        path = newPath
        currentIndex = 0
        isExecuting = true

        // Release all keys when starting a new path
        releaseAllKeys()
    }

    /**
     * Stop following the current path
     */
    fun stopPath() {
        isExecuting = false
        releaseAllKeys()
    }

    /**
     * Check if currently executing a path
     */
    fun isExecutingPath(): Boolean {
        return isExecuting
    }

    /**
     * Get the current path (for rendering)
     */
    fun getCurrentPath(): List<BlockPos>? {
        return if (isExecuting) path else null
    }

    /**
     * Get the current target node (for highlighting)
     */
    fun getCurrentTarget(): BlockPos? {
        return if (isExecuting && currentIndex < path.size) path[currentIndex] else null
    }

    /**
     * Handle tick event to progress along the path
     * Call this method from your mod's tick handler
     */
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (!isExecuting || path.isEmpty() || mc.thePlayer == null) return

        // Skip if we're at the end of the path
        if (currentIndex >= path.size) {
            stopPath()
            return
        }

        // Get current target position
        val targetPos = path[currentIndex]
        val targetVec = Vec3(
            targetPos.x.toDouble() + 0.5,
            targetPos.y.toDouble(),
            targetPos.z.toDouble() + 0.5
        )

        // Get player position
        val playerPos = mc.thePlayer.positionVector

        // Check if we've reached the current waypoint
        val reachDistance = 0.5
        if (playerPos.distanceTo(targetVec) < reachDistance) {
            currentIndex++
            if (currentIndex >= path.size) {
                stopPath()
                return
            }
        }

        // Calculate direction to target
        moveTowardsTarget(targetVec)
    }

    /**
     * Move the player towards the target position
     */
    private fun moveTowardsTarget(targetVec: Vec3) {
        val player = mc.thePlayer

        // Calculate horizontal direction to target
        val dx = targetVec.xCoord - player.posX
        val dz = targetVec.zCoord - player.posZ

        // Calculate yaw to target (in degrees)
        val yawToTarget = MathHelper.wrapAngleTo180_float(
            (atan2(dz, dx) * 180.0 / Math.PI).toFloat() - 90f
        )

        // Set player's rotation
        player.rotationYaw = yawToTarget

        // Handle vertical movement (jumping or falling)
        val targetY = targetVec.yCoord
        val playerY = player.posY

        if (targetY > playerY + 0.5) {
            // Need to jump
            KeyBinding.setKeyBindState(jumpKey.keyCode, true)
        } else {
            KeyBinding.setKeyBindState(jumpKey.keyCode, false)
        }

        // Move forward
        KeyBinding.setKeyBindState(forwardKey.keyCode, true)
        KeyBinding.setKeyBindState(backKey.keyCode, false)
        KeyBinding.setKeyBindState(leftKey.keyCode, false)
        KeyBinding.setKeyBindState(rightKey.keyCode, false)
    }

    /**
     * Release all movement keys
     */
    private fun releaseAllKeys() {
        KeyBinding.setKeyBindState(forwardKey.keyCode, false)
        KeyBinding.setKeyBindState(backKey.keyCode, false)
        KeyBinding.setKeyBindState(leftKey.keyCode, false)
        KeyBinding.setKeyBindState(rightKey.keyCode, false)
        KeyBinding.setKeyBindState(jumpKey.keyCode, false)
    }
}