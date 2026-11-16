# NovaGram

**NovaGram** — современный Android-клиент на актуальной кодовой базе официального Telegram (`13.2.2`, `targetSdk 36` / Android 16–17) с переключаемым дизайном **Material You** и **Liquid Glass**, собственной системой плагинов **NovaPlugins** и встроенным автообновлением через **GitHub Releases**.

---

## Основные возможности

- **Актуальная база Telegram Android** — полная поддержка современных протоколов, Edge-to-Edge и Predictive Back для Android 16–17.
- **Выбор стиля интерфейса в настройках** (`Настройки NovaGram → Стиль интерфейса`):
  - **Material You + Liquid Glass (Гибрид)** — динамическая палитра системы (Android 12–17 `system_accent*` / `system_neutral*`) + аппаратный шейдер Liquid Glass (Android 13–17).
  - **Material You** — чистая адаптивная палитра под ваши обои и тему (с опциональной покраской пузырей сообщений).
  - **Liquid Glass** — полупрозрачные стеклянные панели и размытие без Material You-переопределения цветов.
  - **Классический Telegram** — стандартный внешний вид.
- **Собственная система плагинов (NovaPlugins)**:
  - Изолированный движок плагинов на JavaScript ES6 (Mozilla Rhino) без раздувания APK.
  - Формат файлов `.nvplugin` / `.nv.js` (установка через меню в настройках или прямо из чата).
  - Поддержка чат-команд (`.ping`, `.shrug`, `.reverse`, `.mock`), хуков отправки сообщений, пунктов меню сообщений и декларативных настроек плагина.
  - Подробная документация: [`docs/PLUGINS.md`](docs/PLUGINS.md).
- **Автообновление через GitHub Releases (`h6rnyxxx/NovaGram`)**:
  - Проверка обновлений на старте и по кнопке **«Проверить обновления»** в настройках NovaGram.
  - Показ списка изменений (Changelog), баннер прогресса загрузки и установка APK прямо из приложения.

---

## Сборка и релизы (GitHub Actions)

Готовый workflow для автоматической сборки APK и публикации релизов находится в [`ci/build.yml`](ci/build.yml).
Чтобы активировать GitHub Actions в репозитории, перенесите или скопируйте содержимое [`ci/build.yml`](ci/build.yml) в `.github/workflows/build.yml` (через веб-интерфейс GitHub или локально после выдачи токена со scope `workflow`):
1. Перейдите в **Settings → Secrets and variables → Actions** вашего репозитория и при желании добавьте свои ключи Telegram API:
   - `NOVA_APP_ID` (с [my.telegram.org](https://my.telegram.org))
   - `NOVA_APP_HASH`
   - *(Опционально для своей подписи)* `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.
2. Чтобы автоматически собрать APK и выпустить **GitHub Release**, создайте и отправьте тег:
   ```bash
   git tag v1.0.0
   git push origin v1.0.0
   ```
   После завершения workflow собранный `NovaGram-universal.apk` автоматически появится в разделе **Releases**.

---

## Лицензия

Основано на исходном коде [Telegram for Android](https://github.com/DrKLO/Telegram) (GNU GPL v2 or later, см. [LICENSE](LICENSE)).
