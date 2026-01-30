package kr.minex.knockbackscroll.listeners;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import kr.minex.knockbackscroll.KnockbackScroll;
import kr.minex.knockbackscroll.config.ConfigManager;
import kr.minex.knockbackscroll.managers.CooldownManager;
import kr.minex.knockbackscroll.managers.EffectManager;
import kr.minex.knockbackscroll.managers.ScrollManager;

import java.util.UUID;

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
     * 플레이어 접속 시 이중 안전장치
     *
     * 혹시 이전 세션에서 남아있는 넉백 저항 속성이 있다면 정리
     * (비정상 종료 등의 경우를 대비)
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // 효과 데이터가 없는데 속성이 남아있다면 정리
        if (!plugin.getEffectManager().hasActiveEffect(player)) {
            plugin.getKnockbackListener().removeKnockbackResistance(player);
        }
    }

    /**
     * 플레이어 퇴장 시 데이터 정리
     *
     * 중요: 효과가 활성화된 상태에서 퇴장하면 Bukkit Attribute가 남아있게 되므로
     * 반드시 Player 객체가 유효한 동안 속성을 제거해야 함
     *
     * 엣지 케이스: 효과 만료 직후 체커가 실행되기 전에 퇴장하는 경우를 대비해
     * 조건 없이 항상 속성 제거를 시도함 (removeKnockbackResistance는 멱등성 보장)
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // 항상 Bukkit Attribute 제거 시도 (Player 객체가 유효한 이 시점에서)
        // removeKnockbackResistance 내부에서 appliedModifiers도 정리됨
        plugin.getKnockbackListener().removeKnockbackResistance(player);

        // 메모리 데이터 정리 (쿨타임, 효과 데이터)
        // 참고: KnockbackListener.cleanup()은 removeKnockbackResistance()에서 이미 처리됨
        plugin.getCooldownManager().cleanup(uuid);
        plugin.getEffectManager().cleanup(uuid);
    }
}
