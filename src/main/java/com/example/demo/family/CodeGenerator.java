// [이 파일이 왜 필요한가]
// 부모에게 발급할 6자리 숫자 연동 코드를 만드는 도구. 숫자로만 구성한 이유는 자녀가 폰에서
// 숫자 키패드로 빠르게 입력할 수 있게 하기 위함(문자 섞인 코드보다 오타 가능성이 낮음).
package com.example.demo.family;

import java.security.SecureRandom;

/*
 * [왜 Random이 아니라 SecureRandom인가]
 * 이 코드는 "이 값을 아는 사람 = 우리 가족"이라는 보안 토큰 역할을 함. 일반 Random은 시드가 예측 가능해서
 * (예: 현재 시간 기반) 공격자가 다음에 나올 코드를 추측할 수 있음. SecureRandom은 예측이 사실상 불가능함.
 */
public final class CodeGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();

    private CodeGenerator() {
    }

    // 000000 ~ 999999 범위의 6자리 숫자 문자열 (앞자리가 0이어도 6자리를 유지하기 위해 %06d로 zero-padding)
    public static String sixDigitCode() {
        int value = RANDOM.nextInt(1_000_000);
        return String.format("%06d", value);
    }
}
