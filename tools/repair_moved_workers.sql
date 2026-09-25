-- =====================================================================
-- BuildFlow: boshqa obyektga "ko'chirilgan" ishchilar bazasini tiklash
-- =====================================================================
--
-- Muammo: ishchini tahrirlashda obyekt almashtirilganda faqat
-- workers.object_id o'zgargan. worker_days va daily_bonuses obyektga
-- workers orqali bog'langani uchun barcha eski kunlar yangi obyektga
-- o'tib ketgan, worker_payments.object_id va general_bonuses.object_id
-- esa eski obyektda qolgan. Natijada:
--   * yangi obyekt: eski kunlar ish haqi sifatida qo'shilgan, to'lovlari yo'q -> qarz bo'lib ko'rinadi;
--   * eski obyekt: to'lovlar bor, ish haqi yo'q.
--
-- Tuzatish: har bir shunday ishchi uchun ESKI obyektda alohida ishchi
-- yozuvi yaratiladi (Import funksiyasi yaratadigan modelning aynan o'zi)
-- va eski obyektga tegishli kunlar, bonuslar, to'lovlar unga o'tkaziladi.
-- Asl ishchi yozuvi yangi obyektda qoladi.
--
-- Ishlatish (ilova ichidagi "Eksport"dan olingan .db nusxasida):
--   sqlite3 buildflow_backup_XXXX.db
--   1-QADAM so'rovlarini bajarib, natijani ko'ring.
--   2-QADAMda repair_plan jadvalini to'ldiring.
--   3-QADAMni bajaring, 4-QADAM bilan tekshiring, keyin COMMIT.
--   Faylni ilovaning "Import" funksiyasi orqali qaytaring.
-- =====================================================================

PRAGMA foreign_keys = ON;

-- ---------------------------------------------------------------------
-- 1-QADAM. DIAGNOSTIKA: qaysi ishchilar zarar ko'rgan
-- ---------------------------------------------------------------------
-- To'lovi yoki umumiy bonusi joriy obyektidan boshqa obyektda turgan ishchilar.
SELECT w.id                     AS worker_id,
       w.name,
       cur.name                 AS hozirgi_obyekt,
       x.object_id              AS eski_obyekt_id,
       old.name                 AS eski_obyekt,
       x.kind,
       COUNT(*)                 AS soni,
       SUM(x.amount)            AS summa,
       MIN(x.date)              AS birinchi_sana,
       MAX(x.date)              AS oxirgi_sana
FROM (
    SELECT worker_id, object_id, amount, date, 'payment' AS kind FROM worker_payments
    UNION ALL
    SELECT worker_id, object_id, amount, date, 'general_bonus' FROM general_bonuses
) x
JOIN workers w  ON w.id = x.worker_id
JOIN objects cur ON cur.id = w.object_id
LEFT JOIN objects old ON old.id = x.object_id
WHERE x.object_id != w.object_id
GROUP BY w.id, x.object_id, x.kind
ORDER BY w.name;

-- Har bir zarar ko'rgan ishchining kunlari: qaysi sanagacha eski obyektda
-- ishlaganini aniqlash uchun (to'lov qaysi obyektga yozilganini ham ko'rsatadi).
SELECT w.name, wd.date, wd.status, wd.payment_amount, wd.payment_status,
       (SELECT group_concat(DISTINCT o.name) FROM worker_payments p
          JOIN objects o ON o.id = p.object_id
         WHERE p.worker_id = wd.worker_id AND p.date = wd.date) AS tolov_obyekti
FROM worker_days wd
JOIN workers w ON w.id = wd.worker_id
WHERE wd.worker_id IN (
    SELECT p.worker_id FROM worker_payments p JOIN workers w2 ON w2.id = p.worker_id WHERE p.object_id != w2.object_id
    UNION
    SELECT g.worker_id FROM general_bonuses g JOIN workers w2 ON w2.id = g.worker_id WHERE g.object_id != w2.object_id
)
ORDER BY w.name, wd.date;

-- ---------------------------------------------------------------------
-- 2-QADAM. REJA: har bir ishchi uchun bitta qator
-- ---------------------------------------------------------------------
--   worker_id     - 1-qadamdagi worker_id
--   old_object_id - 1-qadamdagi eski_obyekt_id
--   cutoff_date   - eski obyektda ishlagan OXIRGI kun (YYYY-MM-DD, shu kun ham eski obyektga ketadi)
BEGIN TRANSACTION;

DROP TABLE IF EXISTS temp.repair_plan;
CREATE TEMP TABLE repair_plan (
    worker_id     TEXT PRIMARY KEY,
    old_object_id TEXT NOT NULL,
    cutoff_date   TEXT NOT NULL,
    new_worker_id TEXT
);

-- MISOL (o'zingiznikiga almashtiring):
-- INSERT INTO repair_plan (worker_id, old_object_id, cutoff_date) VALUES
--     ('<worker_id>', '<eski_obyekt_id>', '2026-09-10'),
--     ('<worker_id>', '<eski_obyekt_id>', '2026-09-12');

UPDATE repair_plan SET new_worker_id = lower(hex(randomblob(16)));

-- ---------------------------------------------------------------------
-- 3-QADAM. TUZATISH
-- ---------------------------------------------------------------------
-- 3.1 Eski obyektda ishchining alohida (nofaol) nusxasi
INSERT INTO workers (id, object_id, name, phone, position, default_rate, status,
                     start_date, end_date, notes, created_at, updated_at)
SELECT rp.new_worker_id, rp.old_object_id, w.name, w.phone, w.position, w.default_rate, 'INACTIVE',
       w.start_date, rp.cutoff_date, w.notes, w.created_at, CAST(strftime('%s','now') AS INTEGER) * 1000
FROM repair_plan rp JOIN workers w ON w.id = rp.worker_id;

-- 3.2 Eski obyektdagi kunlar (cutoff_date gacha, shu kun ham)
UPDATE worker_days
SET worker_id = (SELECT new_worker_id FROM repair_plan rp WHERE rp.worker_id = worker_days.worker_id)
WHERE worker_id IN (SELECT worker_id FROM repair_plan)
  AND date <= (SELECT cutoff_date FROM repair_plan rp WHERE rp.worker_id = worker_days.worker_id);

-- 3.3 Shu kunlarga bog'langan kunlik bonuslar
UPDATE daily_bonuses
SET worker_id = (SELECT worker_id FROM worker_days wd WHERE wd.id = daily_bonuses.worker_day_id)
WHERE worker_day_id IN (
    SELECT wd.id FROM worker_days wd JOIN repair_plan rp ON rp.new_worker_id = wd.worker_id
);

-- 3.4 To'lovlar: eski obyektga yozilganlari + eski davrdagi kunlar uchun
--     ko'chirishdan keyin yangi obyekt kassasidan to'langanlari.
--     Ikkinchisida pul yangi obyekt kassasidan chiqqani payer_object_id orqali saqlanadi.
UPDATE worker_payments
SET payer_object_id = CASE
        WHEN object_id != (SELECT old_object_id FROM repair_plan rp WHERE rp.worker_id = worker_payments.worker_id)
        THEN COALESCE(payer_object_id, object_id)
        ELSE payer_object_id
    END,
    object_id = (SELECT old_object_id FROM repair_plan rp WHERE rp.worker_id = worker_payments.worker_id),
    worker_id = (SELECT new_worker_id FROM repair_plan rp WHERE rp.worker_id = worker_payments.worker_id)
WHERE worker_id IN (SELECT worker_id FROM repair_plan)
  AND (   object_id = (SELECT old_object_id FROM repair_plan rp WHERE rp.worker_id = worker_payments.worker_id)
       OR date     <= (SELECT cutoff_date   FROM repair_plan rp WHERE rp.worker_id = worker_payments.worker_id));

-- 3.5 Umumiy bonuslar: xuddi shu qoida
UPDATE general_bonuses
SET object_id = (SELECT old_object_id FROM repair_plan rp WHERE rp.worker_id = general_bonuses.worker_id),
    worker_id = (SELECT new_worker_id FROM repair_plan rp WHERE rp.worker_id = general_bonuses.worker_id)
WHERE worker_id IN (SELECT worker_id FROM repair_plan)
  AND (   object_id = (SELECT old_object_id FROM repair_plan rp WHERE rp.worker_id = general_bonuses.worker_id)
       OR date     <= (SELECT cutoff_date   FROM repair_plan rp WHERE rp.worker_id = general_bonuses.worker_id));

-- 3.6 Asl ishchining boshlanish sanasi yangi obyektdagi birinchi kunga
UPDATE workers
SET start_date = COALESCE((SELECT MIN(date) FROM worker_days wd WHERE wd.worker_id = workers.id), start_date),
    updated_at = CAST(strftime('%s','now') AS INTEGER) * 1000
WHERE id IN (SELECT worker_id FROM repair_plan);

-- ---------------------------------------------------------------------
-- 4-QADAM. TEKSHIRUV (bo'sh natija qaytishi kerak)
-- ---------------------------------------------------------------------
SELECT 'payment object mismatch' AS muammo, p.id FROM worker_payments p
JOIN workers w ON w.id = p.worker_id WHERE p.object_id != w.object_id
UNION ALL
SELECT 'general_bonus object mismatch', g.id FROM general_bonuses g
JOIN workers w ON w.id = g.worker_id WHERE g.object_id != w.object_id
UNION ALL
SELECT 'daily_bonus worker mismatch', b.id FROM daily_bonuses b
JOIN worker_days wd ON wd.id = b.worker_day_id WHERE b.worker_id != wd.worker_id;

PRAGMA foreign_key_check;

-- Obyektlar bo'yicha ishchi qarzi (ilovadagi formulaning o'zi)
SELECT o.name,
       (SELECT COALESCE(SUM(wd.payment_amount),0) FROM worker_days wd JOIN workers w ON w.id = wd.worker_id WHERE w.object_id = o.id)
     + (SELECT COALESCE(SUM(b.amount),0) FROM daily_bonuses b JOIN workers w ON w.id = b.worker_id WHERE w.object_id = o.id)
     + (SELECT COALESCE(SUM(amount),0) FROM general_bonuses WHERE object_id = o.id)   AS hisoblangan,
       (SELECT COALESCE(SUM(amount),0) FROM worker_payments WHERE object_id = o.id)  AS tolangan
FROM objects o;

-- Hammasi to'g'ri bo'lsa:
COMMIT;
-- Aks holda: ROLLBACK;
