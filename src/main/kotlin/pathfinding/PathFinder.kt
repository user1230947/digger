package com.yourname.modid.pathfinding

import net.minecraft.client.Minecraft
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import java.util.*
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Basic A* pathfinding algorithm implementation for Minecraft
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
                val gCost = current.gCost + distance(current.pos, neighbor)

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
     * Get valid neighboring blocks
     */
    private fun getNeighbors(pos: BlockPos): List<BlockPos> {
        val neighbors = mutableListOf<BlockPos>()
        val world = mc.theWorld

        // Check 6 adjacent blocks (no diagonals for simplicity)
        val directions = listOf(
            BlockPos(0, 1, 0),  // Up
            BlockPos(0, -1, 0), // Down
            BlockPos(1, 0, 0),  // East
            BlockPos(-1, 0, 0), // West
            BlockPos(0, 0, 1),  // South
            BlockPos(0, 0, -1)  // North
        )

        for (dir in directions) {
            val neighborPos = pos.add(dir)

            // Check if the block is walkable
            val isWalkable = isPositionWalkable(neighborPos)
            if (isWalkable) {
                neighbors.add(neighborPos)
            }
        }

        return neighbors
    }

    /**
     * Check if a position is walkable (air block with solid block below)
     */
    private fun isPositionWalkable(pos: BlockPos): Boolean {
        val world = mc.theWorld

        // Check if the block is air
        val isAir = world.isAirBlock(pos)

        // Check if the block below is solid
        val blockBelow = world.getBlockState(pos.down())
        val isSolid = blockBelow.block.isBlockNormalCube

        return isAir && isSolid
    }

    /**
     * Calculate straight line distance (heuristic)
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