package ai.wenjuanpro.app.domain.session

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SessionStateHolder {
    private val _selectedConfigId = MutableStateFlow<String?>(null)
    val selectedConfigId: StateFlow<String?> = _selectedConfigId.asStateFlow()

    private val _studentId = MutableStateFlow<String?>(null)
    val studentId: StateFlow<String?> = _studentId.asStateFlow()

    fun selectConfig(id: String) {
        _selectedConfigId.value = id
    }

    fun setStudentId(id: String) {
        _studentId.value = id
    }

    fun clear() {
        _selectedConfigId.value = null
        _studentId.value = null
    }
}
