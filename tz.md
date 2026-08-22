# Texnik topshiriq: Obyekt, ishchilar, davomat va xarajatlarni boshqarish mobil ilovasi

## 1. Loyiha maqsadi

Mobil ilovaning asosiy vazifasi — qurilish/remont va shunga o‘xshash obyektlarda:

* obyektlarni boshqarish;
* obyekt qiymati va tushgan pullarni hisobga olish;
* ishchilarni biriktirish;
* har bir ishchining har kungi ishlagan kunini qayd qilish;
* har bir kun uchun ishchiga **alohida to‘lov summasini** belgilash;
* ishchining kunlik va umumiy bonuslarini qayd qilish;
* yo‘l kira va boshqa xarajatlarni kiritish;
* barcha xarajatlarni avtomatik jamlash;
* obyektning tushumi, xarajati va qolgan pulini ko‘rsatish;
* barcha ma'lumotlarni kunlik, oylik va obyekt kesimida ko‘rish.

Muhim biznes qoidasi:

> **Ishchining bitta doimiy `daily_rate` qiymatiga bog‘lanib qolmaslik kerak. Har bir ish kuni uchun to‘lov summasi mustaqil saqlanadi.**

Masalan:

```text
Ali:

1-avgust   250 000
2-avgust   250 000
3-avgust   300 000
4-avgust   200 000
5-avgust   0       — ishlamagan
```

Shuning uchun `Worker.daily_rate` faqat **standart/tavsiya etiladigan stavka** bo‘lishi mumkin. Haqiqiy hisob-kitob `Attendance` yoki `WorkDay` yozuvidagi `payment_amount` orqali amalga oshiriladi.

---

# 2. Asosiy tushunchalar

Tizimning asosiy entitylari:

```text
User
 │
 └── Objects
       │
       ├── Workers
       │     ├── Work Days / Attendance
       │     ├── Daily Bonuses
       │     └── General Bonuses
       │
       ├── Expenses
       │     ├── Transport
       │     ├── Materials
       │     ├── Other
       │     └── ...
       │
       ├── Money Transactions
       │     ├── Income
       │     └── Payments
       │
       └── Reports
```

---

# 3. Foydalanuvchi

Ilovaga kirgan foydalanuvchi o‘z obyektlarini boshqaradi.

### User

Maydonlar:

```text
id
name
phone
email
password_hash
created_at
updated_at
```

Kelajakda:

```text
role
permissions
```

qo‘shilishi mumkin.

---

# 4. Obyekt

Obyekt — tizimning asosiy business entitysi.

Masalan:

```text
Chilonzor 12-uy remonti
```

### Object

```text
id
name
description
total_price
currency
start_date
end_date
status
created_at
updated_at
```

### Status

```text
PLANNED
ACTIVE
COMPLETED
CANCELLED
```

### Misol

```text
Obyekt:
Chilonzor 12-uy

Umumiy narxi:
150 000 000 so'm

Boshlangan:
01.08.2026

Status:
ACTIVE
```

---

# 5. Obyekt pulining hisoblanishi

Obyektning umumiy narxi va undan tushgan pulni bir-biridan ajratish kerak.

Masalan:

```text
Obyekt qiymati:
150 000 000

01.08
+50 000 000

10.08
+30 000 000

18.08
+20 000 000
```

Natija:

```text
Jami tushgan:
100 000 000

Olinishi kerak:
50 000 000
```

Shuning uchun `received_amount` ni Object ichida oddiy o‘zgaruvchi sifatida saqlashdan ko‘ra, **pul kirimlarini alohida transaction sifatida saqlash** to‘g‘riroq.

---

# 6. Money Transaction

### MoneyTransaction

```text
id
object_id
type
amount
date
description
created_at
```

### Type

```text
INCOME
```

Kelajakda:

```text
REFUND
ADJUSTMENT
```

kabi turlar qo‘shilishi mumkin.

Misol:

```text
18.08.2026
INCOME
20 000 000
"2-bosqich to'lovi"
```

Shunda:

