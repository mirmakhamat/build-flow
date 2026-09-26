package uz.buildflow.app.core.database

import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Bir martalik tuzatish (4 -> 5): eski "Ko'chirish" funksiyasi faqat workers.object_id ni
 * o'zgartirgan, worker_days da esa obyekt saqlanmagan. Shu sabab ko'chirilgan ishchining
 * eski obyektdagi kunlari yangi obyektga tegishli bo'lib qolgan.
 *
 * Yangi ishchi yaratilmaydi - o'sha ishchi qoladi, faqat har bir yozuv o'z obyektiga qaytadi:
 *  - kunlar: eski obyektdagi oxirgi faollik (to'lov/bonus) vaqtigacha, oxirgi eski obyekt
 *    uchun esa ko'chirish vaqtigacha (workers.updated_at) yaratilganlari eski obyektga;
 *  - ko'chirishdan keyin joriy obyektda yozilgan, lekin eski obyekt kassasidan qilingan
 *    to'lovlar eski obyekt qarzini yopgan - ular eski obyektga o'tadi;
 *  - joriy obyektda yozilgan, sanasi eski obyektdagi kunga to'g'ri keladigan to'lovlar
 *    ham eski obyektga o'tadi, pul chiqqan kassa payer_object_id da saqlanadi.
 * worker_days.object_id ustuni mavjud bo'lishi kerak. Qolgan NULL kunlarni chaqiruvchi
 * ishchining joriy obyekti bilan to'ldiradi.
 */
object LegacyTransferRepair {

    private val statements = listOf(
        """DROP TABLE IF EXISTS temp.lt_worker""",
        """CREATE TEMP TABLE lt_worker AS SELECT w.id AS worker_id, w.object_id AS cur_object, w.updated_at AS moved_at FROM workers w WHERE EXISTS (SELECT 1 FROM worker_payments p WHERE p.worker_id = w.id AND p.object_id != w.object_id) OR EXISTS (SELECT 1 FROM general_bonuses g WHERE g.worker_id = w.id AND g.object_id != w.object_id)""",
        """DROP TABLE IF EXISTS temp.lt_seg""",
        """CREATE TEMP TABLE lt_seg AS SELECT x.worker_id AS worker_id, x.object_id AS old_object, MAX(x.created_at) AS last_act, MAX(x.created_at) AS boundary FROM (SELECT worker_id, object_id, created_at FROM worker_payments UNION ALL SELECT worker_id, object_id, created_at FROM general_bonuses) x JOIN lt_worker m ON m.worker_id = x.worker_id WHERE x.object_id != m.cur_object GROUP BY x.worker_id, x.object_id""",
        """UPDATE lt_seg SET boundary = MAX(boundary, (SELECT m.moved_at FROM lt_worker m WHERE m.worker_id = lt_seg.worker_id)) WHERE last_act = (SELECT MAX(s2.last_act) FROM lt_seg s2 WHERE s2.worker_id = lt_seg.worker_id)""",
        """UPDATE worker_days SET object_id = (SELECT s.old_object FROM lt_seg s WHERE s.worker_id = worker_days.worker_id AND worker_days.created_at <= s.boundary ORDER BY s.boundary LIMIT 1) WHERE object_id IS NULL AND EXISTS (SELECT 1 FROM lt_seg s WHERE s.worker_id = worker_days.worker_id AND worker_days.created_at <= s.boundary)""",
        """UPDATE worker_payments SET object_id = payer_object_id, payer_object_id = NULL WHERE EXISTS (SELECT 1 FROM lt_worker m JOIN lt_seg s ON s.worker_id = m.worker_id WHERE m.worker_id = worker_payments.worker_id AND worker_payments.object_id = m.cur_object AND s.old_object = worker_payments.payer_object_id)""",
        """UPDATE worker_payments SET payer_object_id = COALESCE(payer_object_id, object_id), object_id = (SELECT d.object_id FROM worker_days d WHERE d.worker_id = worker_payments.worker_id AND d.date = worker_payments.date) WHERE type = 'SALARY' AND EXISTS (SELECT 1 FROM lt_worker m WHERE m.worker_id = worker_payments.worker_id AND worker_payments.object_id = m.cur_object) AND EXISTS (SELECT 1 FROM worker_days d JOIN lt_seg s ON s.worker_id = d.worker_id AND s.old_object = d.object_id WHERE d.worker_id = worker_payments.worker_id AND d.date = worker_payments.date)""",
        """UPDATE worker_payments SET payer_object_id = NULL WHERE payer_object_id = object_id""",
        """DROP TABLE IF EXISTS temp.lt_seg""",
        """DROP TABLE IF EXISTS temp.lt_worker"""
    )

    fun run(db: SupportSQLiteDatabase) {
        statements.forEach { db.execSQL(it) }
    }
}
