package stack.moaticket.system.sse.register;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import stack.moaticket.system.alarm.sse.model.EmitterMeta;
import stack.moaticket.system.alarm.sse.component.register.SseEmitterRegister;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.stream.IntStream;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
public class SseEmitterRegisterTest {
    private final SseEmitterRegister sseEmitterRegister = new SseEmitterRegister();

    @Test
    @DisplayName("Register에 Emitter를 등록한다.")
    void insertThenGetEmitter() {
        // given
        Long mid = 1L;
        SseEmitter emitter = mock(SseEmitter.class);

        // when
        String cid = sseEmitterRegister.insert(mid, emitter);

        // then
        Map<String, EmitterMeta> map = sseEmitterRegister.getSseEmitters(mid);
        assertThat(map).isNotNull();
        assertThat(map.containsKey(cid));
        assertThat(map.get(cid).getEmitter()).isSameAs(emitter);
    }

    @Test
    @DisplayName("유저 ID에 따라 Emitter는 분리되어 등록된다.")
    void insertIsolatedByMemberId() {
        // given
        SseEmitter e1 = mock(SseEmitter.class);
        SseEmitter e2 = mock(SseEmitter.class);

        // when
        String cid1 = sseEmitterRegister.insert(1L, e1);
        String cid2 = sseEmitterRegister.insert(2L, e2);

        // then
        Map<String, EmitterMeta> m1 = sseEmitterRegister.getSseEmitters(1L);
        Map<String, EmitterMeta> m2 = sseEmitterRegister.getSseEmitters(2L);

        assertThat(m1).containsKey(cid1);
        assertThat(m1.get(cid1).getEmitter()).isSameAs(e1);
        assertThat(m1).doesNotContainKey(cid2);

        assertThat(m2).containsKey(cid2);
        assertThat(m2.get(cid2).getEmitter()).isSameAs(e2);
        assertThat(m2).doesNotContainKey(cid1);
    }

    @Test
    @DisplayName("GET은 (멤버ID, 커넥션ID)가 없으면 NULL을 반환한다.")
    void shouldReturnNullWhenMissingPair() {
        // given
        Long mid = 1L;
        SseEmitter emitter = mock(SseEmitter.class);
        String cid = sseEmitterRegister.insert(mid, emitter);

        // when
        EmitterMeta missingConnection = sseEmitterRegister.get(mid, "fake");
        EmitterMeta missingMember = sseEmitterRegister.get(999L, cid);

        // then
        then(missingConnection).isNull();
        then(missingMember).isNull();
    }

    @Test
    @DisplayName("Emitter Map은 Snapshot으로 받아서 내부에 직접적으로 영향을 줘선 안된다.")
    void makeSnapshotNotToEffectOnOriginalList() {
        // given
        Long mid = 1L;
        SseEmitter emitter = mock(SseEmitter.class);
        String cid = sseEmitterRegister.insert(mid, emitter);

        // when
        Map<String, EmitterMeta> snapshot = sseEmitterRegister.getSseEmitters(mid);
        snapshot.clear();

        // then
        Map<String, EmitterMeta> after = sseEmitterRegister.getSseEmitters(mid);
        assertThat(after).containsKey(cid);
        assertThat(after.get(cid).getEmitter()).isSameAs(emitter);
    }

    @Test
    @DisplayName("GET_FILTERED 호출 시 EmitterMap이 비어있으면 emptyMap을 반환한다.")
    void getFilteredWhenEmptyReturnEmptyMap() {
        // when
        Map<Long, Map<String, EmitterMeta>> result = sseEmitterRegister.getFiltered(meta -> true);

        // then
        assertThat(result).isEmpty();
        assertThat(result).isSameAs(Collections.emptyMap());
    }

    @Test
    @DisplayName("GET_FILTERED 호출 시 Predicate를 만족하는 meta만 불러온다.")
    void gitFilteredFilteringWorks() {
        // given
        Long m1 = 1L;
        Long m2 = 2L;

        SseEmitter e1 = mock(SseEmitter.class);
        SseEmitter e2 = mock(SseEmitter.class);
        SseEmitter e3 = mock(SseEmitter.class);

        String cid1 = sseEmitterRegister.insert(m1, e1);
        String cid2 = sseEmitterRegister.insert(m1, e2);
        String cid3 = sseEmitterRegister.insert(m2, e3);

        EmitterMeta meta1 = sseEmitterRegister.get(m1, cid1);
        EmitterMeta meta2 = sseEmitterRegister.get(m1, cid2);
        EmitterMeta meta3 = sseEmitterRegister.get(m2, cid3);

        AtomicLong last1 = (AtomicLong) ReflectionTestUtils.getField(meta1, "lastSentAtMillis");
        AtomicLong last2 = (AtomicLong) ReflectionTestUtils.getField(meta2, "lastSentAtMillis");
        AtomicLong last3 = (AtomicLong) ReflectionTestUtils.getField(meta3, "lastSentAtMillis");

        long now = System.currentTimeMillis();
        last1.set(now - 60_000);
        last2.set(now);
        last3.set(now - 60_000);

        LocalDateTime current = LocalDateTime.now();

        Predicate<EmitterMeta> predicate = meta ->
                meta.tryMarkHeartbeat(current);

        // when
        Map<Long, Map<String, EmitterMeta>> result = sseEmitterRegister.getFiltered(predicate);

        // then
        assertThat(result).containsKeys(m1, m2);
        assertThat(result.get(m1)).containsKey(cid1);
        assertThat(result.get(m1)).doesNotContainKey(cid2);
        assertThat(result.get(m2)).containsKey(cid3);
    }