```text
total_received =
SUM(INCOME transactions)
```

---

# 7. Ishchi

Har bir ishchi ma'lum obyektga biriktiriladi.

### Worker

```text
id
object_id
name
phone
position
default_rate
status
start_date
end_date
notes
created_at
updated_at
```

### `default_rate`

Bu **majburiy hisob-kitob qiymati emas**.

Masalan:

```text
Ali
Standart stavka:
250 000
```

Lekin konkret kun uchun:

```text
18-avgust:
300 000
```

bo‘lishi mumkin.

Shuning uchun real ish haqi `default_rate` dan emas, ish kuni yozuvidan olinadi.

---

# 8. Ishchini obyektga qo‘shish

Foydalanuvchi:

```text
+ Ishchi qo‘shish
```

bosadi.

Forma:

```text
Ism
Telefon
Lavozim
Standart kunlik stavka
Ish boshlagan sana
Izoh
```

Masalan:

```text
Ism:
Ali

Lavozim:
Usta

Standart stavka:
250 000

Boshlanish:
01.08.2026
```

---

# 9. Ishchining ish kuni

Bu tizimdagi eng muhim entitylardan biri.

`Attendance` nomidan ko‘ra `WorkDay` yoki `WorkerDay` modeli business logic uchun qulayroq, chunki unda nafaqat kelgan-kelmaganligi, balki **shu kun uchun real to‘lov** ham saqlanadi.

### WorkerDay

```text
id
worker_id
date
status
payment_amount
note
created_at
updated_at
```

### Status

```text
WORKED
ABSENT
HALF_DAY
SICK
LEAVE
OTHER
```

Lekin `payment_amount` statusdan mustaqil bo‘lishi kerak.

Masalan:

```text
Ali
18.08

status:
WORKED

payment:
300 000
```

---

# 10. Nega `payment_amount` alohida bo‘lishi kerak?

Quyidagi holatlar real hayotda bo‘lishi mumkin:

### 1-holat

```text
Ali
01.08 → 250 000
02.08 → 250 000
03.08 → 300 000
```

### 2-holat

Bir kunda yarim kun ishladi:

```text
04.08
HALF_DAY
150 000
```

### 3-holat

Oddiy stavkasi 250 000, lekin og‘ir ish qilgani uchun:

```text
05.08
WORKED
350 000
```

### 4-holat

Kelmagan:

```text
06.08
ABSENT
0
```

Shuning uchun:

```text
Worker.default_rate
```

faqat default qiymat.

Haqiqiy hisob:

```text
SUM(WorkerDay.payment_amount)
```

---

# 11. Kalendar UI

Ishchining sahifasida kalendar bo‘ladi.

Masalan:

```text
        AVGUST 2026

Du Se Ch Pay Ju Sh Ya
17 18 19 20 21 22 23
✓  ✓  -  ✓  ✓  -  -

24 25 26 27 28 29 30
✓  ✓  ✓  -  ✓  -  -
```

Har bir kunning o‘z qiymati bor.

Masalan:

```text
18-avgust
Ishlagan
300 000 so'm
```

```text
19-avgust
Ishlamagan
0 so'm
```

```text
20-avgust
Ishlagan
250 000 so'm
```

---

# 12. Bir kunni kiritish

Foydalanuvchi kalendardan sanani bosadi.

```text
18 AVGUST

Holat:
[ Ishladi ]

To'lov:
300 000 so'm

Izoh:
Qo'shimcha ish bo'ldi

[ SAQLASH ]
```

Agar ishlamagan bo‘lsa:

```text
Holat:
[ Ishlamadi ]

To'lov:
0

Sabab:
Dam olish
```

---

# 13. Bir nechta ishchi uchun davomat

Har bir ishchini alohida ochib kiritish majburiy bo‘lmasligi kerak.

Obyektning kunlik davomat ekranida:

```text
18 AVGUST

Ali
[✓] 300 000

Vali
[✓] 250 000

Sardor
[✓] 300 000

Jasur
[ ] 0

Bek
[✓] 220 000
```

