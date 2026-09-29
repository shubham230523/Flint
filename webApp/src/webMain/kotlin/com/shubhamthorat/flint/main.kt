package com.shubhamthorat.flint

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.shubhamthorat.flint.data.repository.FirebaseAuthRepository
import com.shubhamthorat.flint.domain.repository.AuthRepository

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val authRepository: AuthRepository = FirebaseAuthRepository()

    ComposeViewport {
        App(
            authRepository = authRepository
        )
    }
}
