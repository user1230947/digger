package com.yourname.modid.pathfinding

import net.minecraft.client.Minecraft
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import net.minecraft.block.BlockSlab
import net.minecraft.block.BlockStairs
import java.util.*
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Improved A* pathfinding algorithm implementation for Minecraft
 * - Supports diagonal movement
 * - Handles vertical traversal (slabs, stairs, 1-block jumps)
 */
class PathFinder {
    private val mc = Minecraft.getMinecraft()
    private val openSet = PriorityQueue<Node>(compareBy { it.fCost })
    private val closedSet = HashSet<BlockPos>()

    /**
     * Find a path from start to end position
     * @param start Starting position
     * @param end Target position
     * @return List of positions representing the path, or empty list if no path found
     */
    fun findPath(start: BlockPos, end: BlockPos): List<BlockPos> {
        // Reset the sets
        openSet.clear()
        closedSet.clear()

        // Add the starting node
        openSet.add(Node(start, null, 0.0, heuristic(start, end)))

        // Main A* loop
        while (openSet.isNotEmpty()) {
            val current = openSet.poll()

            // If we've reached the destination
            if (current.pos == end) {
                return reconstructPath(current)
            }

            closedSet.add(current.pos)

            // Check each neighbor
            for (neighbor in getNeighbors(current.pos)) {
                // Skip if we've already processed this node
                if (neighbor in closedSet) continue

                // Calculate new path cost
                val moveCost = if (isDiagonal(current.pos, neighbor)) 1.414 else 1.0 // √2 for diagonal
                val gCost = current.gCost + moveCost

                // Check if this is a better path or new node
                val existingNode = openSet.find { it.pos == neighbor }
                if (existingNode == null) {
                    // New node, add it to the open set
                    openSet.add(Node(neighbor, current, gCost, heuristic(neighbor, end)))
                } else if (gCost < existingNode.gCost) {
                    // Better path, update existing node
                    openSet.remove(existingNode)
                    openSet.add(Node(neighbor, current, gCost, heuristic(neighbor, end)))
                }
            }
        }

        // No path found
        return emptyList()
    }

    /**
     * Check if movement between two positions is diagonal
     */
    private fun isDiagonal(from: BlockPos, to: BlockPos): Boolean {
        val dx = abs(to.x - from.x)
        val dz = abs(to.z - from.z)
        return dx == 1 && dz == 1
    }

    /**
     * Get valid neighboring blocks, including diagonals and verticals
     */
    private fun getNeighbors(pos: BlockPos): List<BlockPos> {
        val neighbors = mutableListOf<BlockPos>()
        val world = mc.theWorld

        // Check horizontal and diagonal moves (8 directions)
        for (dx in -1..1) {
            for (dz in -1..1) {
                // Skip the center
                if (dx == 0 && dz == 0) continue

                // Check same level first
                val horizontalPos = pos.add(dx, 0, dz)

                // Only consider diagonal moves if the adjacent blocks are walkable
                // This prevents cutting corners
                if (dx != 0 && dz != 0) {
                    val corner1 = pos.add(dx, 0, 0)
                    val corner2 = pos.add(0, 0, dz)

                    if (!isPositionWalkable(corner1) || !isPositionWalkable(corner2)) {
                        continue
                    }
                }

                if (isPositionWalkable(horizontalPos)) {
                    neighbors.add(horizontalPos)
                }

                // Check step up (max 1 block up)
                val stepUpPos = pos.add(dx, 1, dz)
                if (canStepUp(pos, stepUpPos)) {
                    neighbors.add(stepUpPos)
                }

                // Check step down (max 1 block down)
                val stepDownPos = pos.add(dx, -1, dz)
                if (isPositionWalkable(stepDownPos)) {
                    neighbors.add(stepDownPos)
                }
            }
        }

        return neighbors
    }

    /**
     * Check if the player can step up to the target position
     * Handles slabs, stairs, and 1-block height differences
     */
    private fun canStepUp(currentPos: BlockPos, targetPos: BlockPos): Boolean {
        val world = mc.theWorld

        // Target must be air to be walkable
        if (!world.isAirBlock(targetPos)) {
            return false
        }

        // The block at current level
        val currentBlock = world.getBlockState(currentPos).block
        // The block we'd step up from
        val stepBlock = world.getBlockState(currentPos.up()).block
        // The block below the target
        val belowTargetBlock = world.getBlockState(targetPos.down()).block

        // Check for slabs and stairs at our current position
        val isOnSlab = currentBlock is BlockSlab || currentBlock is BlockStairs

        // Check if the block below target is solid
        val hasSupport = belowTargetBlock.isBlockNormalCube

        // Check for obstruction at head level (2 blocks up from current)
        val hasHeadroom = world.isAirBlock(currentPos.up(2))

        // Can step up if:
        // 1. We're on a slab/stair (only need to step up half a block), OR
        // 2. Normal 1-block jump is possible (has solid support and headroom)
        return (isOnSlab || (hasSupport && hasHeadroom))
    }

    /**
     * Check if a position is walkable
     */
    private fun isPositionWalkable(pos: BlockPos): Boolean {
        val world = mc.theWorld

        // Check if the block is air
        val isAir = world.isAirBlock(pos)

        // Check if the block below is solid
        val blockBelow = world.getBlockState(pos.down())
        val isSolid = blockBelow.block.isBlockNormalCube

        // Check if we have headroom
        val hasHeadroom = world.isAirBlock(pos.up())

        return isAir && isSolid && hasHeadroom
    }

    /**
     * Calculate Euclidean distance (heuristic)
     */
    private fun heuristic(from: BlockPos, to: BlockPos): Double {
        return distance(from, to)
    }

    /**
     * Calculate Euclidean distance between two positions
     */
    private fun distance(from: BlockPos, to: BlockPos): Double {
        val dx = to.x - from.x
        val dy = to.y - from.y
        val dz = to.z - from.z
        return sqrt((dx * dx + dy * dy + dz * dz).toDouble())
    }

    /**
     * Reconstruct the path from the end node
     */
    private fun reconstructPath(endNode: Node): List<BlockPos> {
        val path = mutableListOf<BlockPos>()
        var current: Node? = endNode

        while (current != null) {
            path.add(current.pos)
            current = current.parent
        }

        return path.reversed()
    }

    /**
     * Node class for A* algorithm
     */
    private data class Node(
        val pos: BlockPos,
        val parent: Node?,
        val gCost: Double,
        val hCost: Double
    ) {
        val fCost: Double get() = gCost + hCost
    }
}