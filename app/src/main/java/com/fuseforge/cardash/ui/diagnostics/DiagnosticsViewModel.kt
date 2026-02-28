package com.fuseforge.cardash.ui.diagnostics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fuseforge.cardash.data.db.DiagnosticCode
import com.fuseforge.cardash.data.db.DiagnosticDao
import com.fuseforge.cardash.services.obd.ConnectionStatus
import com.fuseforge.cardash.services.obd.OBDService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class DiagnosticsViewModel(
    private val obdService: OBDService,
    private val diagnosticDao: DiagnosticDao
) : ViewModel() {

    private val _scannedCodes = MutableStateFlow<List<DiagnosticCode>>(emptyList())
    val scannedCodes = _scannedCodes.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning = _isScanning.asStateFlow()

    val connectionStatus = obdService.connectionStatus

    fun scan() {
        if (_isScanning.value || connectionStatus.value != ConnectionStatus.CONNECTED) return

        viewModelScope.launch {
            _isScanning.value = true
            _scannedCodes.value = obdService.scanTroubleCodes(diagnosticDao)
            _isScanning.value = false
        }
    }

    fun clear() {
        _scannedCodes.value = emptyList()
    }
}
