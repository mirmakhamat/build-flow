# 📜 BuildFlow — Bajarilgan Ishlar Tarixi (Changelog)

Barcha texnik o'zgarishlar, qo'shilgan modullar, funksiyalar va tuzatishlar ushbu faylda xronologik tartibda saqlanadi.

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
