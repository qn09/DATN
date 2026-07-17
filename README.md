# DATN Exchange Platform

DATN Exchange Platform la he thong mo phong san giao dich tai san so duoc xay dung de nghien cuu kien truc backend tai chinh, xu ly giao dich spot va bao mat ung dung web.

Project duoc to chuc theo mo hinh monorepo, tach rieng ung dung nguoi dung va ung dung quan tri:

```text
DATN/
|-- exchange-backend/   # API giao dich va tai khoan nguoi dung
|-- user-frontend/      # Giao dien React cho nguoi dung
|-- admin-backend/      # API quan tri doc lap
|-- admin-frontend/     # Giao dien React cho quan tri vien
`-- docker-compose.yml  # PostgreSQL development environment
```

## Tinh nang chinh

- Dang ky, dang nhap va xac thuc bang JWT.
- Phan quyen user/admin va kiem tra quyen so huu account.
- Quan ly vi nhieu tai san voi available balance va locked balance.
- Double-entry ledger, idempotency va audit history cho cac bien dong tai chinh.
- Dat lenh BUY/SELL, order book, matching engine va trade settlement.
- Mo phong thanh khoan theo gia Binance Spot.
- Gia thi truong theo thoi gian thuc va dinh gia portfolio bang USDT.
- Rate limiting toan he thong, theo IP va rieng cho API login.
- Dashboard quan tri doc lap de theo doi account, order, trade va thi truong.

## Cong nghe

- Java 17, Spring Boot 3, Spring Security va Spring JDBC.
- PostgreSQL 16 va Docker Compose.
- JWT HMAC-SHA256, BCrypt va Bucket4j.
- React, Vite va Lucide React.
- Maven, JUnit va npm.

Day la project phuc vu hoc tap va do an tot nghiep. Cac giao dich, so du va thanh khoan thi truong trong he thong deu la du lieu mo phong, khong su dung tai san that.
