Custom Magisk Manager

Кастомный менеджер для управления Magisk-модулем CustMagisk и набором функций для диагностики/безопасности с консолью действий (логи, таймеры, факты).
Поддержка: Android 8.0+

Важно:
- Часть функций требует root.
- Если найден MagiskSU и выдан root-доступ — Magisk-режим включается автоматически.
- Если Magisk скрыт/переименован, менеджер всё равно может работать через MagiskSU.


ВОЗМОЖНОСТИ

Менеджер (APK)
- Консоль действий снизу: вывод логов, статусы, сообщения.
- Таймер ожидания для действий (перезагрузка/отмена/критичные операции).
- “Полезные факты” из facts.txt (без ломания фактов переносами строк).
- Диагностика окружения (su, root, базовая информация).
- Управление модулем CustMagisk:
  - скачать модуль (URL в конфиге)
  - выбрать ZIP из файлов
  - установить ZIP как Magisk-модуль
  - включить/выключить/удалить модуль
  - проверить состояние модуля
- DenyList: предупреждение с ожиданием 10 секунд перед действием + кнопка перехода в Magisk (для ручной настройки).

Ядро (Rust core)
- Встраивается в APK как бинарник (arm64-v8a + armeabi-v7a) и запускается через su -c.
- Стрим логов построчно в консоль (ts|LEVEL|message + DATA JSON).
- Команды:
  - env
  - sha256 <path>
  - module_state <id>
  - module_enable <id> / module_disable <id>
  - module_install <zip> (через magisk --install-module)
  - module_remove <id> / modules_remove_all
  - module_api <action> <arg> [timeout_ms]

Модуль (Magisk CustMagisk)
- Создаёт /data/adb/CustMagisk/ и ведёт:
  - state.json (состояние/инфо)
  - logs/module.log
  - api/request.json и api/response.json
- Реализует файловое API (polling), чтобы менеджер мог получать диагностику и выполнять безопасные задачи.


СТРУКТУРА РЕПОЗИТОРИЯ

.
|-- app/                 Android приложение
|-- core/                Rust ядро (CLI бинарник)
`-- module/
    `-- CustMagisk/       структура Magisk-модуля (сборка в ZIP)


СБОРКА APK

Требования
- Android Studio
- Android SDK + NDK (SDK Manager -> SDK Tools -> NDK (Side by side))
- Rust + cargo-ndk (для сборки core)

1) Собрать Rust core и положить в assets (Windows)

cd core
cargo build --release --target aarch64-linux-android
cargo build --release --target armv7-linux-androideabi

mkdir ..\app\src\main\assets\core\arm64-v8a
mkdir ..\app\src\main\assets\core\armeabi-v7a

copy /Y target\aarch64-linux-android\release\core ..\app\src\main\assets\core\arm64-v8a\core
copy /Y target\armv7-linux-androideabi\release\core ..\app\src\main\assets\core\armeabi-v7a\core

2) Собрать APK
- Android Studio -> Build -> Build APK(s) (debug)
- или Build -> Generate Signed Bundle / APK -> APK (release)


СБОРКА ZIP МОДУЛЯ

Папка модуля: module/CustMagisk/
В ZIP файлы должны лежать в корне:
- module.prop
- post-fs-data.sh
- service.sh
- uninstall.sh

Собирай ZIP так, чтобы внутри не было лишней вложенной папки CustMagisk/.


УСТАНОВКА

1) Установить APK
Обычной установкой.

2) Установить модуль (2 варианта)
- Через приложение: Magisk -> Модуль CustMagisk -> Выбрать ZIP -> Установить
- Или вручную: через Magisk -> Modules -> Install from storage

После установки модуля рекомендуется перезагрузка.


ПРОВЕРКА РАБОТЫ

В приложении:
- Magisk -> Диагностика / Env — выводит данные окружения.

После установки модуля:
- module_api ping ""
- module_api collect_basic ""
- module_api collect_logs ""


НАСТРОЙКА DENYLIST

DenyList настраивается через Magisk.
В менеджере для DenyList всегда показывается предупреждение с ожиданием 10 секунд и кнопкой “Открыть Magisk”.


ROADMAP
- Улучшение UI/анимаций консоли и таймеров
- Расширение безопасного API модуля (диагностика, экспорт логов, проверки целостности)
- Улучшение установки/обновления модуля из Release-ссылок
