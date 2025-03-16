package com.yourname.modid.pathfinding

import net.minecraft.client.Minecraft
import net.minecraft.client.settings.KeyBinding
import net.minecraft.util.BlockPos
import net.minecraft.util.MathHelper
import net.minecraft.util.Vec3
import net.minecraftforge.fml.common.gameevent.TickEvent
import kotlin.math.abs
import kotlin.math.atan2

class PathExecutor {
    private val mc = Minecraft.getMinecraft()
    private var path: List<BlockPos> = emptyList()
    private var currentIndex = 0
    private var isExecuting = false
    private var jumpCooldown = 0

    // Input controls
    private val forwardKey: KeyBinding = mc.gameSettings.keyBindForward
    private val backKey: KeyBinding = mc.gameSettings.keyBindBack
    private val leftKey: KeyBinding = mc.gameSettings.keyBindLeft
    private val rightKey: KeyBinding = mc.gameSettings.keyBindRight
    private val jumpKey: KeyBinding = mc.gameSettings.keyBindJump
    private val sneakKey: KeyBinding = mc.gameSettings.keyBindSneak

    /**
     * Start following a path
     * @param newPath The path to follow
     */
    fun startPath(newPath: List<BlockPos>) {
        path = newPath
        currentIndex = 0
        isExecuting = true
        jumpCooldown = 0

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
            jumpCooldown = 0
            if (currentIndex >= path.size) {
                stopPath()
                return
            }
        }

        // Move toward the target
        moveTowardsTarget(targetVec)

        // Decrement jump cooldown
        if (jumpCooldown > 0) {
            jumpCooldown--
        }
    }

    /**
     * Move the player towards the target position
     * Handles diagonal movement and vertical traversal
     */
    private fun moveTowardsTarget(targetVec: Vec3) {
        val player = mc.thePlayer
        val playerPos = player.positionVector

        // Calculate horizontal direction to target
        val dx = targetVec.xCoord - player.posX
        val dz = targetVec.zCoord - player.posZ
        val dy = targetVec.yCoord - player.posY

        // Calculate yaw to target (in degrees)
        val yawToTarget = MathHelper.wrapAngleTo180_float(
            (atan2(dz, dx) * 180.0 / Math.PI).toFloat() - 90f
        )

        // Set player's rotation
        player.rotationYaw = yawToTarget

        // Handle vertical movement (jumping or falling)
        if (dy > 0.1 && jumpCooldown == 0) {
            // Need to jump
            KeyBinding.setKeyBindState(jumpKey.keyCode, true)
            jumpCooldown = 10 // Add cooldown to prevent spam jumping
        } else {
            KeyBinding.setKeyBindState(jumpKey.keyCode, false)
        }

        // Special handling for dropping down
        val isDescending = dy < -0.5
        if (isDescending) {
            // When descending, we might need to release forward key momentarily
            // to prevent getting stuck on ledges
            val isAtEdge = isPlayerAtEdge()
            if (isAtEdge) {
                // At an edge, careful movement
                KeyBinding.setKeyBindState(forwardKey.keyCode, true)
                KeyBinding.setKeyBindState(sneakKey.keyCode, true)
            } else {
                KeyBinding.setKeyBindState(forwardKey.keyCode, true)
                KeyBinding.setKeyBindState(sneakKey.keyCode, false)
            }
        } else {
            KeyBinding.setKeyBindState(sneakKey.keyCode, false)
            KeyBinding.setKeyBindState(forwardKey.keyCode, true)
        }

        // No need for left/right strafing if we're directly adjusting player yaw
        KeyBinding.setKeyBindState(leftKey.keyCode, false)
        KeyBinding.setKeyBindState(rightKey.keyCode, false)
        KeyBinding.setKeyBindState(backKey.keyCode, false)
    }

    /**
     * Check if the player is standing at an edge (useful for safe descending)
     */
    private fun isPlayerAtEdge(): Boolean {
        val player = mc.thePlayer
        val world = mc.theWorld

        val playerPos = BlockPos(player.posX, player.posY - 0.1, player.posZ)

        // Get player's looking direction (rounded to nearest cardinal direction)
        val yaw = MathHelper.wrapAngleTo180_float(player.rotationYaw)

        // Get the block in front of the player based on direction
        val frontPos = when {
            yaw > -45 && yaw <= 45 -> playerPos.south() // South
            yaw > 45 && yaw <= 135 -> playerPos.west() // West
            yaw > 135 || yaw <= -135 -> playerPos.north() // North
            else -> playerPos.east() // East
        }

        // Check if there's no solid block below the front position
        return !world.getBlockState(frontPos.down()).block.isBlockNormalCube
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
        KeyBinding.setKeyBindState(sneakKey.keyCode, false)
    }
}