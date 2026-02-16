import ws from 'k6/ws';
import { check, sleep } from 'k6';
import { Trend } from 'k6/metrics';
import exec from 'k6/execution'; // 테스트 진행 시간 확인을 위해 추가

const messageLatency = new Trend('message_latency_ms');

// 1. 테스트 부하 시나리오 설정
export const options = {
    // stages: [
    //     { duration: '2m', target: 15000 }, // 5분 동안 7,500명까지 서서히 증가 (Ramp-up)
    //     { duration: '1m', target: 15000 }, // 10분 동안 7,500명 유지 (Steady State)
    //     { duration: '1m', target: 0 },    // 2분 동안 종료 (Ramp-down)
    // ],
    // stages: [
    //     { duration: '2m', target: 7500 }, // 5분 동안 7,500명까지 서서히 증가 (Ramp-up)
    //     { duration: '1m', target: 7500 }, // 10분 동안 7,500명 유지 (Steady State)
    //     { duration: '1m', target: 0 },    // 2분 동안 종료 (Ramp-down)
    // ],
    // stages: [
    //     { duration: '1m', target: 5000 },
    //     { duration: '30s', target: 5000 },
    //     { duration: '30s', target: 0 },
    // ],
    // stages: [
    //     { duration: '10s', target: 1000 },
    //     { duration: '1m', target: 1000 },
    //     { duration: '10s', target: 0 },
    // ],
    stages: [
        { duration: '15s', target: 400 },
        { duration: '60s', target: 400 },
        { duration: '15s', target: 0 },
    ],
    thresholds: {
        'checks': ['rate>0.95'], // 전체 요청의 95% 이상이 성공해야 함
        'message_latency_ms' : ['p(95)<500'],
    },
};

