package io.github.mdsadiqueinam.qamus.ui.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.mdsadiqueinam.qamus.ui.navigation.QamusNavigator
import javax.inject.Inject

/**
 * ViewModel for the NavHost that provides access to the QamusNavigator
 */
@HiltViewModel
class NavHostViewModel @Inject constructor(
    val navigator: QamusNavigator
) : ViewModel()