Bu ekranda bir kunda barcha ishchilarni tez kiritish mumkin.

---

# 14. Kunlik bonus

Bonusning birinchi turi — **specific work day bonus**.

Masalan:

```text
18.08.2026

Ali
Ish haqi:
300 000

Bonus:
100 000

Jami:
400 000
```

### DailyBonus

```text
id
worker_id
worker_day_id
amount
date
reason
created_at
```

Bu bonus aynan ma'lum ish kuniga bog‘lanadi.

---

# 15. Umumiy bonus

Ikkinchi turi — ishchiga alohida bir kun bilan bog‘lanmagan bonus.

Masalan:

```text
Ali
Umumiy bonus:
1 000 000

Sabab:
Obyektni muddatidan oldin tugatgani uchun
```

### GeneralBonus

```text
id
worker_id
object_id
amount
date
reason
created_at
```

Bu `worker_day_id` ga bog‘lanmaydi.

---

# 16. Bonuslarning hisoblanishi

Ishchining jami daromadi:

```text
work_payments
+
daily_bonuses
+
general_bonuses
```

Masalan:

```text
Ish kunlari:
4 500 000

Kunlik bonuslar:
500 000

Umumiy bonus:
1 000 000

────────────────
Jami:
6 000 000
```

---

# 17. Bonusni ish haqiga qo‘shib yubormaslik

Database'da:

```text
payment_amount = 300 000
bonus = 100 000
```

alohida saqlanishi kerak.

Quyidagicha qilish noto‘g‘ri:

```text
payment_amount = 400 000
```

Chunki keyinchalik foydalanuvchi:

> Bu 400 000 ning qancha qismi ish haqi, qancha qismi bonus?

degan savolga javob topa olmaydi.

To‘g‘ri:

```text
Ish haqi       300 000
Kunlik bonus   100 000
──────────────────────
Jami           400 000
```

---

# 18. Xarajatlar

Barcha pul chiqimlari `Expense` orqali boshqariladi.

### Expense

```text
id
object_id
worker_id
worker_day_id
category
amount
date
description
created_at
updated_at
```

Lekin `worker_id` va `worker_day_id` har doim to‘ldirilmaydi.

---

# 19. Xarajat kategoriyalari

Boshlang‘ich kategoriyalar:

```text
WORKER_PAYMENT
DAILY_BONUS
GENERAL_BONUS
TRANSPORT
MATERIAL
FOOD
EQUIPMENT
OTHER
```

Lekin worker payment va bonuslarni ikki marta xarajatga hisoblab yubormaslik kerak.

Arxitektura nuqtayi nazaridan yaxshiroq variant:

```text
WorkerDay
DailyBonus
GeneralBonus
Expense
```

alohida entity bo‘ladi.

Hisobot engine esa ularning barchasini xarajat sifatida yig‘adi.

---

# 20. Yo‘l kira

Masalan:

```text
18.08.2026

Yo'l kira:
500 000

Izoh:
Ishchilarni obyektga olib kelish
```

Agar bitta ishchiga tegishli bo‘lsa:

```text
worker_id = Ali
```

Agar barcha ishchilarga tegishli bo‘lsa:

```text
worker_id = null
```

---

# 21. Boshqa xarajat

Masalan:

```text
Material:
1 500 000

18.08.2026
Sement va qum
```

yoki:

```text
OTHER

800 000

Asbob-uskunani ta'mirlash
```

---

# 22. Xarajatlarni kiritish

Xarajat qo‘shish:

```text
+ Xarajat

Kategoriya:
[ Yo'l kira ]

Summa:
500 000

Sana:
18.08.2026

Ishchi:
[ Barchasi ]

Izoh:
...
```

---

# 23. Obyektning umumiy xarajati

Formula:

```text
total_expense =
worker_payments
+ daily_bonuses
+ general_bonuses
+ other_expenses
```

Masalan:

```text
Ish haqi:
40 000 000

Kunlik bonus:
2 000 000

Umumiy bonus:
3 000 000

Yo'l kira:
5 000 000

Material:
8 000 000

Boshqa:
2 000 000

──────────────────
Jami:
60 000 000
```

