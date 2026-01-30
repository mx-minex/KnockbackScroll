package kr.minex.knockbackscroll;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.junit.jupiter.api.*;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 플레이어 퇴장 시 넉백 저항 효과 정리 테스트
 *
 * 버그 시나리오: 넉백 저항 주문서 사용 후 효과 끝나기 전에 나가면
 * 다시 들어왔을 때 효과가 계속 유지되는 문제
 */
@DisplayName("플레이어 퇴장 시 효과 정리 테스트")
class PlayerQuitEffectCleanupTest {

    private ServerMock server;
    private KnockbackScroll plugin;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(KnockbackScroll.class);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    @DisplayName("효과 활성화 후 퇴장하면 Bukkit Attribute가 제거되어야 한다")
    void 효과_활성화_후_퇴장시_속성_제거_테스트() {
        // Given: 플레이어가 서버에 접속하고 효과를 활성화
        PlayerMock player = server.addPlayer("TestPlayer");

        // 효과 활성화
        plugin.getEffectManager().activateEffect(player);

        // 효과가 적용되었는지 확인
        assertTrue(plugin.getEffectManager().hasActiveEffect(player),
            "효과가 활성화되어야 함");

        AttributeInstance attribute = player.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE);
        assertNotNull(attribute, "넉백 저항 속성이 존재해야 함");

        boolean hasModifier = attribute.getModifiers().stream()
            .anyMatch(mod -> "knockback_scroll_resistance".equals(mod.getName()));
        assertTrue(hasModifier, "넉백 저항 모디파이어가 적용되어야 함");

        // When: 플레이어가 퇴장
        player.disconnect();

        // Then: 재접속 시 효과가 없어야 함
        PlayerMock rejoinedPlayer = server.addPlayer("TestPlayer");

        // 효과 데이터가 없어야 함
        assertFalse(plugin.getEffectManager().hasActiveEffect(rejoinedPlayer),
            "재접속 후 효과 데이터가 없어야 함");

        // Bukkit Attribute도 정리되어야 함
        AttributeInstance rejoinedAttribute = rejoinedPlayer.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE);
        if (rejoinedAttribute != null) {
            boolean hasModifierAfterRejoin = rejoinedAttribute.getModifiers().stream()
                .anyMatch(mod -> "knockback_scroll_resistance".equals(mod.getName()));
            assertFalse(hasModifierAfterRejoin,
                "재접속 후 넉백 저항 모디파이어가 없어야 함");
        }
    }

    @Test
    @DisplayName("효과가 없는 상태에서 퇴장해도 오류가 발생하지 않아야 한다")
    void 효과_없이_퇴장시_오류_없음_테스트() {
        // Given: 플레이어가 서버에 접속 (효과 없음)
        PlayerMock player = server.addPlayer("TestPlayer");

        // 효과가 없는 상태
        assertFalse(plugin.getEffectManager().hasActiveEffect(player),
            "효과가 없어야 함");

        // When & Then: 퇴장해도 오류 없이 처리되어야 함
        assertDoesNotThrow(() -> player.disconnect(),
            "효과 없이 퇴장해도 오류가 발생하지 않아야 함");
    }

    @Test
    @DisplayName("재접속 시 이중 안전장치가 작동해야 한다")
    void 재접속시_이중_안전장치_테스트() {
        // Given: 플레이어가 서버에 접속
        PlayerMock player = server.addPlayer("TestPlayer");

        // Bukkit Attribute만 수동으로 추가 (비정상 상태 시뮬레이션)
        AttributeInstance attribute = player.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE);
        if (attribute != null) {
            AttributeModifier modifier = new AttributeModifier(
                UUID.randomUUID(),
                "knockback_scroll_resistance",
                1.0,
                AttributeModifier.Operation.ADD_NUMBER
            );
            attribute.addModifier(modifier);
        }

        // 퇴장
        player.disconnect();

        // When: 재접속
        PlayerMock rejoinedPlayer = server.addPlayer("TestPlayer");

        // Then: onPlayerJoin 이중 안전장치로 속성이 정리되어야 함
        AttributeInstance rejoinedAttribute = rejoinedPlayer.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE);
        if (rejoinedAttribute != null) {
            boolean hasModifier = rejoinedAttribute.getModifiers().stream()
                .anyMatch(mod -> "knockback_scroll_resistance".equals(mod.getName()));
            assertFalse(hasModifier,
                "재접속 시 이중 안전장치로 속성이 정리되어야 함");
        }
    }

    @Test
    @DisplayName("효과 활성화와 비활성화가 정상적으로 동작해야 한다")
    void 효과_활성화_비활성화_테스트() {
        // Given: 플레이어가 서버에 접속
        PlayerMock player = server.addPlayer("TestPlayer");

        // When: 효과 활성화
        plugin.getEffectManager().activateEffect(player);

        // Then: 효과가 활성화됨
        assertTrue(plugin.getEffectManager().hasActiveEffect(player),
            "효과가 활성화되어야 함");

        // When: 효과 비활성화
        plugin.getEffectManager().deactivateEffect(player);

        // Then: 효과가 비활성화됨
        assertFalse(plugin.getEffectManager().hasActiveEffect(player),
            "효과가 비활성화되어야 함");

        // Attribute도 제거됨
        AttributeInstance attribute = player.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE);
        if (attribute != null) {
            boolean hasModifier = attribute.getModifiers().stream()
                .anyMatch(mod -> "knockback_scroll_resistance".equals(mod.getName()));
            assertFalse(hasModifier,
                "비활성화 후 모디파이어가 없어야 함");
        }
    }
}
