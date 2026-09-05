// [이 파일이 왜 필요한가]
// 회원이 "부모"인지 "자녀"인지 구분하는 값. 문자열("PARENT"/"CHILD")을 그냥 쓰지 않고 enum으로 둔 이유는
// 오타(예: "Parent", "parent")로 잘못된 값이 DB에 들어가는 걸 컴파일 시점에 막기 위함.
package com.example.demo.auth;

public enum Role {
    PARENT,
    CHILD
}
