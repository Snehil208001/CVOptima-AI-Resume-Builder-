package com.snehil.cvoptima.mainui.generator.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import javax.inject.Inject

@HiltViewModel
class StreamingGenerationViewModel @Inject constructor(
    private val okHttpClient: OkHttpClient
) : ViewModel() {

    private val _streamedText = MutableStateFlow("")
    val streamedText = _streamedText.asStateFlow()

    private val _isDone = MutableStateFlow(false)
    val isDone = _isDone.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private var eventSource: EventSource? = null

    fun startStreaming(taskId: String) {
        if (eventSource != null) return

        _streamedText.value = ""
        _isDone.value = false
        _error.value = null

        val request = Request.Builder()
            .url("http://10.0.2.2:8080/api/v1/ai/optimize/$taskId/stream")
            .header("Accept", "text/event-stream")
            .build()

        val listener = object : EventSourceListener() {
            override fun onOpen(eventSource: EventSource, response: Response) {
                // Connection successfully opened
            }

            override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                when (type) {
                    "token" -> {
                        _streamedText.value += data
                    }
                    "done" -> {
                        _isDone.value = true
                        eventSource.cancel()
                    }
                    "error" -> {
                        _error.value = data
                        eventSource.cancel()
                    }
                }
            }

            override fun onClosed(eventSource: EventSource) {
                _isDone.value = true
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                if (!_isDone.value) {
                    _error.value = t?.message ?: "Connection connection to stream failed"
                }
            }
        }

        val sseFactory = EventSources.createFactory(okHttpClient)
        eventSource = sseFactory.newEventSource(request, listener)
    }

    override fun onCleared() {
        super.onCleared()
        eventSource?.cancel()
    }
}
