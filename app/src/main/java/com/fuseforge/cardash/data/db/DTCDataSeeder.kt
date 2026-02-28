package com.fuseforge.cardash.data.db

import android.util.Log

object DTCDataSeeder {
    
    suspend fun seedIfEmpty(dao: DiagnosticDao) {
        if (dao.getCount() > 0) return
        
        Log.d("DTCDataSeeder", "Seeding DTC database with common codes...")
        
        val commonCodes = listOf(
            DiagnosticCode("P0101", "Mass Air Flow (MAF) Circuit Operating Range or Performance Problem", "Dirty sensor, vacuum leaks, air intake restriction", 2),
            DiagnosticCode("P0113", "Intake Air Temperature Sensor 1 Circuit High Input", "Faulty IAT sensor, wiring/connector issues", 2),
            DiagnosticCode("P0128", "Coolant Thermostat (Coolant Temperature Below Thermostat Regulating Temperature)", "Stuck open thermostat, faulty coolant sensor", 2),
            DiagnosticCode("P0171", "System Too Lean (Bank 1)", "Vacuum leaks, clogged fuel injectors, faulty MAF sensor", 3),
            DiagnosticCode("P0174", "System Too Lean (Bank 2)", "Vacuum leaks, fuel pump issues, MAF sensor contamination", 3),
            DiagnosticCode("P0300", "Random or Multiple Cylinder Misfire Detected", "Worn spark plugs, faulty ignition coils, low fuel pressure", 3),
            DiagnosticCode("P0301", "Cylinder 1 Misfire Detected", "Spark plug, ignition coil, or fuel injector in cylinder 1", 3),
            DiagnosticCode("P0302", "Cylinder 2 Misfire Detected", "Spark plug, ignition coil, or fuel injector in cylinder 2", 3),
            DiagnosticCode("P0303", "Cylinder 3 Misfire Detected", "Spark plug, ignition coil, or fuel injector in cylinder 3", 3),
            DiagnosticCode("P0304", "Cylinder 4 Misfire Detected", "Spark plug, ignition coil, or fuel injector in cylinder 4", 3),
            DiagnosticCode("P0401", "Exhaust Gas Recirculation (EGR) Flow Insufficient Detected", "Clogged EGR valve or carbon buildup in passages", 2),
            DiagnosticCode("P0420", "Catalyst System Efficiency Below Threshold (Bank 1)", "Faulty catalytic converter, exhaust leaks, O2 sensor issues", 2),
            DiagnosticCode("P0442", "Evaporative Emission System Leak Detected (Small Leak)", "Loose gas cap, damaged charcoal canister", 2),
            DiagnosticCode("P0455", "Evaporative Emission System Leak Detected (Large Leak)", "Faulty gas cap, large charcoal canister leak", 3),
            DiagnosticCode("P0500", "Vehicle Speed Sensor (VSS) Malfunction", "Faulty VSS, wiring issues, instrument cluster fault", 3),
            DiagnosticCode("P0606", "PCM / ECM Processor Fault", "Internal control module hardware failure", 3),
            DiagnosticCode("P0700", "Transmission Control System Malfunction (MIL Request)", "Underlying transmission problem caught by TCM", 3),
            DiagnosticCode("P0AA6", "Hybrid Battery Voltage System Isolation Fault", "Insulation leakage in hybrid high-voltage system", 3)
        )
        
        dao.insertDiagnosticCodes(commonCodes)
        Log.d("DTCDataSeeder", "DTC database seeded successfully.")
    }
}
