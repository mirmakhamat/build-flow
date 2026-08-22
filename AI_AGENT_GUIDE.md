# 🤖 BuildFlow: AI Agentlari Uchun To'liq Ishlash va Implementatsiya Qo'llanmasi

Ushbu qo'llanma **BuildFlow** loyihasida ishlovchi barcha AI agentlari, dasturchilar va arxitektorlar uchun yagona haqiqat manbai (**Single Source of Truth**) hisoblanadi. Har qanday yangi funksiya, UI komponent, ma'lumotlar bazasi o'zgarishi yoki biznes mantiq ushbu qo'llanmada keltirilgan qoidalarga 100% muvofiq bo'lishi shart.

---

## 📌 1. Loyiha Missiyasi va Falsafasi

### 1.1 Loyihaning Maqsadi
**BuildFlow** — qurilish, ta'mirlash (remont) va loyiha boshqaruvida ishlaydigan brigadirlar, prorablar va usta boshilari uchun mo'ljallangan **offline-first mobil moliyaviy-boshqaruv tizimi**.

Ilova shunchaki "ishchilar davomati daftarchasi" emas, balki **real vaqtdagi mini-fintech accounting (buxgalteriya va moliya) tizimidir**.

### 1.2 Hozirgi Bosqich Talablari (V1 - Offline Local Edition)
1. **To'liq Offline / Local:** Ilova internetga bog'liq emas. Barcha ma'lumotlar qurilmaning o'zida (Local SQLite / Room / Drift / Isar) saqlanadi.
2. **Auth (Autentifikatsiya) Yo'q:** Login, Register, SMS kod, parol tiklash ekranlari talab etilmaydi. Ilova ochilishi bilan to'g'ridan-to'g'ri asosiy ishchi muhitga (Obyektlar / Dashboard) o'tadi.
3. **Maksimal Tezkorlik va Qulaylik:** Qurilish maydonidagi usta bitta qo'li bilan 2 soniyada davomat, xarajat yoki kirim kiritishi mumkin bo'lishi kerak.
4. **Buxgalteriya jargonlaridan xoli sodda UI:** Debit, kredit, balans kabi qiyin so'zlar o'rniga oddiy xalq tilidagi terminlar: **Obyekt qiymati, Tushgan pul, Kutilayotgan pul, Xarajat, Qo'ldagi pul (Qoldiq), Ish haqi, Bonus, Yo'l kira**.

---

## 🧠 2. Qat'iy Biznes Qoidalari va Moliyaviy Formulalar

Har bir AI agent ushbu qoidalarni hech qachon buzmasligi kerak:

### 2.1 Ishchi va Ish Haqi Qoidasi (ENG MUHIM QOIDA)
> ⚠️ **Worker.default_rate orqali o'tmishdagi yoki joriy ish haqini hisoblash QAT'IYAN TAQIQLANADI!**

* `Worker.default_rate` — bu faqat **tavsiya etilgan / forma ochilganda avtomatik to'ldiriladigan** standart narx.
* Real hayotda ishchi bir kuni 250 000, ertasi kuni yarim kun ishlab 150 000, indinga qiyin ish qilgani uchun 350 000 so'm olishi mumkin.
* Har bir kunlik to'lov mustaqil ravishda `WorkerDay.payment_amount` maydonida saqlanadi.
* Agar keyinchalik `Worker.default_rate` o'zgartirilsa, o'tmishdagi `WorkerDay` yozuvlariga aslo ta'sir qilmasligi kerak.

### 2.2 Bonuslarning Ajratilishi
> ⚠️ **Bonusni aslo ish haqiga (`payment_amount`) qo'shib saqlamang!**

Bonuslar 2 xil bo'ladi va alohida jadvallarda saqlanadi:
1. **Kunlik bonus (`DailyBonus`):** Aniq bir ish kuni bilan bog'langan (`worker_day_id` ga ega). Masalan: 18-avgust kuni ortiqcha ishlagani uchun +100 000 so'm.
2. **Umumiy bonus (`GeneralBonus`):** Ma'lum bir kunga bog'lanmagan, obyekt doirasidagi rag'batlantirish (`object_id` va `worker_id` ga ega). Masalan: "Obyektni muddatidan oldin topshirgani uchun" +1 000 000 so'm.

### 2.3 Moliyaviy Hisob-kitob Formulalari

