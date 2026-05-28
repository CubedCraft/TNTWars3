package xyz.pondwader.replay_engine.codec

const val CAPTURE_REPLAY_FORMAT_VERSION = 2

data class ReplayHeader(
    val formatVersion: Int = CAPTURE_REPLAY_FORMAT_VERSION,
    val sourceWorldName: String,
    val mapId: String? = null,
    val startedAtMillis: Long,
    val metadata: Map<String, String> = emptyMap()
)

data class FrameBatch(
    val startTick: Long,
    val endTick: Long,
    val frames: List<Frame> = emptyList(),
)

data class Frame(
    val tick: Long,
    val events: MutableList<CaptureEventPayload> = mutableListOf(),
)