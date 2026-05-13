## Sprint 5 — Backend validation bugs

### 1. POST /lists/{id}/invite не валидирует invitee_email

Принимает любой email и возвращает 200 OK, в том числе:
- email несуществующего в системе пользователя
- email текущего пользователя (самоприглашение)
- email уже-участника списка

Должен возвращать:
- 404 если пользователь с таким email не зарегистрирован
- 409 с serverMessage="self_invite" если приглашает сам себя
- 409 с serverMessage="already_member" если пользователь уже участник
- 409 с serverMessage="already_invited" если уже есть pending invite

Android-сторона уже умеет показывать корректные сообщения для всех
четырёх случаев (см. ShareScreen.kt sendInvite + NetworkError.Conflict).

### 2. POST /lists/{id}/items возвращает 500 для некоторых товаров

Наблюдалось при добавлении товаров "Багет", "Пицца". Тело ответа:
"ошибка создания товара". Логов сервера нет, нужны structured logs
(slog в JSON) со стороны Go-функции, чтобы понять причину.