```text
┌─────────────────────────────────────────────────────────────────────────┐
│                           MOLIYAVIY FORMULALAR                          │
├─────────────────────────────────────────────────────────────────────────┤
│ 1. Obyekt Qiymati        = Object.total_price                           │
│ 2. Tushgan Pul (Kirim)   = SUM(MoneyTransaction.amount) [type=INCOME]   │
│ 3. Kutilayotgan Pul      = Object.total_price - Tushgan Pul             │
│                                                                         │
│ 4. Ishchilar Ish Haqi    = SUM(WorkerDay.payment_amount)                │
│ 5. Kunlik Bonuslar       = SUM(DailyBonus.amount)                       │
│ 6. Umumiy Bonuslar       = SUM(GeneralBonus.amount)                     │
│ 7. Boshqa Xarajatlar     = SUM(Expense.amount)                          │
│                                                                         │
│ 8. JAMI XARAJAT          = Ish Haqi + Kunlik Bonus + Umumiy Bonus       │
│                            + Boshqa Xarajatlar                          │
│                                                                         │
│ 9. QO'LDAGI PUL (Kassa)  = Tushgan Pul - JAMI XARAJAT                   │
│ 10. TAXMINIY FOYDA       = Obyekt Qiymati - JAMI XARAJAT                │
└─────────────────────────────────────────────────────────────────────────┘
```

> **Eslatma:** `Qo'ldagi pul` (real kassa qoldig'i) bilan `Taxminiy foyda` (obyekt oxirida kutilayotgan sof daromad) boshqa-boshqa tushunchalardir.

### 2.4 Ish Haqi Qarzdorligi va Avanslar (WorkerPayment)
* `WorkerDay.payment_amount` — bu ishchining shu kuni **ishlab topgan puli** (Earned).
* `WorkerPayment.amount` — bu ishchining qo'liga **haqiqatda berilgan pul** (Paid: oylik, kunlik to'lov yoki Avans).
* **Ishchiga bo'lgan qarz:** `Ishlab topgan jami puli (Ish haqi + Bonuslar) - Berilgan jami pul (WorkerPaymentlar)`.

### 2.5 Ma'lumotlarni Saqlash va O'chirish Tamoyili
Moliyaviy yozuvlarni darhol bazadan o'chirib yuborish (Hard Delete) tavsiya etilmaydi. Buning o'rniga:
* `is_deleted` flag yoki `status = 'CANCELLED'` mexanizmidan foydalaniladi.
* Har qanday tahrirlash yoki o'chirish holati `AuditLog` ga yozilishi tavsiya etiladi.

---

## 🗄️ 3. Ma'lumotlar Modeli (Database Schema & Entities)

Barcha jadvallar va ularning maydonlari:

```text
User (Kelajak uchun)
 │
 └── Object (Obyekt)
      │
      ├── Worker (Ishchi)
      │    │
      │    ├── WorkerDay (Kunlik davomat va to'lov)
      │    │      └── DailyBonus (Kunlik bonus)
      │    │
      │    ├── GeneralBonus (Umumiy bonus)
      │    │
      │    └── WorkerPayment (Ishchiga to'langan pul / Avans)
      │
      ├── Expense (Boshqa xarajatlar: Yo'l kira, Material, Ovqat va h.k.)
      │
      ├── MoneyTransaction (Obyektga tushgan pul / Kirim)
      │
      └── AuditLog (O'zgarishlar tarixi)
```

