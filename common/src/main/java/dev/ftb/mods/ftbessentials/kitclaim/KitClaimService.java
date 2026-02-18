package dev.ftb.mods.ftbessentials.kitclaim;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.logging.LogUtils;
import dev.ftb.mods.ftbessentials.kit.Kit;
import dev.ftb.mods.ftbessentials.kit.KitManager;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;

public final class KitClaimService {
    private static final Logger LOGGER = LogUtils.getLogger();

    private KitClaimService() {
    }

    public static int claimKit(CommandSourceStack source, String kitNameRaw) {
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception e) {
            LOGGER.warn("[KitClaim] Claim attempted by non-player source.");
            source.sendFailure(Component.literal("Only players can claim kits."));
            return 0;
        }

        String kitName = normalize(kitNameRaw);
        LOGGER.info("[KitClaim] Claim request: player={} rawKit={} normalized={}", player.getGameProfile().getName(), kitNameRaw, kitName);
        String resolvedKitName = resolveKitName(kitName);
        if (resolvedKitName == null) {
            LOGGER.warn("[KitClaim] Unknown kit: player={} kit={}", player.getGameProfile().getName(), kitName);
            source.sendFailure(Component.literal("Unknown kit.").withStyle(ChatFormatting.RED));
            return 0;
        }

        boolean isPublic = isPublic(kitName);
        boolean hasPerm = isPublic || hasKitPermission(player, kitName);
        LOGGER.info("[KitClaim] Access check: player={} kit={} isPublic={} hasPerm={}", player.getGameProfile().getName(), resolvedKitName, isPublic, hasPerm);

        if (!hasPerm) {
            source.sendFailure(Component.literal("No permission.").withStyle(ChatFormatting.RED));
            return 0;
        }

        try {
            KitManager.getInstance().giveKitToPlayer(resolvedKitName, player);
            LOGGER.info("[KitClaim] Kit granted: player={} kit={}", player.getGameProfile().getName(), resolvedKitName);
            source.sendSuccess(() -> Component.literal("Kit '" + resolvedKitName + "' claimed.").withStyle(ChatFormatting.YELLOW), false);
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            String msg = e.getMessage() == null ? "Kit claim failed." : e.getMessage();
            LOGGER.warn("[KitClaim] Kit grant failed: player={} kit={} error={}", player.getGameProfile().getName(), resolvedKitName, msg);
            source.sendFailure(Component.literal(msg).withStyle(ChatFormatting.RED));
            return 0;
        }
    }

    public static int listClaimableKits(CommandSourceStack source) {
        Set<String> kits = getClaimableKits(source);
        if (kits.isEmpty()) {
            LOGGER.info("[KitClaim] List kits: none available for source={}", source.getTextName());
            source.sendFailure(Component.literal("No kits available.").withStyle(ChatFormatting.RED));
            return 0;
        }
        LOGGER.info("[KitClaim] List kits: source={} kits={}", source.getTextName(), kits);
        source.sendSuccess(() -> Component.literal("Available kits: " + String.join(", ", kits)).withStyle(ChatFormatting.AQUA), false);
        return Command.SINGLE_SUCCESS;
    }

    public static CompletableFuture<Suggestions> suggestClaimableKits(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(getClaimableKits(ctx.getSource()), builder);
    }

    private static Set<String> getClaimableKits(CommandSourceStack source) {
        List<Kit> all = new ArrayList<>(KitManager.getInstance().allKits());
        Set<String> result = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

        ServerPlayer player = source.getEntity() instanceof ServerPlayer sp ? sp : null;
        for (Kit kit : all) {
            String kitName = kit.getKitName();
            String normalized = normalize(kitName);
            if (isPublic(normalized)) {
                result.add(kitName);
                continue;
            }
            if (player != null && hasKitPermission(player, normalized)) {
                result.add(kitName);
            }
        }

        return result;
    }

    private static boolean isPublic(String kitName) {
        return KitClaimConfig.get().publicKits().contains(kitName);
    }

    private static boolean hasKitPermission(ServerPlayer player, String kitName) {
        String perm = KitClaimConfig.get().kitPermissions().get(kitName);
        if (perm == null || perm.isBlank()) {
            return false;
        }
        return KitClaimPermissionUtil.hasPermission(player, perm);
    }

    private static String resolveKitName(String kitName) {
        if (KitManager.getInstance().get(kitName).isPresent()) {
            return kitName;
        }

        for (Kit kit : KitManager.getInstance().allKits()) {
            if (kit.getKitName().equalsIgnoreCase(kitName)) {
                return kit.getKitName();
            }
        }

        return null;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
