# 📊 BuildFlow — Loyiha Holati va Ijro Xaritasi (Project State)

> Ushbu fayl loyihaning joriy holatini, bajarilgan ishlarni va keyingi qadamlarni aniq qayd etib borish uchun xizmat qiladi. Har bir AI agent yoki dasturchi ishni boshlashdan oldin ushbu faylni o'qishi va har bir qadamdan so'ng uni yangilab borishi shart.

---

## 🛠️ Texnologik Stek
* **Platforma:** Android (Native)
* **Dasturlash tili:** Kotlin (1.9.24, JVM 17/21)
* **UI Framework:** Jetpack Compose (Material 3)
* **Mahalliy Baza (Local DB):** Room Database (SQLite, version 2.6.1 - 9 ta entity)
* **Arxitektura:** Clean Architecture (Domain, Data, Presentation) + MVVM
* **Asinxronlik:** Kotlin Coroutines + StateFlow / Flow
* **DI (Dependency Injection):** AppContainer (Clean manual DI)
* **Optimallashtirish:** R8 / Proguard Code & Resource Shrinking (1.4 MB Release APK)
* **Imzolash:** Release Keystore (Google Play va Production tayyor)
* **Offline & No Auth:** To'liq mahalliy (internet va auth talab etilmaydi, to'g'ridan-to'g'ri ishga tushadi)

---

## 🚦 Bajarilgan Barcha Imkoniyatlar (100% YAKUNLANDI)

### ✅ 1. Obyektga Asoslangan Navigatsiya (Object-Scoped Navigation)
- [x] **Bosh Ekranda:** Barcha obyektlar ro'yxati chiqadi.
- [x] **Obyekt Tanlanganda:** Pastda faqat shu obyektga tegishli 4 ta tab (`Dashboard`, `Ishchilar`, `Xarajatlar`, `Hisobot`) chiqadi.
- [x] **Tepada Ortga Tugmasi:** Barcha bo'limlardan bosh sahifaga (barcha obyektlar ro'yxatiga) qaytish tugmasi (`⬅`).
- [x] **Qat'iy Izolyatsiya:** Ishchilar, xarajatlar va hisobotlar faqat tanlangan obyektga bog'liq bo'ladi.

### ✅ 2. Barcha Modullarda To'liq CRUD va Tahrirlash
- [x] Obyektlar, Ishchilar, Davomat kunlari (Kalendar), Xarajatlar, Kirimlar/Avanslar, Ishchi to'lovlari, Bonuslarni to'liq tahrirlash va o'chirish.

### ✅ 3. Aqlli Input Maskalari va Refresh
- [x] Barcha summalarda 3 xonadan ajratish maskasi (`150 000 000 so'm`).
- [x] Telefon raqamlarida `+998 (90) 123-45-67` maskasi.
- [x] Barcha ekranlarda `Refresh` (🔄) tugmasi.