### 3.1 `objects` (Obyektlar)
| Maydon | Turi | Tavsif |
| :--- | :--- | :--- |
| `id` | `TEXT / UUID` | Birlamchi kalit |
| `name` | `TEXT` | Obyekt nomi (masalan: "Chilonzor 12-uy remonti") |
| `description` | `TEXT?` | Qo'shimcha ma'lumot |
| `total_price` | `REAL / INTEGER` | Obyektning kelishilgan umumiy summasi (so'm) |
| `currency` | `TEXT` | Standart: `UZS` |
| `start_date` | `TEXT / INTEGER` | Boshlangan sana (ISO-8601 yoki Unix timestamp) |
| `end_date` | `TEXT? / INTEGER?` | Rejalashtirilgan / tugagan sana |
| `status` | `TEXT` | `PLANNED`, `ACTIVE`, `COMPLETED`, `CANCELLED` |
| `created_at` | `INTEGER` | Yaratilgan vaqt |
| `updated_at` | `INTEGER` | O'zgartirilgan vaqt |

### 3.2 `workers` (Ishchilar)
| Maydon | Turi | Tavsif |
| :--- | :--- | :--- |
| `id` | `TEXT / UUID` | Birlamchi kalit |
| `object_id` | `TEXT` | Bog'langan obyekt ID si |
| `name` | `TEXT` | Ishchining F.I.Sh (masalan: "Ali Valiyev") |
| `phone` | `TEXT?` | Telefon raqami |
| `position` | `TEXT?` | Lavozimi / mutaxassisligi (Usta, Shogird, Santexnik, Malyar) |
| `default_rate` | `REAL / INTEGER` | Standart kunlik stavka (so'm) — faqat tavsiya! |
| `status` | `TEXT` | `ACTIVE`, `INACTIVE`, `FIRED` |
| `start_date` | `TEXT / INTEGER` | Ish boshlagan sana |
| `end_date` | `TEXT? / INTEGER?` | Ishdan ketgan sana |
| `notes` | `TEXT?` | Qo'shimcha izoh |
| `created_at` | `INTEGER` | Yaratilgan vaqt |
| `updated_at` | `INTEGER` | O'zgartirilgan vaqt |

### 3.3 `worker_days` (Kunlik Davomat va Ish Haqi)
| Maydon | Turi | Tavsif |
| :--- | :--- | :--- |
| `id` | `TEXT / UUID` | Birlamchi kalit |
| `worker_id` | `TEXT` | Ishchi ID |
| `date` | `TEXT` | Sana (`YYYY-MM-DD` formatida) |
| `status` | `TEXT` | `WORKED` (ishladi), `HALF_DAY` (yarim kun), `ABSENT` (kelmadi), `SICK` (kasal), `LEAVE` (javob olgan), `OTHER` |
| `payment_amount` | `REAL / INTEGER` | Shu kun uchun aniq to'lov summasi (so'm) |
| `payment_status` | `TEXT` | `UNPAID` (to'lanmagan), `PAID` (to'langan) |
| `note` | `TEXT?` | Izoh (masalan: "Qo'shimcha suvoq ishlari bo'ldi") |
| `created_at` | `INTEGER` | Yaratilgan vaqt |
| `updated_at` | `INTEGER` | O'zgartirilgan vaqt |

### 3.4 `daily_bonuses` (Kunlik Bonuslar)
| Maydon | Turi | Tavsif |
| :--- | :--- | :--- |
| `id` | `TEXT / UUID` | Birlamchi kalit |
| `worker_id` | `TEXT` | Ishchi ID |
| `worker_day_id` | `TEXT` | Kunlik davomat ID |
| `amount` | `REAL / INTEGER` | Bonus summasi (so'm) |
| `date` | `TEXT` | Sana (`YYYY-MM-DD`) |
| `reason` | `TEXT?` | Berilish sababi |
| `created_at` | `INTEGER` | Yaratilgan vaqt |

### 3.5 `general_bonuses` (Umumiy Obyekt Bonuslari)
| Maydon | Turi | Tavsif |
| :--- | :--- | :--- |
| `id` | `TEXT / UUID` | Birlamchi kalit |
| `worker_id` | `TEXT` | Ishchi ID |
| `object_id` | `TEXT` | Obyekt ID |
| `amount` | `REAL / INTEGER` | Bonus summasi (so'm) |
| `date` | `TEXT` | Sana (`YYYY-MM-DD`) |
| `reason` | `TEXT?` | Sababi (masalan: "Bosqichni vaqtida tugatgani uchun") |
| `created_at` | `INTEGER` | Yaratilgan vaqt |

### 3.6 `expenses` (Boshqa Xarajatlar)
| Maydon | Turi | Tavsif |
| :--- | :--- | :--- |
| `id` | `TEXT / UUID` | Birlamchi kalit |
| `object_id` | `TEXT` | Obyekt ID |
| `worker_id` | `TEXT?` | Agar xarajat ma'lum bir ishchiga tegishli bo'lsa (aks holda `NULL`) |
| `category` | `TEXT` | `TRANSPORT` (yo'l kira), `MATERIAL` (qurilish mollari), `FOOD` (ovqatlanish), `EQUIPMENT` (uskuna/ijara), `OTHER` (boshqa) |
| `amount` | `REAL / INTEGER` | Xarajat summasi (so'm) |
| `date` | `TEXT` | Sana (`YYYY-MM-DD`) |
| `description` | `TEXT?` | Izoh (masalan: "Sement va qum xaridi", "Ishchilarni olib kelish taksi") |
| `created_at` | `INTEGER` | Yaratilgan vaqt |
| `updated_at` | `INTEGER` | O'zgartirilgan vaqt |

### 3.7 `money_transactions` (Pul Kirimlari / Tranzaksiyalar)
| Maydon | Turi | Tavsif |
| :--- | :--- | :--- |
| `id` | `TEXT / UUID` | Birlamchi kalit |
| `object_id` | `TEXT` | Obyekt ID |
| `type` | `TEXT` | `INCOME` (mijozdan tushgan pul), `REFUND`, `ADJUSTMENT` |
| `amount` | `REAL / INTEGER` | Tushgan summa (so'm) |
| `date` | `TEXT` | Sana (`YYYY-MM-DD`) |
| `description` | `TEXT?` | Izoh (masalan: "1-bosqich avans to'lovi") |
| `created_at` | `INTEGER` | Yaratilgan vaqt |

### 3.8 `worker_payments` (Ishchiga Berilgan Real To'lovlar / Avanslar)
| Maydon | Turi | Tavsif |
| :--- | :--- | :--- |
| `id` | `TEXT / UUID` | Birlamchi kalit |
| `worker_id` | `TEXT` | Ishchi ID |
| `object_id` | `TEXT` | Obyekt ID |
| `amount` | `REAL / INTEGER` | Berilgan summa (so'm) |
| `date` | `TEXT` | Sana (`YYYY-MM-DD`) |
| `type` | `TEXT` | `SALARY` (ish haqi), `ADVANCE` (avans), `BONUS_PAYOUT` (bonus berish) |
| `description` | `TEXT?` | Izoh (masalan: "Karta orqali o'tkazildi") |
| `created_at` | `INTEGER` | Yaratilgan vaqt |

---

## 🎨 4. UI/UX Dizayn Tizimi va Standartlari

### 4.1 Vizual Stil va Ranglar Palitrasi
* **Konsepsiya:** Modern Fintech + Construction Management (toza, zich, raqamlar aniq ko'rinadigan).
* **Asosiy fon:** Light theme (`#F8FAFC` yoki `#FFFFFF`).
* **Asosiy rang (Primary):** Deep Blue / Indigo (`#1E3A8A` / `#2563EB`).
* **Muvaffaqiyat / Kirim / Ijobiy (Success):** Emerald Green (`#059669` / `#10B981`).
* **Xarajat / Chiqim / Qarz (Expense/Alert):** Crimson / Rose (`#DC2626` / `#EF4444`).
* **Ogohlantirish / Kutilayotgan (Warning):** Amber / Orange (`#D97706` / `#F59E0B`).
* **Matn rangi (Text Primary):** Slate 900 (`#0F172A`), Slate 500 (`#64748B`).

### 4.2 Formatlash Qoidalari
* Barcha valyuta qiymatlari minglik ajratuvchi bo'sh joy bilan yoziladi: `250 000 so'm`, `150 000 000 so'm`.
* Sanalar lokal formatda: `18-avgust 2026` yoki `18.08.2026`.
* Katta jadvallar o'rniga: **Kompakt kartochkalar (Cards)**, **Statistik bloklar (Metrics)** va **Pastki ochiluvchi modallar (Bottom Sheets)** ishlatiladi.

### 4.3 Navigatsiya Strukturasi (Bottom Navigation - 4 Ta Asosiy Tab)
```text
┌────────────────────────────────────────────────────────┐
│  [🏢 Obyektlar]   [👷 Ishchilar]   [💸 Xarajatlar]   [📊 Hisobot] │
└────────────────────────────────────────────────────────┘
```
1. **Obyektlar (Objects Tab):** Barcha obyektlar ro'yxati, obyekt tanlanganda uning to'liq moliyaviy dashboardi ochiladi.
2. **Ishchilar (Workers Tab):** Tanlangan obyekt bo'yicha yoki umumiy ishchilar ro'yxati, ularning balansi, ishlagan kunlari va kalendari.
3. **Xarajatlar (Expenses Tab):** Kunlik, oylik va kategoriyalar bo'yicha (Ish haqi, Bonus, Yo'l kira, Material, Boshqa) barcha xarajatlar tahlili.
4. **Hisobot (Reports Tab):** Obyekt daromadliligi, pul oqimi (Cashflow), kunlik/oylik xulosalar va breakdown.

---

## 📱 5. Ekranlar va Foydalanuvchi Oqimlari (Screens & User Flows)

### 5.1 Bosh Ekran (Obyektlar Ro'yxati)
```text
┌──────────────────────────────────────────────┐
│  BuildFlow                      [⚙️ Sozlamalar]│
│                                              │
│  OBYEKTLAR                                   │
│  ──────────────────────────────────────────  │
│  ┌────────────────────────────────────────┐  │
│  │ 🏢 Chilonzor 12-uy remonti   [ACTIVE] │  │
│  │ 💰 150 000 000 so'm                    │  │
│  │ ────────────────────────────────────── │  │
│  │ Tushgan:  100 000 000  │ Qoldiq: 40 mln│  │
│  │ Xarajat:   60 000 000  │ Ishchilar: 12 │  │
│  └────────────────────────────────────────┘  │
│                                              │
│  ┌────────────────────────────────────────┐  │
│  │ 🏢 Yunusobod hovli qurilishi [ACTIVE]  │  │
│  │ 💰 85 000 000 so'm                     │  │
│  │ ────────────────────────────────────── │  │
│  │ Tushgan:   60 000 000  │ Qoldiq: 27.5 m│  │
│  │ Xarajat:   32 500 000  │ Ishchilar: 8  │  │
│  └────────────────────────────────────────┘  │
│                                              │
│                                      [➕ Yangi]│
└──────────────────────────────────────────────┘
```

### 5.2 Obyekt Ichki Dashboardi (Asosiy Boshqaruv Markazi)
Obyekt kartochkasi bosilganda ochiladi:
```text
┌──────────────────────────────────────────────┐
│  ← Chilonzor 12-uy remonti       [✏️ Tahrir]  │
├──────────────────────────────────────────────┤
│  MOLIYAVIY XULOSA                            │
│  ┌────────────────────────────────────────┐  │
│  │ 🏢 Obyekt Qiymati:   150 000 000 so'm   │  │
│  │ 💵 Tushgan Pul:      100 000 000 so'm   │  │
│  │ ⏳ Kutilmoqda:        50 000 000 so'm   │  │
│  │ ────────────────────────────────────── │  │
│  │ 📉 Jami Xarajat:      60 000 000 so'm   │  │
│  │ 🟢 QO'LDAGI PUL:      40 000 000 so'm   │  │
│  └────────────────────────────────────────┘  │
│                                              │
│  TEZKOR STATISTIKA                           │
│  ┌────────────────────┐ ┌──────────────────┐ │
│  │ 👷 12 Ishchi       │ │ 📅 183 Ish kuni  │ │
│  └────────────────────┘ └──────────────────┘ │
│                                              │
│  XARAJATLAR BREAKDOWN                        │
│  • Ish haqi:        40 000 000 so'm (66.7%)  │
│  • Bonuslar:         5 000 000 so'm  (8.3%)  │
│  • Yo'l kira:        4 000 000 so'm  (6.7%)  │
│  • Material:         8 000 000 so'm (13.3%)  │
│  • Boshqa:           2 000 000 so'm  (3.3%)  │
│                                              │
│  [➕ TEZKOR KIRITISH (Davomat/Xarajat/Kirim)] │
└──────────────────────────────────────────────┘
```

### 5.3 Ishchi Profili va Kalendar Ekrani (Calendar-First UI)
```text
┌──────────────────────────────────────────────┐
│  ← Ali Valiyev (Usta)            [✏️ Tahrir]  │
│  Standart stavka: 250 000 so'm / kun          │
├──────────────────────────────────────────────┤
│  UMUMIY STATISTIKA (Avgust 2026)             │
│  • Ishlagan kun:   18 kun                    │
│  • Ish haqi:       4 800 000 so'm            │
│  • Bonuslar:       1 500 000 so'm            │
│  • Jami topgani:   6 300 000 so'm            │
├──────────────────────────────────────────────┤
│                AVGUST 2026                   │
│   Du   Se   Ch  Pay   Ju   Sh   Ya           │
│   17   18   19   20   21   22   23           │
│  [✓]  [✓]  [-]  [✓]  [✓]  [-]  [-]           │
│  250k 300k  0   250k 250k  0    0            │
├──────────────────────────────────────────────┤
│  Tanlangan kun: 18-Avgust                    │
│  Holat: Ishladi (To'liq kun)                 │
│  To'lov: 300 000 so'm (Qo'shimcha ish)       │
│  Bonus: 100 000 so'm (Tez bajargani uchun)   │
│  [ ✏️ KUNNI TAHRIRLASH ]                     │
└──────────────────────────────────────────────┘
```

### 5.4 Guruhli / Kunlik Tezkor Davomat Ekrani
Har kuni 12 ta ishchini alohida kiritib o'tirmaslik uchun **Guruhli Davomat kiritish oynasi**:
```text
┌──────────────────────────────────────────────┐
│  ← Kunlik Davomat: 18-Avgust 2026   [SAQLASH]│
├──────────────────────────────────────────────┤
│  [✓ Barchasini belgilash]                    │
│                                              │
│  ☑️ Ali Valiyev                              │
│     Holat: [Ishladi ▼]  Summa: [300 000]     │
│                                              │
│  ☑️ Vali Aliyev                              │
│     Holat: [Ishladi ▼]  Summa: [250 000]     │
│                                              │
│  ☑️ Sardor Karimov                           │
│     Holat: [Yarim kun ▼] Summa: [150 000]    │
│                                              │
│  ⬜ Jasur Toshov                             │
│     Holat: [Kelmadi ▼]  Summa: [0]           │
└──────────────────────────────────────────────┘
```

### 5.5 Tezkor Harakatlar Menusi (Global Floating Action Button `+`)
Foydalanuvchi `+` tugmasini bosganda quyidagi Bottom Sheet modal chiqadi:
```text
┌──────────────────────────────────────────────┐
│  Yangi Yozuv Kiritish                        │
│                                              │
│  [ 📅 Kunlik Davomat Kiritish ]              │
│  [ 💸 Xarajat Kiritish (Yo'l kira, Material)]│
│  [ 💵 Obyektga Pul Kirimi (Mijoz to'lovi) ]  │
│  [ 🎁 Ishchiga Bonus Yozish ]                │
│  [ 👷 Yangi Ishchi Qo'shish ]                │
│  [ 🏢 Yangi Obyekt Yaratish ]                │
└──────────────────────────────────────────────┘
```

---

## 🛠️ 6. Texnik Arxitektura va Implementatsiya Standartlari

AI agentlari kod yozishda quyidagi arxitektura tamoyillariga rioya qilishi shart:

### 6.1 Clean Architecture Qatlamlari
```text
lib/ / src/
├── core/
│   ├── theme/           # Ranglar, shriftlar, UI o'lchamlari
│   ├── utils/           # Valyuta formatlash, sana converterlar
│   ├── database/        # SQLite / Local DB helper, migratsiyalar
│   └── constants/       # Enums, kategoriyalar, statuslar
├── data/
│   ├── models/          # DB Entity modellari (JSON/SQLite mapping)
│   ├── repositories/    # Mahalliy DB bilan ishlovchi repositorylar
│   └── datasources/     # Mahalliy DAO (Data Access Object) lar
├── domain/
│   ├── entities/        # Sof biznes obyektlari
│   ├── usecases/        # Hisob-kitoblar, kiritish/o'chirish logikasi
│   └── repositories/    # Repository interfeyslari
└── presentation/
    ├── screens/         # Dashboard, Objects, Workers, Expenses, Reports
    ├── widgets/         # Qayta ishlatiluvchi kartochkalar, modallar, inputlar
    └── state/           # State management (Bloc / Riverpod / ViewModel / Provider)
```

### 6.2 Hisob-kitob Engine Logikasi (Financial Calculation Engine)
Barcha hisob-kitoblar SQL darajasida yoki sof Domain UseCase darajasida amalga oshiriladi:

```dart
// Misol: Obyekt hisob-kitob modeli
class ObjectFinancialSummary {
  final double totalPrice;          // Obyekt qiymati
  final double receivedMoney;       // Jami tushgan pul
  final double workerSalaryTotal;   // Jami ish haqi
  final double dailyBonusesTotal;   // Jami kunlik bonuslar
  final double generalBonusesTotal; // Jami umumiy bonuslar
  final double otherExpensesTotal;  // Jami boshqa xarajatlar

  double get remainingReceivable => totalPrice - receivedMoney;
  
  double get totalBonus => dailyBonusesTotal + generalBonusesTotal;
  
  double get totalExpense => 
      workerSalaryTotal + totalBonus + otherExpensesTotal;
      
  double get cashBalance => receivedMoney - totalExpense;
  
  double get estimatedProfit => totalPrice - totalExpense;
}
```

### 6.3 Local Database Queries va Indexlar
Tezkor ishlash uchun quyidagi SQL indekslar bo'lishi shart:
* `CREATE INDEX idx_worker_days_worker_date ON worker_days(worker_id, date);`
* `CREATE INDEX idx_worker_days_date ON worker_days(date);`
* `CREATE INDEX idx_expenses_object_date ON expenses(object_id, date);`
* `CREATE INDEX idx_expenses_category ON expenses(category);`
* `CREATE INDEX idx_money_tx_object ON money_transactions(object_id);`
* `CREATE INDEX idx_daily_bonuses_day ON daily_bonuses(worker_day_id);`

---

## 📋 7. AI Agentlari Uchun Bosqichma-bosqich Vazifalar Rejasi (Roadmap)

AI agentlari vazifalarni quyidagi tartibda amalga oshirishi lozim:

### 1-Bosqich: Database va Data Layer (Poydevor)
- [ ] Mahalliy ma'lumotlar bazasini yaratish (barcha 8 ta jadval va indekslar).
- [ ] DAO (Data Access Object) va Repository qatlamlarini to'liq CRUD bilan qurish.
- [ ] Barcha moliyaviy hisob-kitob formulalarini (Summary UseCase) unit testlar bilan mustahkamlash.

### 2-Bosqich: Obyektlar va Boshqaruv Ekranlari
- [ ] Obyektlar ro'yxati ekrani (Obyekt kartochkasi, statuslar, qoldiq).
- [ ] Obyekt yaratish / tahrirlash dialogi va formasi.
- [ ] Obyekt ichki moliyaviy Dashboard ekrani (Fintech style metric cardlar).

### 3-Bosqich: Ishchilar va Davomat Tizimi
- [ ] Ishchilar ro'yxati va yangi ishchi qo'shish formasi (`default_rate` bilan).
- [ ] Ishchi profili va Kalendar ko'rinishi (kunlik statuslar, to'lovlar, bonuslar).
- [ ] Guruhli kunlik davomat kiritish ekrani (barcha ishchilarni 1 marta bosishda belgilash).
- [ ] Kunlik va Umumiy bonus qo'shish modallari.

### 4-Bosqich: Xarajatlar va Kirimlar Boshqaruvi
- [ ] Xarajat qo'shish formasi (Kategoriyalar: Yo'l kira, Material, Ovqat, Uskuna, Boshqa).
- [ ] Xarajatlar ro'yxati (filtrlash: bugun, bu oy, kategoriya bo'yicha).
- [ ] Pul kirimlari ekrani (Mijozdan olingan pullarni qayd qilish).

### 5-Bosqich: Hisobotlar va Detalizatsiya
- [ ] Oylik va kunlik xarajatlar breakdown ekrani.
- [ ] Ishchilar bo'yicha daromad va qarz balansi hisoboti.
- [ ] Obyektning sof foydasi va kassa oqimi tahlili.
- [ ] Ma'lumotlarni zaxiralash / eksport qilish (JSON/Excel local export - kelajak uchun poydevor).

---

## 🔒 8. Sifat Nazorati va Xatoliklarni Oldini Olish Qoidalari

Har bir agent o'z kodini topshirishdan oldin quyidagilarni tekshirishi shart:
1. **Formula to'g'riligi:** `cashBalance = received - expense` va `totalExpense = salary + bonus + others` doimo to'g'ri ishlayotganini tekshirish.
2. **Default Rate mustaqilligi:** Ishchining standart narxi o'zgarganda o'tmishdagi kunlik to'lovlar buzilib ketmasligini ta'minlash.
3. **Formatlash:** Hech qanday ekranda `250000.0` ko'rinmasligi, doimo `250 000 so'm` formatida chiqishi.
4. **Offline barqarorlik:** Dastur internet yo'qligida hech qachon qotib qolmasligi yoki crash bo'lmasligi.
5. **No Auth:** Hech qanday joyda login/token tekshiruvi bo'lmasligi, dastur to'g'ridan-to'g'ri ochilishi.
