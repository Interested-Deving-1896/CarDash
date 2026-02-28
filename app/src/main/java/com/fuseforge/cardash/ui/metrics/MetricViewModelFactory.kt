package com.fuseforge.cardash.ui.metrics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.fuseforge.cardash.services.obd.OBDService
import com.fuseforge.cardash.services.obd.OBDServiceWithDiagnostics
import com.fuseforge.cardash.services.obd.PollingEngine

class MetricViewModelFactory(
    private val obdService: OBDService,
    private val obdServiceWithDiagnostics: OBDServiceWithDiagnostics,
    private val pollingEngine: PollingEngine
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MetricViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MetricViewModel(obdService, obdServiceWithDiagnostics, pollingEngine) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