    @Test
    @DisplayName("GET_FILTERED는 원본이 아닌 스냅샷을 반환한다.")
    void getFilteredWillReturnSnapshot() {
        // given
        Long mid = 1L;
        SseEmitter e1 = mock(SseEmitter.class);
        String cid = sseEmitterRegister.insert(mid, e1);

        // when
        Map<Long, Map<String, EmitterMeta>> snapshot1 = sseEmitterRegister.getFiltered(meta -> true);

        // then
        snapshot1.get(mid).remove(cid);

        Map<Long, Map<String, EmitterMeta>> snapshot2 = sseEmitterRegister.getFiltered(meta -> true);

        assertThat(snapshot2.get(mid)).containsKey(cid);
    }

    @Test
    @DisplayName("Remove는 해당 커넥션ID를 가진 Emitter만 제거한다.")
    void removeDeleteOnlyTarget() {
        // given
        Long mid = 1L;
        SseEmitter e1 = mock(SseEmitter.class);
        SseEmitter e2 = mock(SseEmitter.class);

        String cid1 = sseEmitterRegister.insert(mid, e1);
        String cid2 = sseEmitterRegister.insert(mid, e2);

        // when
        sseEmitterRegister.remove(mid, cid1);

        // then
        Map<String, EmitterMeta> after = sseEmitterRegister.getSseEmitters(mid);
        assertThat(after).doesNotContainKey(cid1);
        assertThat(after).containsKey(cid2);
        assertThat(after.get(cid2).getEmitter()).isSameAs(e2);

        assertThat(sseEmitterRegister.get(mid, cid1)).isNull();
    }

    @Test
    @DisplayName("존재하지 않는 커넥션ID를 Remove해도 예외 없이 동작한다.")
    void althoughRemoveMissingConnectionIdWillRunWithoutException() {
        // given
        Long mid = 1L;
        SseEmitter emitter = mock(SseEmitter.class);
        String cid = sseEmitterRegister.insert(mid, emitter);

        // when & then
        assertThatCode(() -> sseEmitterRegister.remove(mid, "fake"))
                .doesNotThrowAnyException();

        Map<String, EmitterMeta> after = sseEmitterRegister.getSseEmitters(mid);
        assertThat(after).containsKey(cid);
        assertThat(after.get(cid).getEmitter()).isSameAs(emitter);
    }

    @Test
    @DisplayName("마지막 Emitter를 제거하면 멤버ID 엔트리도 정리된다.")
    void removeLastEmitterCleanupMemberKey() {
        // given
        Long mid = 1L;
        SseEmitter emitter = mock(SseEmitter.class);
        String cid = sseEmitterRegister.insert(mid, emitter);

        // when
        sseEmitterRegister.remove(mid, cid);

        // then
        then(sseEmitterRegister.getSseEmitters(mid)).isEmpty();
        then(sseEmitterRegister.get(mid, cid)).isNull();
    }

    @Test
    @DisplayName("동시에 Insert → Get → Remove가 수행되어도 예외 없이 동작한다.")
    void concurrentInsertRemoveGetShouldNotThrow() throws Exception {
        // given
        Long mid = 1L;
        int threads = Math.min(8, Runtime.getRuntime().availableProcessors());

        ExecutorService es = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);

        List<Callable<Void>> tasks = IntStream.range(0, 200)
                .mapToObj(i -> (Callable<Void>)() -> {
                    SseEmitter sseEmitter = mock(SseEmitter.class);
                    start.await();
                    String cid = sseEmitterRegister.insert(mid, sseEmitter);
                    sseEmitterRegister.getSseEmitters(mid);
                    sseEmitterRegister.remove(mid, cid);
                    return null;
                })
                .toList();

        // when
        start.countDown();
        List<Future<Void>> futures = es.invokeAll(tasks);

        // then
        for(Future<Void> f : futures) {
            assertThatCode(f::get).doesNotThrowAnyException();
        }

        es.shutdown();
        assertThat(es.awaitTermination(2, TimeUnit.SECONDS)).isTrue();
        then(sseEmitterRegister.getSseEmitters(mid)).isEmpty();
    }
}
