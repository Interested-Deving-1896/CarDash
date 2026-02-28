package com.fuseforge.cardash.ui.diagnostics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.fuseforge.cardash.data.db.DiagnosticDao
import com.fuseforge.cardash.services.obd.OBDService

@Suppress("UNCHECKED_CAST")
class DiagnosticsViewModelFactory(
    private val obdService: OBDService,
    private val diagnosticDao: DiagnosticDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DiagnosticsViewModel::class.java)) {
            return DiagnosticsViewModel(obdService, diagnosticDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