---

# 24. Obyekt dashboardi

Asosiy ekran:

```text
CHILONZOR OBYEKTI

150 000 000
Obyekt qiymati

100 000 000
Tushgan pul

50 000 000
Kutilayotgan to'lov

────────────────────

60 000 000
Jami xarajat

40 000 000
Qo'ldagi pul

────────────────────

12
Ishchilar

183
Ishlangan ish kunlari
```

---

# 25. Moliyaviy formulalar

### Obyekt qiymati

```text
object_price
```

### Tushgan pul

```text
received_money =
SUM(MoneyTransaction.amount)
WHERE type = INCOME
```

### Kutilayotgan pul

```text
remaining_receivable =
object_price - received_money
```

### Ish haqi

```text
worker_salary =
SUM(WorkerDay.payment_amount)
```

### Kunlik bonus

```text
daily_bonus =
SUM(DailyBonus.amount)
```

### Umumiy bonus

```text
general_bonus =
SUM(GeneralBonus.amount)
```

### Boshqa xarajat

```text
other_expense =
SUM(Expense.amount)
```

### Jami xarajat

```text
total_expense =
worker_salary
+ daily_bonus
+ general_bonus
+ other_expense
```

### Qo‘ldagi pul

```text
cash_balance =
received_money - total_expense
```

### Obyekt foydasi

Agar obyektning to‘liq qiymati va barcha xarajatlari ma'lum bo‘lsa:

```text
estimated_profit =
object_price - total_expense
```

Lekin `cash_balance` bilan `estimated_profit` bir xil emas.

Masalan:

```text
Obyekt:
150 mln

Tushgan:
100 mln

Xarajat:
60 mln

Qo'ldagi:
40 mln

Kutilayotgan:
50 mln

Taxminiy foyda:
90 mln
```

---

# 26. Ishchi bo‘yicha umumiy statistika

Ishchini ochganda:

```text
ALI

Ishlangan kun:
18

Ish haqi:
4 800 000

Kunlik bonus:
500 000

Umumiy bonus:
1 000 000

Jami:
6 300 000
```

Pastida oylar bo‘yicha:

```text
AVGUST

01.08    250 000
02.08    250 000
03.08    300 000
04.08    0
05.08    350 000
...
```

---

# 27. Oylik hisobot

Masalan:

```text
AVGUST 2026

Ishchilar:
12

Ishlangan kunlar:
183

Ish haqi:
40 000 000

Bonus:
5 000 000

Yo'l kira:
4 000 000

Material:
8 000 000

Boshqa:
2 000 000

────────────────

Jami xarajat:
59 000 000
```

---

# 28. Kunlik hisobot

18-avgust:

```text
18 AVGUST

Ishchilar:
10

Bugungi ish haqi:
2 750 000

Bugungi bonus:
300 000

Yo'l kira:
500 000

Material:
0

────────────────

Bugungi xarajat:
3 550 000
```

Bu ayniqsa obyekt boshqaruvi uchun muhim.

---

# 29. Xarajatlar ekrani

```text
XARAJATLAR

Bugun
3 550 000

Bu oy
25 600 000

Umumiy
59 000 000
```

Pastida kategoriyalar:

```text
Ish haqi          40 000 000
Bonus              5 000 000
Yo'l kira          4 000 000
Material           8 000 000
Boshqa             2 000 000
```

---

# 30. Xarajat detalizatsiyasi

`Ish haqi` ustiga kirilganda:

```text
ISH HAQI

Ali
18 kun
4 800 000

Vali
20 kun
4 200 000

Sardor
19 kun
5 100 000
```

Ali'ga kirilganda:

```text
ALI

01.08
250 000

02.08
250 000

03.08
300 000

...

Jami:
4 800 000
```

---

# 31. Bonus detalizatsiyasi

```text
BONUSLAR

Ali
500 000
18.08

Vali
300 000
15.08

Ali
1 000 000
Umumiy bonus
```

