package uz.buildflow.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import uz.buildflow.app.core.util.DeviceSecurityManager

class DeviceSecurityTest {

    @Test
    fun testActivationKeyCalculation() {
        val deviceId1 = "BF-8A42-99F1"
        val key1 = DeviceSecurityManager.calculateActivationKey(deviceId1)

        // 1. Format: XXXX-XXXX (9 belgidan iborat, o'rtada tire)
        assertEquals(9, key1.length)
        assertEquals('-', key1[4])

        // 2. Bir xil ID uchun har doim bir xil kalit chiqishi (Deterministik)
        val key1Again = DeviceSecurityManager.calculateActivationKey(deviceId1)
        assertEquals(key1, key1Again)

        // 3. Boshqa qurilma ID si uchun butunlay boshqa kalit chiqishi
        val deviceId2 = "BF-1234-5678"
        val key2 = DeviceSecurityManager.calculateActivationKey(deviceId2)
        assertNotEquals(key1, key2)

        // 4. Katta/kichik harflarga sezgir bo'lmasligi (Case-insensitive)
        val key1Lower = DeviceSecurityManager.calculateActivationKey("bf-8a42-99f1")
        assertEquals(key1, key1Lower)
    }
}
