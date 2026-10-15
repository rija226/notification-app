package hr.podsjetnik.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import hr.podsjetnik.container
import hr.podsjetnik.data.TaskRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Gate for first launch (HANDOFF §6.8): null while loading, then whether onboarding is done. */
class OnboardingViewModel(private val repository: TaskRepository) : ViewModel() {
    val completed: StateFlow<Boolean?> = repository.settings.map { it.onboardingCompleted }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val samsungConfirmed: StateFlow<Boolean> = repository.settings.map { it.samsungSetupConfirmed }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun confirmSamsung() {
        viewModelScope.launch { repository.updateSettings { it.copy(samsungSetupConfirmed = true) } }
    }

    fun finish() {
        viewModelScope.launch { repository.updateSettings { it.copy(onboardingCompleted = true) } }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { OnboardingViewModel(checkNotNull(this[APPLICATION_KEY]).container.taskRepository) }
        }
    }
}
