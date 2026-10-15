package hr.podsjetnik.ui.components

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** Emits once a minute so on-screen statuses ("Rok istekao", next reminder) stay current. UI only, never reminder timing. */
fun minuteTicks(): Flow<Unit> = flow {
    while (true) {
        emit(Unit)
        delay(60_000)
    }
}
