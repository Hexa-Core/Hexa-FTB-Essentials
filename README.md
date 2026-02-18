# FTB Essentials (HexaCore patched)

## Що це
Це пропатчена версія FTB Essentials 1.20.1, яка додає self-claim для китів:
- `/kit claim <kit>`
- `/kit list` (показує лише доступні кити)

Кити видаються через внутрішню логіку FTB Essentials, тому cooldown працює правильно.

## Вимоги
- Minecraft Forge 1.20.1
- FTB Library 2001.x (обовʼязково)
- LuckPerms Forge 5.4+ (рекомендовано для permission)
- FTB Ranks 2001.x (опційно)

## Як встановити
1. Заміни офіційний `ftb-essentials-forge-2001.x.jar` на цей зібраний jar.
2. Переконайся, що `ftb-library-forge` встановлений.
3. Запусти сервер 1 раз, щоб створився конфіг.
4. Відредагуй конфіг `config/hexacore-kitclaim.toml` при потребі.
5. Перезапусти сервер.

## Команди
- `/kit claim <kit>`
- `/kit list`

## Permissions (LuckPerms)
Система доступу:
- `hexacore.kit.claim.start`
- `hexacore.kit.claim.food`
- `hexacore.kit.claim.premium`
- `hexacore.kit.claim.deluxem`
- `hexacore.kit.claim.ultra`
- `hexacore.kit.claim.legend`

Якщо кит у `publicKits`, permission не потрібен.

## Конфіг
Файл: `config/hexacore-kitclaim.toml`

Дефолт:
```
publicKits = ["start", "food"]

[kitPermissions]
premium = "hexacore.kit.claim.premium"
deluxem = "hexacore.kit.claim.deluxem"
ultra = "hexacore.kit.claim.ultra"
legend = "hexacore.kit.claim.legend"
```

## Tebex (приклад)
Після покупки:
```
lp user {username} parent add premium
```

Група `premium` має містити:
```
hexacore.kit.claim.premium
```

Тоді гравець може:
```
/kit claim premium
```

## Збірка
```
.\gradlew.bat :forge:build
```

Вихідний jar:
- `forge/build/libs/ftb-essentials-forge-<version>.jar`

## Технічні примітки
- `/kit` доступний всім, якщо `KIT enabled`, але адмін-команди залишаються тільки для OP.
- LuckPerms викликається через reflection (без compile-time залежності).
