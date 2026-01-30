package kr.minex.knockbackscroll.managers;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import kr.minex.knockbackscroll.KnockbackScroll;
import kr.minex.knockbackscroll.models.ScrollType;

import java.util.List;

/**
 * 주문서 아이템 생성 및 검증 관리 클래스
 * PersistentDataContainer를 사용하여 아이템 식별
 */
public class ScrollManager {

    private final KnockbackScroll plugin;

    // NamespacedKey - 주문서 식별용
    private final NamespacedKey scrollKey;
    private final NamespacedKey typeKey;

    public ScrollManager(KnockbackScroll plugin) {
        this.plugin = plugin;
        this.scrollKey = new NamespacedKey(plugin, "knockback_scroll");
        this.typeKey = new NamespacedKey(plugin, "scroll_type");
    }

    /**
     * 주문서 아이템 생성
     * @param type 주문서 타입 (1회용/다회용)
     * @return 생성된 주문서 ItemStack
     */
    public ItemStack createScroll(ScrollType type) {
        ItemStack scroll = new ItemStack(Material.PAPER);
        ItemMeta meta = scroll.getItemMeta();

        if (meta == null) {
            plugin.getLogger().severe("ItemMeta를 생성할 수 없습니다.");
            return scroll;
        }

        // 표시명 설정
        String displayName = plugin.getConfigManager().getScrollDisplayName(type);
        meta.setDisplayName(displayName);

        // 설명(Lore) 설정
        List<String> lore = plugin.getConfigManager().getScrollLore(type);
        meta.setLore(lore);

        // 인챈트 효과 추가 (시각적 효과만)
        meta.addEnchant(Enchantment.DURABILITY, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

        // PersistentDataContainer에 데이터 저장
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(scrollKey, PersistentDataType.BYTE, (byte) 1);
        pdc.set(typeKey, PersistentDataType.STRING, type.getId());

        scroll.setItemMeta(meta);
        return scroll;
    }

    /**
     * 아이템이 넉백저항 주문서인지 확인
     * @param item 확인할 아이템
     * @return 주문서이면 true
     */
    public boolean isKnockbackScroll(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return pdc.has(scrollKey, PersistentDataType.BYTE);
    }

    /**
     * 주문서 타입 반환
     * @param item 주문서 아이템
     * @return 주문서 타입 (주문서가 아니면 null)
     */
    public ScrollType getScrollType(ItemStack item) {
        if (!isKnockbackScroll(item)) {
            return null;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        String typeId = pdc.get(typeKey, PersistentDataType.STRING);

        return ScrollType.fromId(typeId);
    }

    /**
     * 기존 아이템을 주문서로 변환 (지정 명령어용)
     * @param item 변환할 아이템
     * @param type 주문서 타입
     * @return 성공 여부
     */
    public boolean convertToScroll(ItemStack item, ScrollType type) {
        if (item == null || item.getType().isAir()) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }

        // 표시명 설정
        String displayName = plugin.getConfigManager().getScrollDisplayName(type);
        meta.setDisplayName(displayName);

        // 설명(Lore) 설정
        List<String> lore = plugin.getConfigManager().getScrollLore(type);
        meta.setLore(lore);

        // 인챈트 효과 추가 (시각적 효과만)
        meta.addEnchant(Enchantment.DURABILITY, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

        // PersistentDataContainer에 데이터 저장
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(scrollKey, PersistentDataType.BYTE, (byte) 1);
        pdc.set(typeKey, PersistentDataType.STRING, type.getId());

        item.setItemMeta(meta);
        return true;
    }

    /**
     * 주문서 사용 처리 (1회용인 경우 소모)
     * @param player 사용 플레이어
     * @param item 사용할 주문서
     * @return 사용 성공 여부
     */
    public boolean consumeScroll(Player player, ItemStack item) {
        ScrollType type = getScrollType(item);
        if (type == null) {
            return false;
        }

        if (type == ScrollType.SINGLE_USE) {
            // 1회용: 아이템 수량 감소
            if (item.getAmount() > 1) {
                item.setAmount(item.getAmount() - 1);
            } else {
                // 메인핸드에 있는 아이템 제거
                player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
            }
        }
        // 다회용(UNLIMITED)은 소모하지 않음

        return true;
    }
}
