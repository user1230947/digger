package com.yourname.modid.rendering

import com.yourname.modid.pathfinding.PathExecutor
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.WorldRenderer
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.util.BlockPos
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import org.lwjgl.opengl.GL11
import java.awt.Color

class PathRenderer(private val pathExecutor: PathExecutor) {

    @SubscribeEvent
    fun onRenderWorldLast(event: RenderWorldLastEvent) {
        // Only render if there's an active path
        if (!pathExecutor.isExecutingPath()) return

        val path = pathExecutor.getCurrentPath() ?: return
        if (path.isEmpty()) return

        val mc = Minecraft.getMinecraft()
        val player = mc.thePlayer

        // Get player's position for rendering offset
        val playerX = player.lastTickPosX + (player.posX - player.lastTickPosX) * event.partialTicks
        val playerY = player.lastTickPosY + (player.posY - player.lastTickPosY) * event.partialTicks
        val playerZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * event.partialTicks

        // Setup rendering
        GlStateManager.pushMatrix()
        GlStateManager.disableTexture2D()
        GlStateManager.disableLighting()
        GlStateManager.disableCull()
        GlStateManager.disableDepth()
        GlStateManager.enableBlend()
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)

        // Translate to player position
        GlStateManager.translate(-playerX, -playerY, -playerZ)

        // Draw path nodes as boxes
        drawPathNodes(path)

        // Draw lines connecting the nodes
        drawPathLines(path)

        // Restore rendering state
        GlStateManager.enableDepth()
        GlStateManager.enableCull()
        GlStateManager.enableLighting()
        GlStateManager.enableTexture2D()
        GlStateManager.disableBlend()
        GlStateManager.popMatrix()
    }

    private fun drawPathNodes(path: List<BlockPos>) {
        val tessellator = net.minecraft.client.renderer.Tessellator.getInstance()
        val worldRenderer = tessellator.worldRenderer

        // For each node in the path
        for (pos in path) {
            // Different color for current target node
            val color = if (pos == pathExecutor.getCurrentTarget()) {
                Color(255, 0, 0, 128) // Red for current target
            } else {
                Color(0, 255, 255, 128) // Cyan for other nodes
            }

            // Draw a cube marker at the position
            drawCubeMarker(worldRenderer, pos, 0.4, color)
        }
    }

    private fun drawPathLines(path: List<BlockPos>) {
        if (path.size < 2) return

        val tessellator = net.minecraft.client.renderer.Tessellator.getInstance()
        val worldRenderer = tessellator.worldRenderer

        // Set line width
        GL11.glLineWidth(3.0f)

        // Draw lines connecting nodes
        worldRenderer.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION_COLOR)

        // Add each node to the line
        for (pos in path) {
            worldRenderer.pos(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5)
                .color(255, 255, 0, 200) // Yellow lines
                .endVertex()
        }

        tessellator.draw()
    }

    private fun drawCubeMarker(worldRenderer: WorldRenderer, pos: BlockPos, size: Double, color: Color) {
        val x = pos.x.toDouble()
        val y = pos.y.toDouble()
        val z = pos.z.toDouble()
        val halfSize = size / 2.0

        // Calculate cube vertices
        val minX = x + 0.5 - halfSize
        val minY = y + 0.1 // Slightly above ground
        val minZ = z + 0.5 - halfSize
        val maxX = x + 0.5 + halfSize
        val maxY = minY + size
        val maxZ = z + 0.5 + halfSize

        // Start drawing the cube (6 faces)
        worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR)

        // Bottom face
        worldRenderer.pos(minX, minY, minZ).color(color.red, color.green, color.blue, color.alpha).endVertex()
        worldRenderer.pos(maxX, minY, minZ).color(color.red, color.green, color.blue, color.alpha).endVertex()
        worldRenderer.pos(maxX, minY, maxZ).color(color.red, color.green, color.blue, color.alpha).endVertex()
        worldRenderer.pos(minX, minY, maxZ).color(color.red, color.green, color.blue, color.alpha).endVertex()

        // Top face
        worldRenderer.pos(minX, maxY, minZ).color(color.red, color.green, color.blue, color.alpha).endVertex()
        worldRenderer.pos(minX, maxY, maxZ).color(color.red, color.green, color.blue, color.alpha).endVertex()
        worldRenderer.pos(maxX, maxY, maxZ).color(color.red, color.green, color.blue, color.alpha).endVertex()
        worldRenderer.pos(maxX, maxY, minZ).color(color.red, color.green, color.blue, color.alpha).endVertex()

        // Front face
        worldRenderer.pos(minX, minY, minZ).color(color.red, color.green, color.blue, color.alpha).endVertex()
        worldRenderer.pos(minX, maxY, minZ).color(color.red, color.green, color.blue, color.alpha).endVertex()
        worldRenderer.pos(maxX, maxY, minZ).color(color.red, color.green, color.blue, color.alpha).endVertex()
        worldRenderer.pos(maxX, minY, minZ).color(color.red, color.green, color.blue, color.alpha).endVertex()

        // Back face
        worldRenderer.pos(minX, minY, maxZ).color(color.red, color.green, color.blue, color.alpha).endVertex()
        worldRenderer.pos(maxX, minY, maxZ).color(color.red, color.green, color.blue, color.alpha).endVertex()
        worldRenderer.pos(maxX, maxY, maxZ).color(color.red, color.green, color.blue, color.alpha).endVertex()
        worldRenderer.pos(minX, maxY, maxZ).color(color.red, color.green, color.blue, color.alpha).endVertex()

        // Left face
        worldRenderer.pos(minX, minY, minZ).color(color.red, color.green, color.blue, color.alpha).endVertex()
        worldRenderer.pos(minX, minY, maxZ).color(color.red, color.green, color.blue, color.alpha).endVertex()
        worldRenderer.pos(minX, maxY, maxZ).color(color.red, color.green, color.blue, color.alpha).endVertex()
        worldRenderer.pos(minX, maxY, minZ).color(color.red, color.green, color.blue, color.alpha).endVertex()

        // Right face
        worldRenderer.pos(maxX, minY, minZ).color(color.red, color.green, color.blue, color.alpha).endVertex()
        worldRenderer.pos(maxX, maxY, minZ).color(color.red, color.green, color.blue, color.alpha).endVertex()
        worldRenderer.pos(maxX, maxY, maxZ).color(color.red, color.green, color.blue, color.alpha).endVertex()
        worldRenderer.pos(maxX, minY, maxZ).color(color.red, color.green, color.blue, color.alpha).endVertex()

        net.minecraft.client.renderer.Tessellator.getInstance().draw()
    }
}