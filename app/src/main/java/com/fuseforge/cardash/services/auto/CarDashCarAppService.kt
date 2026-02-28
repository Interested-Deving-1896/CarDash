package com.fuseforge.cardash.services.auto

import android.content.Intent
import android.content.pm.ApplicationInfo // Added for FLAG_DEBUGGABLE
import androidx.car.app.CarAppService
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator
import androidx.lifecycle.DefaultLifecycleObserver
import com.fuseforge.cardash.R // Assuming R class is in com.fuseforge.cardash
import android.util.Log // Import Log

class CarDashCarAppService : CarAppService() {
    override fun createHostValidator(): HostValidator {
        return if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
        } else {
            val builder = HostValidator.Builder(applicationContext)
            try {
                builder.addAllowedHosts(R.array.hosts_allowlist_sample)
            } catch (e: Exception) {
                // Ignore
            }
            builder.build()
        }
    }

    override fun onCreateSession(): Session {
        return CarDashSession()
    }
}
 