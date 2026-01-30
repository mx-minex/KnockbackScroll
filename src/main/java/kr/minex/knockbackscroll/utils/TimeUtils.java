package kr.minex.knockbackscroll.utils;

/**
 * 시간 관련 유틸리티 클래스
 */
public final class TimeUtils {

    private TimeUtils() {
        // 인스턴스화 방지
    }

    /**
     * 초를 포맷된 문자열로 변환
     * @param seconds 초
     * @return "초" 형식 (예: "10초", "65초")
     */
    public static String formatSeconds(long seconds) {
        if (seconds <= 0) {
            return "0초";
        }
        return seconds + "초";
    }

    /**
     * 초를 MM:SS 형식으로 변환 (1분 이상인 경우)
     * @param seconds 초
     * @return "MM:SS" 또는 "SS초" 형식
     */
    public static String formatTime(long seconds) {
        if (seconds <= 0) {
            return "0초";
        }
        if (seconds < 60) {
            return seconds + "초";
        }
        long minutes = seconds / 60;
        long remainingSeconds = seconds % 60;
        return String.format("%d분 %02d초", minutes, remainingSeconds);
    }

    /**
     * 밀리초를 초로 변환 (올림 처리)
     * @param millis 밀리초
     * @return 초 (올림)
     */
    public static long millisToSeconds(long millis) {
        if (millis <= 0) {
            return 0;
        }
        return (millis + 999) / 1000;
    }

    /**
     * 남은 시간 계산 (밀리초 기준)
     * @param endTimeMillis 종료 시각 (밀리초)
     * @return 남은 초 (올림), 만료됐으면 0
     */
    public static long getRemainingSeconds(long endTimeMillis) {
        long remaining = endTimeMillis - System.currentTimeMillis();
        if (remaining <= 0) {
            return 0;
        }
        return millisToSeconds(remaining);
    }
}
