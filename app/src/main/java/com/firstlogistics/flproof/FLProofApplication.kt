package com.firstlogistics.flproof

import android.app.Application
import com.firstlogistics.flproof.data.database.AppDatabase
import com.firstlogistics.flproof.data.repository.EvidenceRepository
import com.firstlogistics.flproof.data.repository.SeabankPackageRepository
import com.firstlogistics.flproof.data.repository.SettingsRepository
import com.firstlogistics.flproof.utils.StorageManager

class FLProofApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var evidenceRepository: EvidenceRepository
        private set

    lateinit var seabankPackageRepository: SeabankPackageRepository
        private set

    lateinit var settingsRepository: SettingsRepository
        private set

    lateinit var storageManager: StorageManager
        private set

    lateinit var evidenceHttpServer: com.firstlogistics.flproof.server.EvidenceHttpServer
        private set

    lateinit var supabaseUploader: com.firstlogistics.flproof.supabase.SupabaseUploader
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        storageManager = StorageManager(this)
        storageManager.initDirectories()

        database = AppDatabase.getInstance(this)
        evidenceRepository = EvidenceRepository(database.evidenceDao(), storageManager)
        seabankPackageRepository = SeabankPackageRepository(database.seabankPackageDao())
        settingsRepository = SettingsRepository(this)

        supabaseUploader = com.firstlogistics.flproof.supabase.SupabaseUploader(
            this,
            settingsRepository,
            evidenceRepository
        )

        evidenceHttpServer = com.firstlogistics.flproof.server.EvidenceHttpServer(
            this,
            evidenceRepository,
            storageManager
        )
        evidenceHttpServer.start()
    }

    companion object {
        lateinit var instance: FLProofApplication
            private set
    }
}
