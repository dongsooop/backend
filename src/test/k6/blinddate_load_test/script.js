import ws from 'k6/ws';
import http from 'k6/http';
import {check} from 'k6';
import crypto from 'k6/crypto';
import encoding from 'k6/encoding';
import execution from 'k6/execution';

// 0. 환경 설정 ============================================

// secret key는 decode 해서 써야 함
const SECRET_KEY = 'VGhpc0lzQVZlcnlMb25nU2VjcmV0S2V5Rm9yRG9uZ3Nvb3BMb2NhbERldmVsb3BtZW50VGVzdGluZ1B1cnBvc2VPbmx5MTIzNDU2';

// API 경로 및 WebSocket URL
const BASE_URL = 'http://blinddate-test:8080';
const WS_URL = 'ws://blinddate-test:8080/ws/blinddate';

// 1. 부하 테스트 시나리오 설정 ===============================
export const options = {
    scenarios: {
        ws_join_spike: {
            executor: "constant-arrival-rate",

            // ✅ 초당 1000명 신규 입장
            rate: 1000,
            timeUnit: "1s",

            // 총 3초 동안 실행 → 약 3000명 접속 시도
            duration: "3s",

            // 동시에 실행 가능한 최대 VU 풀
            preAllocatedVUs: 3000,
            maxVUs: 4000,
        },
    },
};

// 2. JWT 생성 유틸리티=======================================
function sign(data, secret) {
    const decodedSecret = encoding.b64decode(secret, 'std');

    // 디코딩된 바이너리 키(decodedSecret)를 사용하여 HMAC 생성
    const hasher = crypto.createHMAC('sha256', decodedSecret);
    hasher.update(data);

    return hasher.digest('base64').replace(/\+/g, '-').replace(/\//g, '_').replace(/=/g, '');
}

function generateJWT(userId, role) {
    // Header에 typ: JWT 추가 (표준 준수)
    const headerData = {alg: 'HS256', typ: 'JWT'};
    const header = encoding.b64encode(JSON.stringify(headerData), 'url').replace(/=/g, '');

    const payloadData = {
        sub: userId.toString(),
        role: [role],
        exp: Math.floor(Date.now() / 1000) + (60 * 60),
        type: 'ACCESS'
    };

    const payload = encoding.b64encode(JSON.stringify(payloadData), 'url').replace(/=/g, '');

    // 수정된 sign 함수 호출
    const signature = sign(`${header}.${payload}`, SECRET_KEY);

    return `${header}.${payload}.${signature}`;
}

// 3. Setup 단계 ==============================================
export function setup() {
    console.log('🔹 Setting up: Starting Blind Date Event...');
    const adminToken = generateJWT('999999', 'ROLE_ADMIN');
    console.info(`adminToken : ${adminToken}`);

    const expiredDate = new Date();
    expiredDate.setHours(expiredDate.getHours() + 1);

    const payload = JSON.stringify({
        expiredDate: expiredDate.toISOString(),
        maxSessionMemberCount: 7
    });

    const params = {
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${adminToken}`
        },
    };

    // ⚠️ User가 지정한 경로 (/blinddate) 사용
    const res = http.post(`${BASE_URL}/blinddate`, payload, params);

    if (res.status === 201) {
        console.log('✅ Blind Date Started (201 Created)');
    } else if (res.status === 409) {
        console.log('⚠️ Blind Date Already Running (409 Conflict)');
    } else {
        console.error(`❌ Failed to start Blind Date. Status: ${res.status} Body: ${res.body}`);
    }
}

// 4. VU 메인 시나리오 =========================================
export default function () {
    const uniqueId = execution.scenario.iterationInTest + 1;
    const userId = uniqueId.toString();
    const token = generateJWT(userId, 'ROLE_USER');
    const authHeader = `Bearer ${token}`;

    const params = {headers: {'Authorization': authHeader}};

    const response = ws.connect(WS_URL, params, function (socket) {
        socket.on('open', function () {
            // 1) STOMP 연결 요청
            const connectFrame = `CONNECT\naccept-version:1.1,1.2\nheart-beat:0,0\nAuthorization:${authHeader}\n\n\0`;
            socket.send(connectFrame);
        });

        socket.on('message', function (msg) {
            // 연결 성공 시 -> 개인 큐 구독 (매칭 대기)
            if (msg.startsWith('CONNECTED')) {
                // console.log(`[VU ${userId}] Connected, Subscribing to /join...`);
                // id:sub-0 은 구독 ID (유니크해야 함)
                socket.send(`SUBSCRIBE\nid:sub-0\ndestination:/user/queue/blinddate/join\n\n\0`);

            }

            if (!msg.startsWith('MESSAGE')) {
                return;
            }

            // 메시지 수신 (세션 배정 알림)
            // STOMP Body 파싱 (헤더와 바디 사이의 빈 줄 찾기)
            const bodyStartIndex = msg.indexOf('\n\n');
            if (bodyStartIndex === -1) {
                return;
            }

            const bodyString = msg.substring(bodyStartIndex + 2).replace(/\0/g, ''); // NULL 문자 제거

            try {
                const body = JSON.parse(bodyString);

                // 세션 ID가 오면 -> 세션 토픽 구독 (입장 완료)
                if (body.sessionId) {
                    // console.log(`[VU ${userId}] 🎉 Assigned to Session: ${body.sessionId}`);
                    socket.send(`SUBSCRIBE\nid:sub-1\ndestination:/topic/blinddate/session/${body.sessionId}/joined\n\n\0`);

                    // (선택) 여기서 "안녕하세요" 메시지를 보낼 수도 있음
                }
            } catch (e) {
                // JSON 파싱 에러는 무시 (시스템 메시지 등일 수 있음)
            }
        });

        socket.on('error', function (e) {
            if (e.error() !== 'websocket: close sent') {
                console.error(`[VU ${userId}] Error: ${e.error()}`);
            }
        });

        // 3초만 기다리면 매칭되기도 전에 나가는 꼴이 됩니다.
        socket.setTimeout(function () {
            socket.close();
        }, 10000);
    });

    check(response, {'status is 101': (r) => r && r.status === 101});
}