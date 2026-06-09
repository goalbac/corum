package com.corum.backend.service.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class SseEmitterRegistry {

    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public SseEmitter subscribe(Long memberId) {
        SseEmitter emitter = new SseEmitter(300_000L); // 5분 타임아웃

        emitters.put(memberId, emitter);

        emitter.onCompletion(() -> emitters.remove(memberId));
        emitter.onTimeout(() -> {
            emitter.complete();
            emitters.remove(memberId);
        });
        emitter.onError(e -> emitters.remove(memberId));

        // 연결 유지용 초기 이벤트
        try {
            emitter.send(SseEmitter.event().name("connect").data("connected"));
        } catch (Exception e) {
            emitters.remove(memberId);
        }

        return emitter;
    }

    public void send(Long memberId, Object data) {
        SseEmitter emitter = emitters.get(memberId);
        if (emitter == null) return;

        try {
            String json = objectMapper.writeValueAsString(data);
            emitter.send(SseEmitter.event().name("notification").data(json));
        } catch (Exception e) {
            emitters.remove(memberId);
        }
    }
}
