package net.mura.stack;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.brigadier.arguments.DoubleArgumentType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class stacker implements ClientModInitializer {
    private static boolean showStacks = true;
    private static double baseServerSpeed = 20.0;
    private static final Path CONFIG_FILE = Path.of("config", "stacks.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    // Config class for saving/loading
    private static class ModConfig {
        double baseServerSpeed = 20.0;
        boolean enabled = true;
    }
    
    @Override
    public void onInitializeClient() {
        // Load config when mod starts
        loadConfig();
        
        // Register commands
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("stacks")
                .then(ClientCommandManager.literal("toggle")
                    .executes(context -> {
                        showStacks = !showStacks;
                        saveConfig();
                        context.getSource().sendFeedback(Text.literal("Stack display: ").formatted(Formatting.WHITE)
                            .append(Text.literal(showStacks ? "ENABLED" : "DISABLED")
                            .formatted(showStacks ? Formatting.GREEN : Formatting.RED)));
                        return 1;
                    })
                )
                .then(ClientCommandManager.literal("base")
                    .then(ClientCommandManager.argument("value", DoubleArgumentType.doubleArg(0))
                        .executes(context -> {
                            baseServerSpeed = DoubleArgumentType.getDouble(context, "value");
                            saveConfig();
                            context.getSource().sendFeedback(Text.literal("Base speed set to: ")
                                .formatted(Formatting.WHITE)
                                .append(Text.literal(String.valueOf(baseServerSpeed)).formatted(Formatting.GOLD)));
                            return 1;
                        })
                    )
                )
            );
        });
        
        // Display stacks in action bar
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (showStacks && client.player != null && client.world != null) {
                float movementSpeed = client.player.getMovementSpeed();
                
                // Remove sprint multiplier if sprinting
                if (client.player.isSprinting()) {
                    movementSpeed = movementSpeed / 1.3f;
                }
                
                // Convert to server speed: (speed - 0.1) × 1000
                double serverSpeed = (movementSpeed - 0.1f) * 1000;
                int stacks = (int) Math.round(serverSpeed - baseServerSpeed);
                
                Text message = Text.literal("")
                    .append(Text.literal("⏩ ").formatted(Formatting.AQUA))
                    .append(Text.literal(String.valueOf(stacks)).formatted(Formatting.YELLOW))
                    .append(Text.literal(" stacks").formatted(Formatting.GRAY));
                
                client.player.sendMessage(message, true);
            }
        });
    }
    
    private void loadConfig() {
        try {
            if (Files.exists(CONFIG_FILE)) {
                String json = Files.readString(CONFIG_FILE);
                ModConfig config = GSON.fromJson(json, ModConfig.class);
                baseServerSpeed = config.baseServerSpeed;
                showStacks = config.enabled;
                System.out.println("Loaded config: base=" + baseServerSpeed + ", enabled=" + showStacks);
            }
        } catch (IOException e) {
            System.out.println("Failed to load config: " + e.getMessage());
        }
    }
    
    private void saveConfig() {
        try {
            ModConfig config = new ModConfig();
            config.baseServerSpeed = baseServerSpeed;
            config.enabled = showStacks;
            
            // Create config directory if it doesn't exist
            Files.createDirectories(CONFIG_FILE.getParent());
            
            // Write config to file
            String json = GSON.toJson(config);
            Files.writeString(CONFIG_FILE, json);
            System.out.println("Saved config: base=" + baseServerSpeed + ", enabled=" + showStacks);
        } catch (IOException e) {
            System.out.println("Failed to save config: " + e.getMessage());
        }
    }
}