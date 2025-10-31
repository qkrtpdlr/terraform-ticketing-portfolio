#!/usr/bin/env python3
"""
동시성 테스트 스크립트
1,000명이 동시에 100장 티켓 예매 경쟁
"""
import concurrent.futures
import requests
import json

ALB_URL = "http://YOUR_ALB_DNS/api"  # 실제 ALB DNS로 변경
EVENT_ID = 1
TOTAL_SEATS = 100
CONCURRENT_USERS = 1000

def reserve_ticket(user_id):
    try:
        response = requests.post(
            f"{ALB_URL}/reservations",
            json={
                "eventId": EVENT_ID,
                "userId": f"user{user_id}",
                "quantity": 1
            },
            timeout=10
        )
        return {
            "user_id": user_id,
            "status": response.status_code,
            "success": response.status_code == 201
        }
    except Exception as e:
        return {
            "user_id": user_id,
            "status": "error",
            "success": False,
            "error": str(e)
        }

if __name__ == "__main__":
    print(f"동시성 테스트 시작: {CONCURRENT_USERS}명이 {TOTAL_SEATS}장 티켓 경쟁")
    
    with concurrent.futures.ThreadPoolExecutor(max_workers=100) as executor:
        results = list(executor.map(reserve_ticket, range(CONCURRENT_USERS)))
    
    success_count = sum(1 for r in results if r["success"])
    fail_count = CONCURRENT_USERS - success_count
    
    print(f"\n결과:")
    print(f"- 성공: {success_count}건")
    print(f"- 실패: {fail_count}건")
    print(f"- 예상 성공: {TOTAL_SEATS}건")
    print(f"- 이중 예매 여부: {'없음 ✅' if success_count == TOTAL_SEATS else '발생 ❌'}")
