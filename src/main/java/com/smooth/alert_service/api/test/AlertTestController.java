package com.smooth.alert_service.api.test;

import com.smooth.alert_service.core.AlertEventHandler;
import com.smooth.alert_service.model.AlertEvent;
import com.smooth.alert_service.support.util.LastSeenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class AlertTestController {

    private static final String GEO_KEY = "location:current";
    
    private final AlertEventHandler alertEventHandler;
    private final RedisTemplate<String, String> redisTemplate;
    private final com.smooth.alert_service.core.AlertSender alertSender;
    private final LastSeenService lastSeenService;
    
    // 사고 알림 테스트
    @PostMapping("/accident")
    public ResponseEntity<Map<String, Object>> testAccident(
            @RequestParam String userId,
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam(required = false) String accidentId) {
        
        try {
            Instant now = Instant.now();

            // 테스트용 위치 데이터: 단일 GEOSET에 기록 + lastseen 갱신
            redisTemplate.opsForGeo().add(GEO_KEY,
                    new org.springframework.data.geo.Point(longitude, latitude), userId);
            lastSeenService.markSeen(userId, now);

            redisTemplate.opsForGeo().add(GEO_KEY,
                    new org.springframework.data.geo.Point(longitude + 0.001, latitude + 0.001), "nearby-user-1");
            lastSeenService.markSeen("nearby-user-1", now);

            redisTemplate.opsForGeo().add(GEO_KEY,
                    new org.springframework.data.geo.Point(longitude + 0.002, latitude + 0.002), "nearby-user-2");
            lastSeenService.markSeen("nearby-user-2", now);

            redisTemplate.opsForGeo().add(GEO_KEY,
                    new org.springframework.data.geo.Point(longitude + 0.01, latitude + 0.01), "far-user-1"); // 반경 밖
            lastSeenService.markSeen("far-user-1", now);
            
            // 사고 이벤트 생성
            String timestamp = now.toString();
            AlertEvent event = new AlertEvent(
                "accident",
                accidentId != null ? accidentId : "test-accident-" + System.currentTimeMillis(),
                userId,
                latitude,
                longitude,
                timestamp
            );
            
            log.info("사고 알림 테스트 시작: {}", event);
            alertEventHandler.handle(event);
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "사고 알림 전송 완료",
                "event", event,
                "geoKey", GEO_KEY,
                "testUsers", Map.of(
                    "accident_user", userId,
                    "nearby_users", new String[]{"nearby-user-1", "nearby-user-2"},
                    "far_users", new String[]{"far-user-1"}
                )
            ));
            
        } catch (Exception e) {
            log.error("사고 알림 테스트 실패", e);
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        }
    }
    
    // 장애물 알림 테스트
    @PostMapping("/obstacle")
    public ResponseEntity<Map<String, Object>> testObstacle(
            @RequestParam String userId,
            @RequestParam Double latitude,
            @RequestParam Double longitude) {
        
        try {
            Instant now = Instant.now();

            // 위치 + lastseen
            redisTemplate.opsForGeo().add(GEO_KEY,
                    new org.springframework.data.geo.Point(longitude, latitude), userId);
            lastSeenService.markSeen(userId, now);

            redisTemplate.opsForGeo().add(GEO_KEY,
                    new org.springframework.data.geo.Point(longitude + 0.0005, latitude + 0.0005), "nearby-user-1");
            lastSeenService.markSeen("nearby-user-1", now);

            redisTemplate.opsForGeo().add(GEO_KEY,
                    new org.springframework.data.geo.Point(longitude + 0.001, latitude + 0.001), "nearby-user-2");
            lastSeenService.markSeen("nearby-user-2", now);

            String timestamp = now.toString();
            AlertEvent event = new AlertEvent(
                "obstacle",
                null,
                userId,
                latitude,
                longitude,
                timestamp
            );
            
            log.info("장애물 알림 테스트 시작: {}", event);
            alertEventHandler.handle(event);
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "장애물 알림 전송 완료",
                "event", event,
                "geoKey", GEO_KEY,
                "note", "본인(" + userId + ")은 알림을 받지 않고, 반경 내 다른 사용자들만 알림 수신"
            ));
            
        } catch (Exception e) {
            log.error("장애물 알림 테스트 실패", e);
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        }
    }
    
    // 포트홀 알림 테스트
    @PostMapping("/pothole")
    public ResponseEntity<Map<String, Object>> testPothole(
            @RequestParam String userId,
            @RequestParam Double latitude,
            @RequestParam Double longitude) {
        
        try {
            Instant now = Instant.now();

            // 위치 + lastseen
            redisTemplate.opsForGeo().add(GEO_KEY,
                    new org.springframework.data.geo.Point(longitude, latitude), userId);
            lastSeenService.markSeen(userId, now);

            redisTemplate.opsForGeo().add(GEO_KEY,
                    new org.springframework.data.geo.Point(longitude + 0.0003, latitude + 0.0003), "nearby-user-1");
            lastSeenService.markSeen("nearby-user-1", now);

            String timestamp = now.toString();

            AlertEvent event = new AlertEvent(
                "pothole",
                null,
                userId,
                latitude,
                longitude,
                timestamp
            );
            
            log.info("포트홀 알림 테스트 시작: {}", event);
            alertEventHandler.handle(event);
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "포트홀 알림 전송 완료 (1회성)",
                "event", event,
                "geoKey", GEO_KEY
            ));
            
        } catch (Exception e) {
            log.error("포트홀 알림 테스트 실패", e);
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        }
    }
    
    // 간단한 WebSocket 메시지 전송 테스트
    @PostMapping("/simple-message")
    public ResponseEntity<Map<String, Object>> sendSimpleMessage(
            @RequestParam String userId,
            @RequestParam(defaultValue = "Hello from server!") String message) {
        
        try {
            // AlertSender 직접 사용하여 간단한 메시지 전송
            var testMessage = new com.smooth.alert_service.model.AlertMessageDto(
                "test",
                Map.of(
                    "title", "테스트 메시지",
                    "content", message,
                    "timestamp", Instant.now().toString()
                )
            );
            
            alertSender.sendToUser(userId, testMessage);
            
            log.info("💬 간단 메시지 전송: userId={}, message={}", userId, message);
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "메시지 전송 완료",
                "targetUser", userId,
                "sentMessage", testMessage
            ));
            
        } catch (Exception e) {
            log.error("간단 메시지 전송 실패", e);
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        }
    }
    
    // WebSocket 연결 상태 확인
    @GetMapping("/connection-status/{userId}")
    public ResponseEntity<Map<String, Object>> checkConnectionStatus(@PathVariable String userId) {
        try {
            // Redis에서 세션 정보 확인
            String sessionKey = "session:" + userId;
            String sessionId = redisTemplate.opsForValue().get(sessionKey);
            
            boolean isConnected = sessionId != null;
            
            return ResponseEntity.ok(Map.of(
                "userId", userId,
                "connected", isConnected,
                "sessionId", sessionId != null ? sessionId : "없음",
                "message", isConnected ? "WebSocket 연결됨" : "WebSocket 연결 안됨"
            ));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", e.getMessage()
            ));
        }
    }
    
    // Redis 위치 데이터 확인
    @GetMapping("/location/{key}")
    public ResponseEntity<Map<String, Object>> checkLocation(@PathVariable String key) {
        try {
            String locationKey = "location:" + key;
            var geoOps = redisTemplate.opsForGeo();
            
            // 모든 위치 데이터 조회는 복잡하므로 키 존재 여부만 확인
            Boolean exists = redisTemplate.hasKey(locationKey);
            
            return ResponseEntity.ok(Map.of(
                "locationKey", locationKey,
                "exists", exists,
                "message", exists ? "위치 데이터 존재" : "위치 데이터 없음"
            ));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", e.getMessage()
            ));
        }
    }
}