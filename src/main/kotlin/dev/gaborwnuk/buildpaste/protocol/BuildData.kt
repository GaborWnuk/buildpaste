package dev.gaborwnuk.buildpaste.protocol

/**
 * One build as the buildpaste.net backend stores it.
 *
 * [blocks], [data] and [nbt] are all indexed by the same block index, which
 * [PlacementOrder] turns into a world position.
 */
data class BuildData(
	val sizeX: Int,
	val sizeY: Int,
	val sizeZ: Int,
	/** The direction the uploader was facing, which [Rotation] turns the build away from. */
	val uploadDirection: String,
	/** Namespaced block names, already resolved from the numeric ids used on the wire. */
	val blocks: List<String>,
	/** Serialized block-state properties such as `[facing=north]`, or null where the block has none. */
	val data: List<String?>,
	/** Serialized block-entity contents, by block index. Sparse: most blocks have none. */
	val nbt: Map<Int, String>,
) {
	val blockCount: Int get() = blocks.size

	/** The volume the build's dimensions describe, which may exceed the block list if a build is truncated. */
	val volume: Long get() = sizeX.toLong() * sizeY.toLong() * sizeZ.toLong()
}

/** The outcome of a call to the backend, mirroring the status codes the plugin protocol uses. */
sealed interface ApiResult<out T> {
	data class Success<T>(val value: T) : ApiResult<T>

	/** The build or account is not known to the backend (HTTP 404). */
	data object NotFound : ApiResult<Nothing>

	/** The backend was reached but refused or failed the request. */
	data class Failed(val reason: String) : ApiResult<Nothing>
}
