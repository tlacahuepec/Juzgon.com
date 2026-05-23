package com.juzgon.feature.about

import androidx.lifecycle.ViewModel
import com.juzgon.domain.BuildMetadata
import com.juzgon.domain.BuildMetadataProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AboutViewModel
    @Inject
    constructor(
        private val buildMetadataProvider: BuildMetadataProvider,
    ) : ViewModel() {
        val metadata: BuildMetadata get() = buildMetadataProvider.get()
    }
