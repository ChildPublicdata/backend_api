// [이 파일이 왜 필요한가]
// 보호자 앱 화면에 그대로 띄울 안내문 3종(한 줄 요약 / 3문장 이내 안내문 / 권장 행동 한 문장)을
// 담아 내려주는 응답 형태를 정의하는 파일.
package com.example.demo.riskzone;

public record RiskZoneGuidanceResponse(
        String summary,
        String message,
        String action
) {
}
