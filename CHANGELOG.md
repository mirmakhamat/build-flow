# 📜 BuildFlow — Bajarilgan Ishlar Tarixi (Changelog)

Barcha texnik o'zgarishlar, qo'shilgan modullar, funksiyalar va tuzatishlar ushbu faylda xronologik tartibda saqlanadi.

---

## [1.8.0-WorkerTransferRepairAndCleanUI] — 2026-09-25

### 🛠️ Ishchi ko'chirish xatosi tuzatildi va baza avtomatik tiklanadi
1. **Sabab**: eski "Ko'chirish" faqat `workers.object_id` ni o'zgartirgan. Kunlar ishchi bilan yangi obyektga o'tib ketgan, to'lovlar va umumiy bonuslar eski obyektda qolgan — yangi obyektda to'langan kunlar qarz bo'lib ko'ringan.
2. **Kod**: `transferWorkers` olib tashlandi; mavjud ishchining obyekti o'zgartirilmaydi. Boshqa obyektga faqat Import (alohida yozuv) orqali qo'shiladi.
3. **Baza (Room 4 → 5 migratsiya, `MovedWorkersRepair`)**: zarar ko'rgan har bir ishchi uchun eski obyektda alohida (Nofaol) yozuv yaratiladi, ko'chirishgacha bo'lgan kunlar, bonuslar va to'lovlar unga qaytariladi. Ko'chirishdan keyin yangi kassadan eski kun uchun berilgan pul `payer_object_id` bilan saqlanadi.
4. **Zaxiradan tiklash**: Room ulanishi yopilib, ilova qayta ishga tushadi (oldin fayl ochiq baza ustiga yozilardi).

### 📊 Hisobot aniqligi
* Ishchilar qarzi har bir ishchi bo'yicha alohida hisoblanadi (bir ishchiga ortiqcha to'lov boshqasining qarzini yopmaydi).
* "Kassadan chiqqan pul" ro'yxati endi ishchi to'lovlari va boshqa obyekt uchun to'langan xarajatlarni ham ko'rsatadi; kassalararo o'tkazma xarajatlar ro'yxatiga qo'shilmaydi.
* Guruhli davomat bir kunni qayta saqlaganda to'lovni takror yozmaydi; nofaol ishchilar ro'yxatda chiqmaydi.

### 🧹 Sodda interfeys
* Boshqa obyekt kassasidan to'lash, to'lov sanasi, kassalararo o'tkazma — "Qo'shimcha sozlamalar" yig'iladigan bo'limida.
* Hisobotdagi obyektlararo qatorlar bitta "Obyektlararo hisob-kitob" bo'limida.
* Baza eksport/import va boshqa obyektdan ishchi qo'shish — yuqoridagi ⋮ menyuda; import tasdiqlash so'raydi.

---

## [1.7.19-AddUnpaidDaysAccruedMetricToAllScreens] — 2026-08-22

### 🎯 Qarzga Yozilgan Kunlar/Bonuslar Summasi Barcha Ekranlarda Shaffof Ko'rsatildi
1. **Yangi Ko'rsatkich (`totalUnpaidAccrued`)**:
   * `WorkerStats` modeliga `totalUnpaidAccrued` (Qarz deb belgilangan kunlik stavkalar va qarzga yozilgan bonuslarning to'g'ridan-to'g'ri yig'indisi) qo'shildi.
2. **Ishchi Statistikasi va To'lovlar Tarixi Ekranlarida**:
   * Endi barcha ekranlarda moliyaviy tahlil 3 ta aniq ustunda aks etadi:
     - 🟢 **Berilgan Pul:** Ishchiga haqiqatda berilgan barcha pullar yig'indisi (`totalPaid`).
     - 🟡 **Qarz kunlar:** Kalendarda qarz sifatida turgan kunlik stavkalar va bonuslar summasi (`totalUnpaidAccrued`).
     - 🔴 **Sof Qarz:** Avanslar va ortiqcha to'lovlar chegirilgandan keyingi haqiqiy to'lanishi kerak bo'lgan sof qoldiq qarz (`remainingDebtToWorker`).
