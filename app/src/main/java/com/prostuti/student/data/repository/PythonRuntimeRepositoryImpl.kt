package com.prostuti.student.data.repository

import android.content.Context
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.android.play.core.splitinstall.SplitInstallException
import com.google.android.play.core.splitinstall.SplitInstallManagerFactory
import com.google.android.play.core.splitinstall.SplitInstallRequest
import com.google.android.play.core.splitinstall.SplitInstallSessionState
import com.google.android.play.core.splitinstall.SplitInstallStateUpdatedListener
import com.google.android.play.core.splitinstall.model.SplitInstallErrorCode
import com.google.android.play.core.splitinstall.model.SplitInstallSessionStatus
import com.prostuti.student.data.service.python.PythonInstallConfirmation
import com.prostuti.student.data.service.python.PythonRunProtocol.MODULE
import com.prostuti.student.domain.code.run.InstallFailure
import com.prostuti.student.domain.code.run.RuntimeState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

/** Downloads the Python module through Play Feature Delivery and follows its progress. */
class PythonRuntimeRepositoryImpl(context: Context) : PythonRuntimeRepository, PythonInstallConfirmation {

    private val manager = SplitInstallManagerFactory.create(context)
    private val _state = MutableStateFlow<RuntimeState>(if (isInstalled()) RuntimeState.Installed else RuntimeState.NotInstalled)
    override val state: StateFlow<RuntimeState> = _state.asStateFlow()
    private var lastSession: SplitInstallSessionState? = null

    private val listener = SplitInstallStateUpdatedListener { s ->
        if (MODULE !in s.moduleNames()) return@SplitInstallStateUpdatedListener
        lastSession = s
        _state.value = when (s.status()) {
            SplitInstallSessionStatus.PENDING, SplitInstallSessionStatus.DOWNLOADING ->
                RuntimeState.Downloading(s.bytesDownloaded(), s.totalBytesToDownload())
            SplitInstallSessionStatus.DOWNLOADED, SplitInstallSessionStatus.INSTALLING -> RuntimeState.Installing
            SplitInstallSessionStatus.INSTALLED -> RuntimeState.Installed
            SplitInstallSessionStatus.REQUIRES_USER_CONFIRMATION -> RuntimeState.NeedsConfirmation
            SplitInstallSessionStatus.FAILED -> failed(s.errorCode())
            SplitInstallSessionStatus.CANCELED, SplitInstallSessionStatus.CANCELING -> RuntimeState.NotInstalled
            else -> _state.value
        }
    }

    init {
        manager.registerListener(listener)   // app-wide singleton: never unregistered
    }

    override fun install() {
        if (isInstalled()) { _state.value = RuntimeState.Installed; return }
        _state.value = RuntimeState.Downloading(0, 0)
        manager.startInstall(SplitInstallRequest.newBuilder().addModule(MODULE).build())
            .addOnFailureListener { e ->
                Timber.w(e, "Python module install failed")
                _state.value = failed((e as? SplitInstallException)?.errorCode ?: SplitInstallErrorCode.INTERNAL_ERROR)
            }
    }

    override fun confirm(launcher: ActivityResultLauncher<IntentSenderRequest>): Boolean {
        val session = lastSession ?: return false
        if (session.status() != SplitInstallSessionStatus.REQUIRES_USER_CONFIRMATION) return false
        return manager.startConfirmationDialogForResult(session, launcher)
    }

    private fun isInstalled() = MODULE in manager.installedModules

    private fun failed(code: Int) = RuntimeState.Failed(
        reason = when (code) {
            SplitInstallErrorCode.NETWORK_ERROR -> InstallFailure.NO_NETWORK
            SplitInstallErrorCode.INSUFFICIENT_STORAGE -> InstallFailure.NO_STORAGE
            SplitInstallErrorCode.API_NOT_AVAILABLE, SplitInstallErrorCode.PLAY_STORE_NOT_FOUND,
            SplitInstallErrorCode.APP_NOT_OWNED, SplitInstallErrorCode.MODULE_UNAVAILABLE -> InstallFailure.NO_PLAY
            else -> InstallFailure.OTHER
        },
        code = code,
    )
}
