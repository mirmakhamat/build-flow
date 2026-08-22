### UI yo‘nalishi

**Minimalist offline-first financial/work management UI.**

Asosiy vizual konsepsiya:

* **Light theme**, oq yoki juda och kulrang background.
* Asosiy rang sifatida **deep blue / indigo**.
* Muhim moliyaviy raqamlar katta va aniq ko‘rsatiladi.
* Card-based dashboard.
* Pastki navigation:
  **Obyektlar · Ishchilar · Xarajatlar · Hisobot**
* Obyekt ichida yuqorida:
  **Obyekt narxi → Tushgan pul → Xarajat → Qoldiq**
* Ishchilar uchun **calendar-first UI**: har bir kun rang/status bilan belgilanadi va bosilganda o‘sha kunning to‘lovi, bonus va izohi ochiladi.
* `+` tugmasi orqali tezkor kiritish:
  **Davomat · To‘lov · Bonus · Xarajat · Kirim**
* Ko‘p sonli ma'lumotlarni jadvalga o‘xshatib emas, mobil uchun **compact cards + bottom sheet** ko‘rinishida berish.
* Xarajatlar kategoriyalari uchun ikonka va qisqa summa.
* Hisobotlarda grafiklardan ko‘ra avval **raqamlar va breakdown**, keyin grafiklar.
* Barcha pul qiymatlari `250 000 so'm` formatida.
* UI foydalanuvchini buxgalteriya terminlari bilan ortiqcha yuklamaydi: **Tushgan pul, Xarajat, Qoldiq, Ish haqi, Bonus, Yo‘l kira** kabi oddiy terminlar ishlatiladi.

### Bosh ekran konsepsiyasi

```text
BuildFlow

Obyektlar
────────────────────

Chilonzor remont
150 000 000 so'm
Xarajat    60 000 000
Qoldiq     40 000 000

Yunusobod uy
85 000 000 so'm
Xarajat    32 500 000
Qoldiq     27 500 000

                    +
```

Umumiy uslub: **modern fintech + construction management**. Ya'ni Excel/buxgalteriya ko‘rinishidan qochib, moliyaviy ma'lumotlarni fintech ilovalaridagi kabi aniq, zich va vizual ko‘rsatish.
