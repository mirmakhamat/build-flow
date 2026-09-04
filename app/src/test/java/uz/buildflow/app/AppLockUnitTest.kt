package uz.buildflow.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.buildflow.app.core.util.DeviceSecurityManager

class AppLockUnitTest {

    @Test
    fun testAppLockLogic() {
        val deviceId = "BF-TEST-0001"
        val key = DeviceSecurityManager.calculateActivationKey(deviceId)
        assertTrue(key.isNotEmpty())
        assertTrue(key.contains("-"))
    }
}
