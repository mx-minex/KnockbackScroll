package kr.minex.knockbackscroll.listeners;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import kr.minex.knockbackscroll.KnockbackScroll;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 넉백 무효화 처리 리스너
 * Attribute를 사용하여 넉백 저항을 최대치로 설정
 *
 * 방식: GENERIC_KNOCKBACK_RESISTANCE 속성을 1.0으로 설정하면 넉백 완전 무효화
 */
public class KnockbackListener implements Listener {

    private final KnockbackScroll plugin;

    // 플레이어별 적용된 AttributeModifier 저장 (정리용)
    private final Map<UUID, AttributeModifier> appliedModifiers = new ConcurrentHashMap<>();

    // AttributeModifier 이름
    private static final String MODIFIER_NAME = "knockback_scroll_resistance";

    public KnockbackListener(KnockbackScroll plugin) {
        this.plugin = plugin;
    }

    /**
     * 넉백 저항 속성 적용
     * 효과 활성화 시 호출
     */
    public void applyKnockbackResistance(Player player) {
        AttributeInstance attribute = player.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE);
        if (attribute == null) {
            return;
        }

        UUID uuid = player.getUniqueId();

        // 이미 적용되어 있으면 제거 후 재적용
        removeKnockbackResistance(player);

        // 넉백 저항 1.0 = 100% 넉백 무효화
        AttributeModifier modifier = new AttributeModifier(
            UUID.randomUUID(),
            MODIFIER_NAME,
            1.0,
            AttributeModifier.Operation.ADD_NUMBER
        );

        attribute.addModifier(modifier);
        appliedModifiers.put(uuid, modifier);
    }

    /**
     * 넉백 저항 속성 제거
     * 효과 만료 시 호출
     */
    public void removeKnockbackResistance(Player player) {
        AttributeInstance attribute = player.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE);
        if (attribute == null) {
            return;
        }

        UUID uuid = player.getUniqueId();
        AttributeModifier existingModifier = appliedModifiers.remove(uuid);

        if (existingModifier != null) {
            attribute.removeModifier(existingModifier);
        }

        // 혹시 이름으로 남아있는 모디파이어도 정리
        for (AttributeModifier mod : attribute.getModifiers()) {
            if (MODIFIER_NAME.equals(mod.getName())) {
                attribute.removeModifier(mod);
            }
        }
    }

    /**
     * 폭발 데미지 넉백도 무효화 (Attribute로 이미 처리되지만 보험용)
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onExplosionDamage(EntityDamageEvent event) {
        if (event instanceof EntityDamageByEntityEvent) {
            return;
        }

        EntityDamageEvent.DamageCause cause = event.getCause();
        if (cause != EntityDamageEvent.DamageCause.BLOCK_EXPLOSION &&
            cause != EntityDamageEvent.DamageCause.ENTITY_EXPLOSION) {
            return;
        }

        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        if (!plugin.getEffectManager().hasActiveEffect(player)) {
            return;
        }

        // Attribute가 이미 적용되어 있으므로 추가 처리 불필요
        // 혹시 모를 경우를 대비해 속성 재확인
        if (!appliedModifiers.containsKey(player.getUniqueId())) {
            applyKnockbackResistance(player);
        }
    }

    /**
     * 데이터 정리 (플레이어 퇴장 시)
     */
    public void cleanup(UUID uuid) {
        appliedModifiers.remove(uuid);
    }

    /**
     * 모든 데이터 정리 (플러그인 비활성화 시)
     */
    public void clear() {
        appliedModifiers.clear();
    }
}
