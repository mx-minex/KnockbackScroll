package kr.minex.knockbackscroll.models;

/**
 * 주문서 타입 열거형
 * 1회용과 다회용(무제한) 주문서 구분
 */
public enum ScrollType {

    SINGLE_USE("single-use", "1회용"),
    UNLIMITED("unlimited", "다회용");

    private final String id;
    private final String displayName;

    ScrollType(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    /**
     * 주문서 타입 ID 반환
     * @return 타입 ID (예: "single", "unlimited")
     */
    public String getId() {
        return id;
    }

    /**
     * 표시용 이름 반환
     * @return 한국어 표시명 (예: "1회용", "다회용")
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * ID로 ScrollType 찾기
     * @param id 타입 ID
     * @return 해당 ScrollType, 없으면 null
     */
    public static ScrollType fromId(String id) {
        if (id == null) {
            return null;
        }
        for (ScrollType type : values()) {
            if (type.id.equalsIgnoreCase(id)) {
                return type;
            }
        }
        return null;
    }

    /**
     * 표시명으로 ScrollType 찾기
     * @param name 한국어 표시명
     * @return 해당 ScrollType, 없으면 null
     */
    public static ScrollType fromDisplayName(String name) {
        if (name == null) {
            return null;
        }
        for (ScrollType type : values()) {
            if (type.displayName.equals(name)) {
                return type;
            }
        }
        return null;
    }
}
