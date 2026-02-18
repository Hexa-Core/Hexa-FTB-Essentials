package dev.ftb.mods.ftbessentials.kitclaim;

import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

import java.lang.reflect.Method;

public final class KitClaimPermissionUtil {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static Object luckPermsApi;
    private static Method mGetUserManager;
    private static Method mGetUser;
    private static Method mGetCachedData;

    private KitClaimPermissionUtil() {
    }

    public static void init() {
        if (tryInitWithClassLoader(KitClaimPermissionUtil.class.getClassLoader(), "KitClaimPermissionUtil.class.getClassLoader()")) {
            return;
        }
        if (tryInitWithClassLoader(Thread.currentThread().getContextClassLoader(), "Thread context classloader")) {
            return;
        }
        if (tryInitWithClassLoader(ClassLoader.getSystemClassLoader(), "System classloader")) {
            return;
        }

        luckPermsApi = null;
        LOGGER.warn("[KitClaim] LuckPerms API not available from any classloader. Falling back to OP level checks.");
    }

    public static boolean hasPermission(ServerPlayer player, String node) {
        if (node == null || node.isBlank()) {
            LOGGER.warn("[KitClaim] Empty permission node for player {}", player.getGameProfile().getName());
            return false;
        }

        if (luckPermsApi == null) {
            LOGGER.debug("[KitClaim] LuckPerms API is null, trying init before check. Player={}, Node={}", player.getGameProfile().getName(), node);
            init();
        }

        if (luckPermsApi != null) {
            try {
                Object userManager = mGetUserManager.invoke(luckPermsApi);
                if (userManager == null) {
                    LOGGER.warn("[KitClaim] LuckPerms user manager is null.");
                    return player.hasPermissions(2);
                }
                if (mGetUser == null) {
                    mGetUser = userManager.getClass().getMethod("getUser", java.util.UUID.class);
                }
                Object user = mGetUser.invoke(userManager, player.getUUID());
                if (user != null) {
                    LOGGER.debug("[KitClaim] LuckPerms user loaded for {}", player.getGameProfile().getName());
                    if (mGetCachedData == null) {
                        mGetCachedData = user.getClass().getMethod("getCachedData");
                    }
                    Object cachedData = mGetCachedData.invoke(user);
                    Object permData = invokeNoArg(cachedData, "getPermissionData");
                    if (permData == null) {
                        LOGGER.warn("[KitClaim] PermissionData is null for player={}", player.getGameProfile().getName());
                        return player.hasPermissions(2);
                    }
                    Object result = invokeWithString(permData, "checkPermission", node);
                    Boolean allowed = toBoolean(result);
                    if (allowed != null) {
                        LOGGER.info("[KitClaim] LP check: player={} node={} allowed={}", player.getGameProfile().getName(), node, allowed);
                        return allowed;
                    }
                    LOGGER.warn("[KitClaim] LP check returned non-boolean result for player={} node={}", player.getGameProfile().getName(), node);
                } else {
                    LOGGER.warn("[KitClaim] LP user not found for {}", player.getGameProfile().getName());
                }
            } catch (Throwable e) {
                LOGGER.warn("[KitClaim] LP permission check failed for player={} node={}, falling back to OP. Error={}", player.getGameProfile().getName(), node, e.toString(), e);
                return player.hasPermissions(2);
            }
        }

        LOGGER.info("[KitClaim] Falling back to OP check: player={} node={} opAllowed={}", player.getGameProfile().getName(), node, player.hasPermissions(2));
        return player.hasPermissions(2);
    }

    private static boolean tryInitWithClassLoader(ClassLoader classLoader, String label) {
        if (classLoader == null) {
            LOGGER.debug("[KitClaim] LuckPerms init skipped: {} is null", label);
            return false;
        }
        try {
            LOGGER.debug("[KitClaim] Trying LuckPerms init via {}", label);
            Class<?> providerClass = Class.forName("net.luckperms.api.LuckPermsProvider", true, classLoader);
            Method get = providerClass.getMethod("get");
            luckPermsApi = get.invoke(null);
            mGetUserManager = luckPermsApi.getClass().getMethod("getUserManager");

            LOGGER.info("[KitClaim] LuckPerms API found via {}. Permission checks enabled.", label);
            return true;
        } catch (Throwable e) {
            luckPermsApi = null;
            LOGGER.warn("[KitClaim] LuckPerms init failed via {}. Error={}", label, e.toString(), e);
            return false;
        }
    }

    private static Boolean toBoolean(Object result) {
        if (result == null) {
            return null;
        }
        if (result instanceof Boolean b) {
            return b;
        }
        Boolean viaAsBoolean = invokeAsBoolean(result);
        if (viaAsBoolean != null) {
            return viaAsBoolean;
        }

        Object inner = invokeNoArg(result, "result");
        if (inner == null) {
            inner = invokeNoArg(result, "state");
        }
        if (inner == null) {
            inner = invokeNoArg(result, "tristate");
        }
        if (inner != null) {
            Boolean innerBool = invokeAsBoolean(inner);
            if (innerBool != null) {
                return innerBool;
            }
        }

        return null;
    }

    private static Boolean invokeAsBoolean(Object target) {
        try {
            Method m = target.getClass().getMethod("asBoolean");
            Object out = m.invoke(target);
            if (out instanceof Boolean b) {
                return b;
            }
        } catch (Throwable ignored) {
            // ignore
        }
        return null;
    }

    private static Object invokeNoArg(Object target, String method) {
        try {
            Method m = target.getClass().getMethod(method);
            return m.invoke(target);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object invokeWithString(Object target, String method, String arg) {
        try {
            Method m = target.getClass().getMethod(method, String.class);
            return m.invoke(target, arg);
        } catch (Throwable ignored) {
            return null;
        }
    }
}