export default function () {
    // -------------------------------------------------------------------------
    // 환경 설정 (사용자님의 서버 환경에 맞춰 수정하세요)
    // -------------------------------------------------------------------------

    // 만약 서버에서 SockJS를 사용한다면 끝에 '/websocket'을 붙여야 합니다.
    const url = 'ws://localhost:8080/ws/connect/websocket';
    // const roomId = (__VU % 300) + 1;
    // const roomId = (__VU % 150) + 1; // 150개 방에 유저 분산
    // const roomId = (__VU % 20) + 1; // 10개 방에 유저 분산
    const roomId = 1; // 1개 방에 유저 분산
    const userId = __VU;             // 유저 고유 ID (테스트용)

    // 쿠키 설정 (로그에 찍혔던 인증 과정을 통과하기 위해 필수)
    const params = {
        headers: {
            'Cookie': `Authorization=eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIyIiwiaWF0IjoxNzcwOTQ1MDE3LCJleHAiOjE3NzEwMzE0MTd9.DSuTaKC1uxMq5eluIVZCZHYonKCL545kC7u6SAbRypU;`, // 실제 유효한 쿠키/토큰 값
                'Origin': 'http://localhost:8080',
        },
    };

    // --- 상태 관리 플래그 및 타이머 변수 추가 ---
    let isConnected = false;
    let chatInterval = null;

    const res = ws.connect(url, params, function (socket) {

        socket.on('open', function () {
            isConnected = true; // 연결 성공 시 플래그 설정
            // console.log(`[VU ${__VU}] 연결 성공! STOMP CONNECT 시도...`);

            // 2. STOMP CONNECT 프레임 (서버 로그 규격 반영)
            // 헤더와 바디 사이의 빈 줄(\n\n)과 끝의 NULL(\0)이 매우 중요합니다.
            const connectFrame = [
                'CONNECT',
                'roomId:' + roomId,
                'memberId:' + userId,
                'accept-version:1.1,1.2',
                'heart-beat:0,0',
                '', // 헤더와 바디 사이의 빈 줄
                '\0' // NULL 종료 문자
            ].join('\n');

            socket.send(connectFrame);

            // 3. STOMP SUBSCRIBE 프레임 (채팅 수신  구독)
            const subscribeFrame =
                "SUBSCRIBE\n" +
                "id:sub-" + userId + "\n" +
                "destination:/sub/" + roomId + "/messages\n" +
                "\n" +
                "\0";
            socket.send(subscribeFrame);

            // // // 4. 주기적 메시지 전송 (50초마다 1번 대화 -> 전체 150 RPS 발생)
            socket.setTimeout(() => {
                // setInterval 시작 전 연결 상태 확인
                if (!isConnected) return;

                chatInterval = socket.setInterval(function () {

                    // --- Ramp-down 체크: 15초(up) + 60초(steady) = 75,000ms 이후에는 전송 중단 ---
                    if (exec.instance.currentTestRunDuration >= 75000) {
                        if (chatInterval) {
                            socket.clearInterval(chatInterval);
                        }
                        return;
                    }

                    // 메시지 전송 직전 연결 상태 다시 확인
                    if (isConnected) {
                        const chatPayload = JSON.stringify({
                            sender: `user-${userId}`,
                            message: "Hello STOMP!",
                            type: "CHAT"
                        });

                        const sendFrame =
                            "SEND\n" +
                            "destination:/pub/send/" + roomId + "\n" +
                            "content-type:application/json\n" +
                            "\n" +
                            chatPayload +
                            "\0";

                        socket.send(sendFrame);
                    }
                }, 1000);    //1초에 300개의 메세지
                // }, 25000);    //1초에 300개의 메세지
                // }, 15000);    //1초에 500개의 메세지
                // }, 7500);    //1초에 1000개의 메세지
                // }, 3750);    //1초에 2000개의 메세지
            },15000 + Math.floor(Math.random() * 10000))

            // }, 1000);    //100명
            // }, 6000);    //1000명
            // }, 50000);   //7500명     rps 150으로 맞추기 위함

        });

        // 메시지 수신 시 로그 (너무 많으면 주석 처리하세요)
        socket.on('message', function (data) {
            // console.log(`[VU ${__VU}] 메시지 수신`);
            try {
                // STOMP 프레임에서 JSON 본문만 추출 (간단한 처리)
                const bodyStart = data.indexOf('{');
                const bodyEnd = data.lastIndexOf('}');
                if (bodyStart !== -1 && bodyEnd !== -1) {
                    const body = data.substring(bodyStart, bodyEnd + 1);
                    const msg = JSON.parse(body);
                    if (msg.timeStamp) {
                        const timeStamp = new Date(msg.timeStamp);
                        const now = Date.now();
                        const latency = now - timeStamp;

                        messageLatency.add(latency);
                    }
                }
            } catch (e) {
                // 파싱 에러 시 무시
            }
        });

        // 에러 발생 시 상세 출력
        socket.on('error', function (e) {
            isConnected = false; // 에러 발생 시 플래그 해제
            if (chatInterval) {
                socket.clearInterval(chatInterval); // 타이머 즉시 중단
            }
            console.log(`[VU ${__VU}] 소켓 에러: ${e.error()}`);
        });

        socket.on('close', function () {
            isConnected = false; // 연결 종료 시 플래그 해제
            if (chatInterval) {
                socket.clearInterval(chatInterval); // 타이머 즉시 중단
            }
            // console.log(`[VU ${__VU}] 연결 종료`);
        });


        // 테스트 시간 동안 연결 유지
        socket.setTimeout(function () {
            isConnected = false;
            if (chatInterval) {
                socket.clearInterval(chatInterval);
            }
            socket.close();
            // }, 750000); // 7500     12분
        }, 300000); // 5000    5분
        // }, 120000); // 100     2분
    });


    // 핸드셰이크 결과 확인 (로그가 안 찍힌다면 여기서 101이 안 나올 확률이 큼)
    check(res, {
        'status is 101 (Switching Protocols)': (r) => r && r.status === 101,
    });

    if (res && res.status !== 101) {
        console.log(`[VU ${__VU}] 접속 실패 - 상태 코드: ${res.status}`);
    }
}