Bonus turi ham ko‘rsatiladi:

```text
Kunlik
Umumiy
```

---

# 32. Pul kirimlari

Alohida `Kirimlar` sahifasi bo‘lishi kerak.

```text
KIRIMLAR

01.08
50 000 000

10.08
30 000 000

18.08
20 000 000

────────────────

Jami:
100 000 000
```

Yangi kirim:

```text
+ Kirim

Summa:
20 000 000

Sana:
18.08.2026

Izoh:
2-bosqich to'lovi
```

---

# 33. Muhim biznes case'lar

Tizim quyidagi holatlarni to‘g‘ri qo‘llab-quvvatlashi shart.

### Case 1 — kunlik narx o‘zgaradi

```text
Ali:

01.08 → 250k
02.08 → 250k
03.08 → 300k
04.08 → 350k
```

Har bir kun mustaqil.

---

### Case 2 — ishchi yarim kun ishlaydi

```text
HALF_DAY
150k
```

---

### Case 3 — ishchi ishladi, lekin pul keyin beriladi

Bu juda muhim.

`ishladi` va `pul berildi` bir xil narsa emas.

Masalan:

```text
18.08

Ali ishladi:
300 000

Lekin pul:
berilmadi
```

Shuning uchun kelajakda `WorkerPayment` entitysi kerak bo‘lishi mumkin.

---

# 34. Ish haqi qarzdorligi

Professionalroq variantda:

```text
WorkerDay

payment_amount = 300 000
payment_status = UNPAID
```

Keyin:

```text
20.08

Ali'ga:
300 000 berildi
```

va:

```text
payment_status = PAID
```

Bu tizimni ancha kuchli qiladi.

---

# 35. WorkerPayment

Agar haqiqiy pul berilishi ham nazorat qilinadigan bo‘lsa:

```text
id
worker_id
object_id
amount
date
type
description
```

Masalan:

```text
18.08
Ali
300 000
ISH HAQI
```

yoki:

```text
20.08
Ali
1 500 000
AVANS
```

Bu holda:

```text
Earned:
4 800 000

Paid:
3 000 000

Debt:
1 800 000
```

Bu juda foydali qo‘shimcha.

---

# 36. Avans

Real loyihada ishchiga oldindan pul berish holati bo‘ladi.

Masalan:

```text
Ali

Ishlagan:
4 800 000

Avans:
1 000 000

Qolgan:
3 800 000
```

Shuning uchun:

```text
WorkerPayment.type

SALARY
ADVANCE
BONUS
OTHER
```

kabi payment turlari kerak bo‘lishi mumkin.

---

# 37. Ma'lumotlarni o‘chirish

Moliyaviy ma'lumotlarni hard delete qilish tavsiya etilmaydi.

Masalan, 18-avgustdagi:

```text
300 000
```

xato bo‘lsa, uni database'dan butunlay o‘chirib yuborishdan ko‘ra:

```text
status = CANCELLED
```

yoki audit log orqali o‘zgarishni saqlash yaxshiroq.

Bu ayniqsa katta loyiha uchun muhim.

---

# 38. Audit log

Moliyaviy tizimda:

```text
Kim?
Nimani?
Qachon?
Eski qiymat?
Yangi qiymat?
```

saqlanishi foydali.

Masalan:

```text
18.08.2026 09:15

User:
Admin

Action:
WorkerDay updated

Ali:
250 000 → 300 000
```

Shunda hisob-kitoblar nima sababdan o‘zgarganini aniqlash mumkin.

---

# 39. Mobil ilova ekranlari

MVP uchun ekranlar:

```text
1. Login
2. Register
3. Dashboard
4. Obyektlar
5. Obyekt yaratish
6. Obyekt dashboard
7. Ishchilar
8. Ishchi qo‘shish
9. Ishchi profili
10. Kalendar
11. Kunlik davomat/to‘lov
12. Bonuslar
13. Bonus qo‘shish
14. Xarajatlar
15. Xarajat qo‘shish
16. Kirimlar
17. Kirim qo‘shish
18. Hisobot
19. Oylik hisobot
20. Sozlamalar
```

