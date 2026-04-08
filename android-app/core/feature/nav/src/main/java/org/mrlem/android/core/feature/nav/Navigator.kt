package org.mrlem.android.core.feature.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

class Navigator(
    private val scope: CoroutineScope,
) {

    sealed interface Operation {

        data class Push(
            val key: NavKey,
        ) : Operation

        data class ReplaceAll(
            val key: NavKey,
        ) : Operation

        data object Pop : Operation
    }

    private val _operations = MutableSharedFlow<Operation>(extraBufferCapacity = 3)
    val operations: Flow<Operation> = _operations

    fun navigate(operation: Operation) {
        scope.launch {
            _operations.emit(operation)
        }
    }
}

@Composable
fun NavigationLaunchedEffect(
    navigator: Navigator,
    backStack: NavBackStack<NavKey>,
    navKeyInterface: KClass<out NavKey> = MainNavKey::class,
) {
    LaunchedEffect(Unit) {
        navigator.operations
            .onEach { operation ->
                when (operation) {
                    is Navigator.Operation.Push -> {
                        if (navKeyInterface.isInstance(operation.key)) {
                            backStack.add(operation.key)
                        }
                    }

                    is Navigator.Operation.ReplaceAll -> {
                        if (navKeyInterface.isInstance(operation.key)) {
                            backStack.clear()
                            backStack.add(operation.key)
                        }
                    }

                    is Navigator.Operation.Pop -> {
                        backStack.removeLastOrNull()
                    }
                }
            }
            .launchIn(this)
    }
}