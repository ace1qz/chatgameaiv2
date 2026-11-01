package chatgameai;

import chatgameai.core.*;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;

import java.nio.file.Path;
import java.util.concurrent.*;

/** Thin Fabric glue (Mojang mappings, MC 1.21.11). All solving logic lives in chatgameai.core. */
public class ChatGameAIClient implements ClientModInitializer {
    private static final ScheduledExecutorService EXEC = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "ChatGameAI");
        t.setDaemon(true);
        return t;
    });
    private Config cfg;
    private ChatGameEngine engine;

    @Override
    public void onInitializeClient() {
        Path conf = FabricLoader.getInstance().getConfigDir();
        cfg = Config.load(conf.resolve("chatgameai.properties"), conf.resolve("chatgameai"));
        engine = ChatGameEngine.createDefault(cfg);

        // Server/plugin broadcasts (where chat games usually arrive) come in as game messages.
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!overlay) handle(message.getString());
        });
        // Lets the guess-the-number solver learn numbers you type yourself.
        ClientSendMessageEvents.CHAT.register(message -> engine.noteOutgoing(message));
        if (cfg.listenToPlayerChat) {
            ClientReceiveMessageEvents.CHAT.register((message, signed, sender, params, ts) -> handle(message.getString()));
        }

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
            dispatcher.register(ClientCommandManager.literal("chatgameai")
                .then(ClientCommandManager.literal("toggle").executes(c -> {
                    cfg.enabled = !cfg.enabled;
                    c.getSource().sendFeedback(Component.literal("ChatGameAI " + (cfg.enabled ? "enabled" : "disabled")));
                    return 1;
                }))
                .then(ClientCommandManager.literal("threshold")
                    .then(ClientCommandManager.argument("value", DoubleArgumentType.doubleArg(0.5, 1.0)).executes(c -> {
                        cfg.threshold = DoubleArgumentType.getDouble(c, "value");
                        c.getSource().sendFeedback(Component.literal("ChatGameAI threshold = " + cfg.threshold));
                        return 1;
                    })))));
    }

    private void handle(String raw) {
        if (!cfg.enabled) return;
        Minecraft mc = Minecraft.getInstance();
        ServerData server = mc.getCurrentServer();
        String host = server == null ? null : server.ip.split(":")[0];
        if (host != null && !cfg.allows(host)) return;

        Decision d = engine.evaluate(raw);
        switch (d.action()) {
            case SEND -> {
                int delay = ThreadLocalRandom.current().nextInt(cfg.minDelayMs, cfg.maxDelayMs + 1);
                String answer = d.candidate().answer();
                EXEC.schedule(() -> mc.execute(() -> {
                    ClientPacketListener conn = mc.getConnection();
                    if (conn != null) conn.sendChat(answer);
                }), delay, TimeUnit.MILLISECONDS);
            }
            case SUGGEST -> mc.execute(() -> {
                if (mc.player != null) mc.player.displayClientMessage(Component.literal(String.format(
                    "[ChatGameAI] unsure: \"%s\" (%.0f%%, %s)", d.candidate().answer(),
                    d.confidence() * 100, d.candidate().solver())).withStyle(ChatFormatting.GRAY), false);
            });
            default -> {}
        }
    }
}
