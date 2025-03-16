package com.yourname.modid

import com.yourname.modid.pathfinding.PathFinder
import com.yourname.modid.pathfinding.PathExecutor
import com.yourname.modid.rendering.PathRenderer
import com.yourname.modid.utils.PathUtils
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiChat
import net.minecraft.client.settings.KeyBinding
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumChatFormatting
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.client.registry.ClientRegistry
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.event.FMLInitializationEvent
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.InputEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import org.lwjgl.input.Keyboard

@Mod(modid = "mymodid", name = "My First Mod", version = "1.0")
class MyMod {
    private val pathFinder = PathFinder()
    private val pathExecutor = PathExecutor()

    // Define keybinding for opening navigation command
    private val keyNavCommand = KeyBinding("Navigation Command", Keyboard.KEY_N, "Navigation Mod")

    // For tracking and processing commands
    private var checkingCommand = false
    private var lastChatInput = ""

    @Mod.EventHandler
    fun preInit(event: FMLPreInitializationEvent) {
        println("Pre-initializing my mod!!!!!!")
    }

    @Mod.EventHandler
    fun init(event: FMLInitializationEvent) {
        println("Initializing my mod!")

        // Register keybinding
        ClientRegistry.registerKeyBinding(keyNavCommand)

        // Create and register path renderer
        val pathRenderer = PathRenderer(pathExecutor)
        MinecraftForge.EVENT_BUS.register(pathRenderer)

        // Register event handlers
        MinecraftForge.EVENT_BUS.register(this)
    }

    @SubscribeEvent
    fun onKeyInput(event: InputEvent.KeyInputEvent) {
        // Check if the navigation command key was pressed
        if (keyNavCommand.isPressed) {
            // Open chat with the command prefix
            Minecraft.getMinecraft().displayGuiScreen(GuiChat("*goto "))
            checkingCommand = true
        }
    }

    @SubscribeEvent
    fun onClientTick(event: TickEvent.ClientTickEvent) {
        if (event.phase == TickEvent.Phase.START) {
            // Update path execution
            pathExecutor.onTick(event)

            val mc = Minecraft.getMinecraft()

            // If we were checking for a command and the chat is now closed
            if (checkingCommand && mc.currentScreen == null) {
                checkingCommand = false

                // Check if the chat was closed using Enter (submit) vs Escape (cancel)
                if (lastChatInput.startsWith("*")) {
                    // Process the command
                    processCommand(lastChatInput)
                }
            }

            // Update the last chat input if the chat is open
            if (mc.currentScreen is GuiChat) {
                val chat = mc.currentScreen as GuiChat
                try {
                    // This is a bit of a hack, but we're trying to get the text from the input field
                    val field = chat.javaClass.getDeclaredField("inputField")
                    field.isAccessible = true
                    val inputField = field.get(chat)
                    val textField = inputField.javaClass.getDeclaredMethod("getText")
                    lastChatInput = textField.invoke(inputField) as String
                } catch (e: Exception) {
                    // If we can't access the field, just ignore it
                }
            }
        }
    }

    private fun processCommand(message: String) {
        // Remove the * prefix
        val commandText = message.substring(1).trim()
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