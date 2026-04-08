# Bank cards REST API

Spring Boot 3 (Java 17): управление картами, пользователями и переводами между своими картами. Аутентификация — JWT, роли `ADMIN` и `USER`. Номер карты хранится в БД в зашифрованном виде (AES-256-GCM), в API отдаётся только маска вида `**** **** **** 1234`.

## Запуск локально

1. Поднимите PostgreSQL (или используйте Docker Compose только для БД):

   ```bash
   docker compose up -d postgres
   ```

2. Задайте при необходимости переменные `JWT_SECRET` (строка ≥ 32 символов для HS256) и `CARD_ENCRYPTION_KEY` (Base64 от 32 байт). В `application.yml` указаны значения по умолчанию **только для разработки**.

3. Соберите и запустите приложение:

   ```bash
   ./mvnw spring-boot:run
   ```

   Либо: `mvn spring-boot:run`, если Maven установлен глобально.

4. Swagger UI: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)  
   Статическая OpenAPI-спецификация: [docs/openapi.yaml](docs/openapi.yaml).

## Запуск всего стека (приложение + PostgreSQL)

```bash
docker compose up --build
```

Профиль `docker` подключается автоматически; JDBC указывает на сервис `postgres`.

## Учётная запись после миграций

После первого старта Liquibase создаёт администратора (если ещё нет пользователя `admin`):

- **Логин:** `admin`  
- **Пароль:** `password`

Сразу смените пароль в продакшене и задайте собственные `JWT_SECRET` и `CARD_ENCRYPTION_KEY`.

## Возможности по ролям

| Действие | ADMIN | USER |
|----------|:-----:|:----:|
| CRUD пользователей (`/api/users`) | ✓ | |
| Создание / полное обновление / удаление карт | ✓ | |
| Смена статуса карты (`PATCH /api/cards/{id}/status`) | ✓ | |
| Просмотр карт (фильтры, пагинация, поиск) | все карты | только свои |
| Запрос блокировки своей карты (`POST .../request-block`) | | ✓ |
| Перевод между **своими** картами (`POST /api/cards/transfers`) | ✓* | ✓ |

\* Админ может переводить между любыми картами (служебный сценарий).

## Тесты

```bash
./mvnw test
```

## Технологии

Java 17, Spring Boot 3, Spring Security, JWT, Spring Data JPA, PostgreSQL, Liquibase, SpringDoc OpenAPI, Docker Compose.
