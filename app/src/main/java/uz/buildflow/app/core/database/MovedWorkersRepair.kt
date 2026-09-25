package uz.buildflow.app.core.database

import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Bir martalik tuzatish: eski "Ko'chirish" funksiyasi faqat workers.object_id ni o'zgartirgan.
 * Kunlar (worker_days) ishchi bilan yangi obyektga o'tib ketgan, to'lovlar va umumiy bonuslar
 * esa eski obyektda qolgan - yangi obyektda to'langan kunlar qarz bo'lib ko'ringan.
 *
 * Har bir shunday ishchi uchun eski obyektda alohida (INACTIVE) ishchi yozuvi yaratiladi
 * (Import qiladigan modelning o'zi) va eski obyekt davridagi ma'lumotlar unga qaytariladi:
 *  - kunlar: ko'chirish vaqtigacha (workers.updated_at) yaratilganlari;
 *  - to'lovlar va umumiy bonuslar: eski obyektga yozilganlari;
 *  - ko'chirishdan keyin yangi obyektdan eski kun/bonus uchun qilingan to'lovlar eski
 *    obyektga o'tadi, pul chiqqan kassa payer_object_id da saqlanadi.
 * Zarar ko'rmagan bazada hech narsa o'zgarmaydi; qayta ishga tushirish xavfsiz.
 */
object MovedWorkersRepair {

    private val statements = listOf(
        """DROP TABLE IF EXISTS temp.mv_worker""",
        """CREATE TEMP TABLE mv_worker AS SELECT w.id AS worker_id, w.object_id AS cur_object, w.updated_at AS moved_at FROM workers w WHERE EXISTS (SELECT 1 FROM worker_payments p WHERE p.worker_id = w.id AND p.object_id != w.object_id) OR EXISTS (SELECT 1 FROM general_bonuses g WHERE g.worker_id = w.id AND g.object_id != w.object_id)""",
        """DROP TABLE IF EXISTS temp.mv_seg""",
        """CREATE TEMP TABLE mv_seg AS SELECT x.worker_id AS worker_id, x.object_id AS old_object, MAX(x.created_at) AS last_act, MAX(x.created_at) AS boundary, lower(hex(randomblob(16))) AS new_worker_id FROM (SELECT worker_id, object_id, created_at FROM worker_payments UNION ALL SELECT worker_id, object_id, created_at FROM general_bonuses) x JOIN mv_worker m ON m.worker_id = x.worker_id WHERE x.object_id != m.cur_object GROUP BY x.worker_id, x.object_id""",
        """UPDATE mv_seg SET boundary = MAX(boundary, (SELECT m.moved_at FROM mv_worker m WHERE m.worker_id = mv_seg.worker_id)) WHERE last_act = (SELECT MAX(s2.last_act) FROM mv_seg s2 WHERE s2.worker_id = mv_seg.worker_id)""",
        """INSERT INTO workers (id, object_id, name, phone, position, default_rate, status, start_date, end_date, notes, created_at, updated_at) SELECT s.new_worker_id, s.old_object, w.name, w.phone, w.position, w.default_rate, 'INACTIVE', w.start_date, NULL, w.notes, w.created_at, w.updated_at FROM mv_seg s JOIN workers w ON w.id = s.worker_id""",
        """DROP TABLE IF EXISTS temp.mv_day""",
        """CREATE TEMP TABLE mv_day AS SELECT d.id AS day_id, d.date AS date, d.worker_id AS worker_id, (SELECT s.new_worker_id FROM mv_seg s WHERE s.worker_id = d.worker_id AND d.created_at <= s.boundary ORDER BY s.boundary LIMIT 1) AS new_worker_id FROM worker_days d WHERE d.worker_id IN (SELECT worker_id FROM mv_worker)""",
        """DELETE FROM mv_day WHERE new_worker_id IS NULL""",
        """UPDATE worker_days SET worker_id = (SELECT new_worker_id FROM mv_day WHERE day_id = worker_days.id) WHERE id IN (SELECT day_id FROM mv_day)""",
        """UPDATE daily_bonuses SET worker_id = (SELECT worker_id FROM worker_days d WHERE d.id = daily_bonuses.worker_day_id) WHERE worker_day_id IN (SELECT day_id FROM mv_day)""",
        """UPDATE worker_payments SET payer_object_id = COALESCE(payer_object_id, object_id), object_id = (SELECT s.old_object FROM mv_seg s JOIN mv_day md ON md.new_worker_id = s.new_worker_id WHERE md.worker_id = worker_payments.worker_id AND md.date = worker_payments.date), worker_id = (SELECT md.new_worker_id FROM mv_day md WHERE md.worker_id = worker_payments.worker_id AND md.date = worker_payments.date) WHERE type = 'SALARY' AND worker_id IN (SELECT worker_id FROM mv_worker) AND object_id = (SELECT m.cur_object FROM mv_worker m WHERE m.worker_id = worker_payments.worker_id) AND EXISTS (SELECT 1 FROM mv_day md WHERE md.worker_id = worker_payments.worker_id AND md.date = worker_payments.date)""",
        """UPDATE worker_payments SET worker_id = (SELECT s.new_worker_id FROM mv_seg s WHERE s.worker_id = worker_payments.worker_id AND s.old_object = worker_payments.object_id) WHERE EXISTS (SELECT 1 FROM mv_seg s WHERE s.worker_id = worker_payments.worker_id AND s.old_object = worker_payments.object_id)""",
        """UPDATE general_bonuses SET worker_id = (SELECT s.new_worker_id FROM mv_seg s WHERE s.worker_id = general_bonuses.worker_id AND s.old_object = general_bonuses.object_id) WHERE EXISTS (SELECT 1 FROM mv_seg s WHERE s.worker_id = general_bonuses.worker_id AND s.old_object = general_bonuses.object_id)""",
        """UPDATE worker_payments SET payer_object_id = COALESCE(payer_object_id, object_id), object_id = (SELECT g.object_id FROM general_bonuses g JOIN mv_seg s ON s.new_worker_id = g.worker_id WHERE s.worker_id = worker_payments.worker_id AND g.date = worker_payments.date LIMIT 1), worker_id = (SELECT g.worker_id FROM general_bonuses g JOIN mv_seg s ON s.new_worker_id = g.worker_id WHERE s.worker_id = worker_payments.worker_id AND g.date = worker_payments.date LIMIT 1) WHERE type = 'BONUS_PAYOUT' AND worker_id IN (SELECT worker_id FROM mv_worker) AND EXISTS (SELECT 1 FROM general_bonuses g JOIN mv_seg s ON s.new_worker_id = g.worker_id WHERE s.worker_id = worker_payments.worker_id AND g.date = worker_payments.date) AND NOT EXISTS (SELECT 1 FROM general_bonuses g2 WHERE g2.worker_id = worker_payments.worker_id AND g2.date = worker_payments.date)""",
        """UPDATE workers SET start_date = COALESCE((SELECT MIN(date) FROM worker_days d WHERE d.worker_id = workers.id), start_date), end_date = (SELECT MAX(date) FROM worker_days d WHERE d.worker_id = workers.id) WHERE id IN (SELECT new_worker_id FROM mv_seg)""",
        """UPDATE workers SET start_date = COALESCE((SELECT MIN(date) FROM worker_days d WHERE d.worker_id = workers.id), start_date) WHERE id IN (SELECT worker_id FROM mv_worker)""",
        """DROP TABLE IF EXISTS temp.mv_day""",
        """DROP TABLE IF EXISTS temp.mv_seg""",
        """DROP TABLE IF EXISTS temp.mv_worker"""
    )

    fun run(db: SupportSQLiteDatabase) {
        statements.forEach { db.execSQL(it) }
    }
}
