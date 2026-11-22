# user-service

Консольное CRUD-приложение на Java 17 + Hibernate + PostgreSQL.

## Требования

- JDK 17+
- Docker и Docker Compose
- Maven (или запуск через IntelliJ IDEA)

## Запуск базы данных

В корне проекта лежат `docker-compose.yml` и `init.sql`. Таблица `users` создаётся автоматически из `init.sql` при первом запуске.

```bash
docker compose up -d
```

Проверить, что БД поднялась:

```bash
docker compose ps
docker compose logs -f postgres
```

Параметры подключения (совпадают с `hibernate.cfg.xml`):

| Параметр | Значение |
|----------|----------|
| Хост     | localhost |
| Порт     | 5432 |
| База     | user_service |
| Логин    | postgres |
| Пароль   | postgres |

## Остановка базы данных

Данные сохраняются в томе `pgdata`:

```bash
docker compose stop     # остановить контейнер
docker compose start    # запустить снова
docker compose down     # удалить контейнер, данные остаются
```

## Полный сброс базы (данные будут удалены)

```bash
docker compose down -v
docker compose up -d
```

Скрипт `init.sql` выполняется только при первом запуске с пустым томом. Если меняешь структуру таблицы, пересоздай том командой выше.

## Запуск приложения

```bash
mvn compile exec:java
```

или запусти класс `Main` из IDE.

## Логи

Пишутся в консоль (INFO и выше) и в файл `logs/user-service.log` (с SQL-запросами).

## Тесты

```bash
mvn test
```

- `service/UserServiceTest` и `utils/UserValidatorTest` — юнит-тесты (JUnit 5 + Mockito), Docker не нужен.
- `dao/UserDaoImplIntegrationTest` — интеграционные тесты DAO на настоящем PostgreSQL в контейнере (Testcontainers).
  **Docker должен быть запущен.** Тестовая БД поднимается и удаляется автоматически и не затрагивает БД из `docker-compose.yml`.