package stack.moaticket.system.sse.register;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import stack.moaticket.system.alarm.sse.register.SseEmitterRegister;

import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.IntStream;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.BDDAssertions.thenCode;
import static org.mockito.BDDMockito.*;

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
        Map<String, SseEmitter> list = sseEmitterRegister.getSseEmitters(mid);
        assertThat(list).isNotNull();
        assertThat(list).contains(entry(cid, emitter));
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
        assertThat(sseEmitterRegister.getSseEmitters(1L))
                .contains(entry(cid1, e1))
                .doesNotContain(entry(cid2, e2));
        assertThat(sseEmitterRegister.getSseEmitters(2L))
                .contains(entry(cid2, e2))
                    .doesNotContain(entry(cid1, e1));
    }

    @Test
    @DisplayName("GET은 (멤버ID, 커넥션ID)가 없으면 NULL을 반환한다.")
    void shouldReturnNullWhenMissingPair() {
        // given
        Long mid = 1L;
        SseEmitter emitter = mock(SseEmitter.class);
        String cid = sseEmitterRegister.insert(mid, emitter);

        // when
        SseEmitter missingConnection = sseEmitterRegister.get(mid, "fake");
        SseEmitter missingMember = sseEmitterRegister.get(999L, cid);

        // then
        then(missingConnection).isNull();
        then(missingMember).isNull();
    }

    @Test
    @DisplayName("Emitter List는 Snapshot으로 받아서 내부에 직접적으로 영향을 줘선 안된다.")
    void makeSnapshotNotToEffectOnOriginalList() {
        // given
        Long mid = 1L;
        SseEmitter emitter = mock(SseEmitter.class);
        String cid = sseEmitterRegister.insert(mid, emitter);

        // when
        Map<String, SseEmitter> snapshot = sseEmitterRegister.getSseEmitters(mid);
        snapshot.clear();

        // then
        Map<String, SseEmitter> after = sseEmitterRegister.getSseEmitters(mid);
        assertThat(after).contains(entry(cid, emitter));
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
        then(sseEmitterRegister.getSseEmitters(mid))
                .contains(entry(cid2, e2))
                .doesNotContain(entry(cid1, e1));
        then(sseEmitterRegister.get(mid, cid1)).isNull();
    }

    @Test
    @DisplayName("존재하지 않는 커넥션ID를 Remove해도 예외 없이 동작한다.")
    void althoughRemoveMissingConnectionIdWillRunWithoutException() {
        // given
        Long mid = 1L;
        SseEmitter emitter = mock(SseEmitter.class);
        String cid = sseEmitterRegister.insert(mid, emitter);

        // when & then
        thenCode(() -> sseEmitterRegister.remove(mid, "fake")).doesNotThrowAnyException();
        then(sseEmitterRegister.getSseEmitters(mid)).contains(entry(cid, emitter));
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
