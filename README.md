# 🏗️ BuildFlow — Construction & Worker Financial Management System

<div align="center">

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-purple.svg?style=for-the-badge&logo=kotlin)](https://kotlinlang.org)
[![Android](https://img.shields.io/badge/Android-API%2024%2B-green.svg?style=for-the-badge&logo=android)](https://android.com)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-blue.svg?style=for-the-badge&logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![Room Database](https://img.shields.io/badge/Room-SQLite%20Offline%20First-orange.svg?style=for-the-badge&logo=sqlite)](https://developer.android.com/training/data-storage/room)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg?style=for-the-badge)](https://opensource.org/licenses/MIT)

**BuildFlow** — Qurilish obyektlari, ishchilar davomati, kunlik ish haqi, avanslar, bonuslar va xarajatlarni to‘liq avtomatlashtirilgan holda boshqaruvchi zamonaviy Android ilovasi.

[Xususiyatlar](#-asosiy-imkoniyatlar) • [Arxitektura](#-arxitektura) • [O'rnatish](#-ornatish-va-ishga-tushirish) • [Texnologiyalar](#-texnologiyalar-steki) • [Litsenziya](#-litsenziya)

</div>

---

## 🌟 Asosiy Imkoniyatlar

### 🏢 1. Obyektlar Boshqaruvi
- Har bir obyekt bo‘yicha umumiy shartnoma summasi, tushumlar, xarajatlar va sof foyda hisobi.
- Barcha moliyaviy hisobotlar real vaqt rejimida avtomatik hisoblanadi.

### 👷 2. Ishchilar va Dinamik Kalendar
- **Dinamik Rangli Kalendar**:
  - 🟢 **Yashil kunlar**: To‘liq to‘langan ish kunlari;
  - 🟡 **Sariq kunlar**: Qarzga yozilgan (to‘lanishi kutilayotgan) kunlar;
  - 🔴 **Qizil kunlar**: Kelmagan (davomat qilinmagan) kunlar.
- **Yagona Standartdagi Boshqaruv**:
  - Kun ustiga bosib davomat, ish haqi, avans va bonuslarni boshqarish.
  - Bitta tugma orqali to‘lash (`CheckCircle`) yoki qarz holatiga o‘tkazish (`Pending`).
- **Shaffof 3 Ustunli Moliyaviy Tahlil**:
  1. 🟢 **Berilgan Pul**: Ishchiga haqiqatda berilgan barcha pullar.
  2. 🟡 **Qarz kunlar**: Kalendarda qarz sifatida yozilgan kunlik stavkalar va bonuslar.
  3. 🔴 **Sof Qarz**: Avanslar chegirilgandan keyingi haqiqiy sof qoldiq qarz.

### 💰 3. Xarajatlar va Kategoriyalar
- Qurilish materiallari, transport, ovqatlanish va boshqa xarajatlarni kategoriyalar bo‘yicha tahlil qilish.
- Standart kategoriyalar bilan birga yangi maxsus kategoriyalar qo‘shish va boshqarish.

### 📊 4. Hisobotlar va Eksport
- Obyektlar bo‘yicha batafsil moliyaviy tahlil.
- Ma'lumotlar bazasini xavfsiz zaxiralash (Backup) va qayta tiklash (Restore).

---

## 🏛️ Arxitektura va Texnologiyalar

Loyiha **Clean Architecture** va **MVI / MVVM** patternlari asosida yaratilgan:

```
app/src/main/java/uz/buildflow/app/
├── core/                  # Umumiy komponentlar, Theme, Utility'lar, DB instansiyasi
├── data/
│   ├── local/             # Room Entity, DAO va Mapper'lar
│   └── repository/        # Repository implementatsiyalari
├── domain/
│   ├── model/             # Sof Kotlin domain modellari va Enums
│   ├── repository/        # Repository interfeyslari
│   └── usecase/           # Sof biznes logika (UseCases)
├── di/                    # Dependency Injection (AppContainer)
└── presentation/          # Jetpack Compose UI va ViewModel'lar
    ├── common/            # Umumiy qayta ishlatiluvchi UI komponentlar
    ├── objects/           # Obyektlar ekrani
    ├── workers/           # Ishchilar, Dinamik Kalendar va To'lovlar
    ├── expenses/          # Xarajatlar va Kategoriyalar
    └── transactions/      # Tushumlar va Tarix
```

### 🛠️ Texnologiyalar Steki:
- **Language**: Kotlin 2.0.21
- **UI Toolkit**: Jetpack Compose, Material 3
- **Local Storage**: Room Database, SQLite (Offline-First)
- **Asynchronous**: Coroutines, Flow, StateFlow
- **Architecture**: Clean Architecture, Single Source of Truth (SSOT)
- **Dependency Injection**: Manual DI via AppContainer (engil va tez)

---

## 🚀 O'rnatish va Ishga Tushirish

### Talablar:
- Android Studio Ladybug / Meerkat yoki undan yuqori
- JDK 17 yoki 21
- Android SDK 35 (Minimum API 24 — Android 7.0+)

### Loyihani klonlash va ishga tushirish:
```bash
# 1. Loyihani yuklab oling
git clone https://github.com/mirmakhamat/build-flow.git

# 2. Loyiha papkasiga o'ting
cd build-flow

# 3. Release APK tayyorlash
./build_release.sh
```

---

## 🧪 Sinovlar va Testlar

Barcha moliyaviy hisob-kitoblar va qarz formulalari Unit Testlar orqali tekshirilgan:
```bash
./gradlew testDebugUnitTest
```

---

## 📄 Litsenziya

Ushbu loyiha [MIT Litsenziyasi](LICENSE) asosida tarqatiladi.
