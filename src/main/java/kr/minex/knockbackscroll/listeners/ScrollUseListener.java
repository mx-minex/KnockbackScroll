package kr.minex.knockbackscroll.listeners;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import kr.minex.knockbackscroll.KnockbackScroll;
import kr.minex.knockbackscroll.config.ConfigManager;
import kr.minex.knockbackscroll.managers.CooldownManager;
import kr.minex.knockbackscroll.managers.EffectManager;
import kr.minex.knockbackscroll.managers.ScrollManager;

/**
 * 주문서 우클릭 사용 이벤트 처리 리스너
 */
public class ScrollUseListener implements Listener {

    private final KnockbackScroll plugin;

    public ScrollUseListener(KnockbackScroll plugin) {
        this.plugin = plugin;
    }

    /**
     * 우클릭 상호작용 이벤트 처리
     * - 주문서 여부 확인
     * - 쿨타임 체크
     * - 효과 발동
     * - 소리 재생
     * - 아이템 소모 (1회용)
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        // 우클릭만 처리
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        // 메인핸드만 처리 (중복 이벤트 방지)
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        // 주문서 확인
        ScrollManager scrollManager = plugin.getScrollManager();
        if (!scrollManager.isKnockbackScroll(item)) {
            return;
        }

        // 사용 권한 확인
        if (!player.hasPermission("knockbackscroll.use")) {
            plugin.getMessageManager().send(player, "command.no-permission");
            return;
        }

        // 이벤트 취소 (블록 상호작용 방지)
        event.setCancelled(true);

        CooldownManager cooldownManager = plugin.getCooldownManager();
        EffectManager effectManager = plugin.getEffectManager();

        // 쿨타임 체크
        if (cooldownManager.isOnCooldown(player)) {
            long remaining = cooldownManager.getRemainingCooldown(player);
            plugin.getMessageManager().sendActionBar(player, "cooldown.action-bar", "remaining", remaining);
            return;
        }

        // 이미 효과가 활성화되어 있는지 확인
        if (effectManager.hasActiveEffect(player)) {
            plugin.getMessageManager().send(player, "effect.already-active");
            return;
        }

        // 주문서 소모 (1회용인 경우)
        if (!scrollManager.consumeScroll(player, item)) {
            return;
        }

        // 효과 활성화
        effectManager.activateEffect(player);

        // 쿨타임 설정
        cooldownManager.setCooldown(player);

        // 소리 재생 (주변 플레이어도 들을 수 있음)
        ConfigManager configManager = plugin.getConfigManager();
        Sound sound = configManager.getActivateSound();
        float volume = configManager.getActivateSoundVolume();
        float pitch = configManager.getActivateSoundPitch();
        player.getWorld().playSound(player.getLocation(), sound, volume, pitch);

        // 효과 활성화 메시지
        int duration = configManager.getDurationSeconds();
        plugin.getMessageManager().send(player, "effect.activated", "duration", duration);
    }

    /**
     * 플레이어 퇴장 시 데이터 정리
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        java.util.UUID uuid = event.getPlayer().getUniqueId();
        plugin.getCooldownManager().cleanup(uuid);
        plugin.getEffectManager().cleanup(uuid);
        plugin.getKnockbackListener().cleanup(uuid);
    }
}
