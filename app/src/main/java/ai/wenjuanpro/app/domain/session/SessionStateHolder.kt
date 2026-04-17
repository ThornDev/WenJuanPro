package ai.wenjuanpro.app.domain.session

import ai.wenjuanpro.app.domain.model.Config
import ai.wenjuanpro.app.domain.model.Session
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SessionStateHolder {
    private val _selectedConfigId = MutableStateFlow<String?>(null)
    val selectedConfigId: StateFlow<String?> = _selectedConfigId.asStateFlow()

    private val _studentId = MutableStateFlow<String?>(null)
    val studentId: StateFlow<String?> = _studentId.asStateFlow()

    private val _selectedConfig = MutableStateFlow<Config?>(null)
    val selectedConfig: StateFlow<Config?> = _selectedConfig.asStateFlow()

    private val _currentSession = MutableStateFlow<Session?>(null)
    val currentSession: StateFlow<Session?> = _currentSession.asStateFlow()

    fun selectConfig(id: String) {
        _selectedConfigId.value = id
    }

    fun setSelectedConfig(config: Config) {
        _selectedConfig.value = config
        _selectedConfigId.value = config.configId
    }

    fun setStudentId(id: String) {
        _studentId.value = id
    }

    fun openSession(session: Session) {
        _currentSession.value = session
        _selectedConfig.value = session.config
        _selectedConfigId.value = session.config.configId
        _studentId.value = session.studentId
    }

    fun advanceCursor(qid: String) {
        _currentSession.update { current ->
            current?.copy(
                cursor = current.cursor + 1,
                completedQids = current.completedQids + qid,
            )
        }
    }

    fun clear() {
        _selectedConfigId.value = null
        _studentId.value = null
        _selectedConfig.value = null
        _currentSession.value = null
    }
}