---

# 40. Obyekt dashboardining final ko‘rinishi

```text
┌──────────────────────────────┐
│ CHILONZOR REMONT             │
│                              │
│ 150 000 000                  │
│ Obyekt qiymati               │
│                              │
│ Tushgan       100 000 000    │
│ Olinadi        50 000 000    │
│                              │
│ Xarajat        60 000 000    │
│ Qoldiq         40 000 000    │
├──────────────────────────────┤
│                              │
│ Ishchilar             12     │
│ Ish kunlari           183    │
│                              │
│ Ish haqi       40 000 000    │
│ Bonus           5 000 000    │
│ Yo'l kira       4 000 000    │
│ Material        8 000 000    │
│ Boshqa          2 000 000    │
│                              │
├──────────────────────────────┤
│  Umumiy   Ishchilar          │
│  Xarajat  Kirimlar  Hisobot  │
└──────────────────────────────┘
```

---

# 41. Backend uchun tavsiya qilinadigan model

Minimal emas, kelajakdagi real case'larni ham ko‘taradigan struktura:

```text
User
Object
Worker

WorkerDay
DailyBonus
GeneralBonus

Expense
MoneyTransaction

WorkerPayment

AuditLog
```

Bog‘lanish:

```text
User
 │
 └── Object
      │
      ├── Worker
      │    │
      │    ├── WorkerDay
      │    │      └── DailyBonus
      │    │
      │    ├── GeneralBonus
      │    │
      │    └── WorkerPayment
      │
      ├── Expense
      │
      ├── MoneyTransaction
      │
      └── AuditLog
```

---

# 42. Eng muhim arxitektura qarori

Tizimni quyidagicha qurish kerak:

```text
Worker
    ↓
WorkerDay
    ↓
payment_amount
```

**`Worker.default_rate` orqali tarixiy ish haqini hisoblamaslik kerak.**

Masalan:

```text
Ali.default_rate = 250 000
```

keyin 3 oy o'tib:

```text
Ali.default_rate = 350 000
```

bo‘lsa, oldingi oylarning hisob-kitobi o‘zgarmasligi kerak.

Shuning uchun:

```text
01.08 WorkerDay.payment_amount = 250 000
02.08 WorkerDay.payment_amount = 250 000
03.08 WorkerDay.payment_amount = 300 000
```

mustaqil saqlanadi.

Xuddi shu prinsip bonus va xarajatlarga ham qo‘llanadi.

---

# 43. Yakuniy business flow

Tizimning to‘liq ishlash zanjiri:

```text
OBYEKT YARATILADI
        ↓
OBYEKT NARXI KIRITILADI
        ↓
ISHCHILAR QO‘SHILADI
        ↓
HAR BIR ISHCHINING DEFAULT STAVKASI BERILADI
        ↓
HAR KUNI DAVOMAT KIRITILADI
        ↓
HAR BIR KUN UCHUN REAL TO‘LOV BELGILANADI
        ↓
KUNLIK BONUSLAR KIRITILADI
        ↓
UMUMIY BONUSLAR KIRITILADI
        ↓
YO‘L KIRA / MATERIAL / BOSHQA XARAJATLAR KIRITILADI
        ↓
OBYEKTGA TUSHGAN PULLAR KIRITILADI
        ↓
             HISOB-KITOB ENGINE
                    ↓
        ┌───────────┼───────────┐
        ↓           ↓           ↓
    Ish haqi      Bonuslar    Boshqa xarajat
        └───────────┼───────────┘
                    ↓
              JAMI XARAJAT
                    ↓
          TUSHGAN PUL - XARAJAT
                    ↓
             QO‘LDAGI PUL
```

Natijada tizimning asosiy maqsadi **“ishchilarni ro‘yxatga olish” emas**, balki **obyektning real vaqtga yaqin moliyaviy accounting tizimini yaratish** bo‘ladi. Ishchi davomatlari shu accounting tizimiga kiruvchi asosiy manbalardan biri hisoblanadi.
